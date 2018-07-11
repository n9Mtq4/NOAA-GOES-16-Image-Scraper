package com.n9mtq4.goes16scraper

import com.n9mtq4.goes16scraper.exceptions.WrongSizeException
import com.n9mtq4.goes16scraper.utils.getTimestamp
import com.n9mtq4.goes16scraper.utils.getTimestampAbsolute
import com.n9mtq4.goes16scraper.webparser.USER_AGENT
import com.n9mtq4.goes16scraper.webparser.parseCatalog
import com.n9mtq4.goes16scraper.webparser.parseDirectoryList
import com.n9mtq4.goes16scraper.webparser.parseDirectoryListSize
import kotlinx.coroutines.experimental.asCoroutineDispatcher
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.nio.channels.Channels
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by will on 12/22/2017 at 7:19 PM.
 * 
 * @author Will "n9Mtq4" Bresnahan
 */

class WeatherWorker(private val sleepTime: Long, private val checkSleepTime: Long, private val beforeDownloadTime: Long, private val downloadBatchSize: Int, private val imageOptions: ImageOptions) : Runnable {
	
	var ticks = 0
	var running = true
	var targetTime = System.currentTimeMillis()
	
	val executorService = Executors.newFixedThreadPool(downloadBatchSize)
	val coroutineDispatcher = executorService.asCoroutineDispatcher()
	
	override fun run() {
		
		// make sure that the image options is good
		// only needs to go once
		imageOptions.sanitize()
		
		while (running) {
			
//			spin lock for time
//			thread.sleep doesn't stay consistent against computer sleeping
//			ex: Thread.sleep(1000 * 60 * 60) should sleep for a min
//			if the computer is put to sleep in the middle of that, it will be longer
//			this spin lock will fix that
			while (running) {
				val currentTime = System.currentTimeMillis()
				if (targetTime - currentTime < checkSleepTime / 2) break
				Thread.sleep(checkSleepTime) // sleep for a couple of minutes
			}
			
//			update ticks
			ticks++
			
//			update target time for the next download
			targetTime = System.currentTimeMillis() + sleepTime
			
//			download all the images
			println("Started download: #$ticks at ${getTimestamp()}")
			work()
			println("Finished download #$ticks at ${getTimestamp()}")
			println("The next download is targeted for ${getTimestampAbsolute(targetTime)}")
			
		}
		
	}
	
	private fun work() {
		
		// check file permissions
		// should make sure before every run
		checkFilePermissions()
		
		try {
			
			val imageUrlList = when(imageOptions.infoTechnique) {
				"catalog" -> parseCatalog(imageOptions)
				"directorylist" -> parseDirectoryList(imageOptions)
				"directorylistsize" -> parseDirectoryListSize(imageOptions)
				else -> {
					println("Not a valid info technique - ${imageOptions.infoTechnique}")
					return
				}
			}
			
			// sleep a bit between getting the list and downloading the images
			// this is to stop the theoretical condition that we download a file that
			// hasn't finished being copied to the remote hard drive.
			println("Got list. Waiting ${beforeDownloadTime.toFloat() / 1000.0} seconds to download.")
			Thread.sleep(beforeDownloadTime)
			
			downloadAll(imageUrlList)
			
		} catch (e: Exception) {
			println("Error downloading the images! Will try again at ${getTimestampAbsolute(targetTime)}. (${e.localizedMessage})")
			e.printStackTrace()
		}
		
	}
	
	private fun downloadAll(imageUrls: ImageToDownloadList) { 
		
		val totalSize = imageUrls.size
		val failed = AtomicInteger(0)
		
		val imagesToDownload = imageUrls.filter { (name, _) -> shouldDownloadImage(name) }
		
		runBlocking(context = coroutineDispatcher) {
			
			imagesToDownload.map { imageToDownload -> launch(context = coroutineDispatcher) {
				
				try {
					downloadImage(imageToDownload)
				}catch (e: Exception) {
					e.printStackTrace()
					failed.incrementAndGet()
				}
				
			} }.forEach { it.join() }
			
		}
		
		// calculate some stats
		val alreadyDownloaded = totalSize - imagesToDownload.size
		val succeeded = imagesToDownload.size - failed.toInt()
		
		println("New: $succeeded, AlreadyHad: $alreadyDownloaded, Failed: $failed, Total: $totalSize")
		
	}
	
	/**
	 * checks to make sure that the file system is allowing us to read and write the required directories
	 * the working directory must be rw for detecting and possibly creating a new directory
	 * the ./img/ directory must be rw for checking if images exist and downloading images
	 * */
	private fun checkFilePermissions() {
		
		// make sure we can create the output directory to put the images
		imageOptions.outputDir.absoluteFile.parentFile.run {
			if (!canRead()) println("This program can't read the current directory. Check your permissions.")
			if (!canWrite()) println("This program can't create the required directory! Check your permissions.")
		}
		
		// make the output directory if needed
		if (!imageOptions.outputDir.exists()) imageOptions.outputDir.mkdirs()
		
		// make sure we can read and write in the output directory
		imageOptions.outputDir.run {
			if (!canRead()) println("This program can't read the current directory. Check your permissions.")
			if (!canWrite()) println("This program can't create the required directory! Check your permissions.")
		}
		
	}
	
	/**
	 * Note: this method may take anywhere from 0 to the CHECK_SLEEP_TIME
	 * to register and stop the run method's loop
	 * THIS DOES NOT STOP THE THREAD, ONLY STOPS THE RUN METHOD
	 * THE IMAGES WILL BE DOWNLOADED ONE MORE TIME BEFORE THE RUN METHOD STOPS
	 * */
	fun stop() {
		this.running = false
	}
	
	/**
	 * Checks to see if the image with the specified name should
	 * be downloaded.
	 * 
	 * If the image exists (has already been downloaded) it should
	 * not be downloaded again
	 * 
	 * @param imageName the name of the image to check
	 * @return true if the image should be downloaded
	 * */
	private fun shouldDownloadImage(imageName: String): Boolean {
		
		val targetFile = getTargetImageFile(imageName)
		
		// TODO: could check for abnormal file sizes here to detect errors
		return !targetFile.exists()
		
	}
	
	private fun downloadImage(imageToDownload: ImageToDownload) {
		
		val (imageName, imageUrl, imageSize) = imageToDownload
		val targetFile = getTargetImageFile(imageName)
		println("Downloading $imageName")
		
		var exception: Exception? = null
		
		try {
			val url = URL(imageUrl)
			val urlConnection = url.openConnection()
			urlConnection.connectTimeout = CONNECTION_TIMEOUT_MS
			urlConnection.readTimeout = READ_TIMEOUT_MS
			urlConnection.setRequestProperty("User-Agent", USER_AGENT)
			urlConnection.getInputStream().use { urlInputStream ->
				Channels.newChannel(urlInputStream).use { rbc ->
					FileOutputStream(targetFile).use { fos ->
						fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
					}
				}
			}
		}catch (e: Exception) {
			exception = e
		}
		
		// check download size if supported
		imageSize.takeIf { it > 0 }?.let { size ->
			
			val actualSize = targetFile.length()
			
			if (size != actualSize) {
				
				// size is wrong
				// delete the incorrect image
				targetFile.delete()
				
				exception = WrongSizeException(imageName, size, actualSize)
			}
			
		}
		
		exception?.let {
			Thread.sleep(2000) // sleep 2 seconds to not spam a broken system
			throw it
		}
		
	}
	
	private fun getTargetImageFile(imageName: String): File = File(imageOptions.outputDir, imageName)
	
}
