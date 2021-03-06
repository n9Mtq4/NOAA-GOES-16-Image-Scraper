package com.n9mtq4.goes16scraper

import com.n9mtq4.goes16scraper.utils.readFromJar
import org.apache.commons.cli.CommandLineParser
import org.apache.commons.cli.DefaultParser
import org.apache.commons.cli.HelpFormatter
import org.apache.commons.cli.Options
import java.io.File

/**
 * Created by will on 12/22/2017 at 7:16 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

fun main(args: Array<String>) {
	
	// the options
	val options = Options().apply {
		
		addOption("o", "output", true, "selects the output directory for the images ($DEFAULT_OUTPUT_DIRECTORY)")
		addOption("s", "satellite", true, "the satellite (run --satellites for list of satellites) ($DEFAULT_SATELLITE)")
		addOption("t", "type", true, "the type of image (run --types for list of types) ($DEFAULT_TYPE)")
		addOption("r", "resolution", true, "selects the image resolution to download (run --resolutions for list of resolutions) ($DEFAULT_RESOLUTION)")
		addOption("b", "band", true, "selects the color/band  (run --bands for list of types) ($DEFAULT_BAND)")
		addOption("e", "fileext", true, "sets the file extension of the image (run --filesexts for a list) ($DEFAULT_FILEEXT)")
		addOption(null, "sleeptime", true, "the time (ms) between downloading images ($DEFAULT_SLEEP_TIME)")
		addOption(null, "checksleeptime", true, "the time (ms) between checking if sleep time has passed ($DEFAULT_CHECK_SLEEP_TIME)")
		addOption(null, "beforedownloadtime", true, "the time (ms) between downloading the list of images and actually downloading them ($DEFAULT_SLEEP_TIME_BEFORE_DOWNLOAD)")
		addOption(null, "downloadbatchsize", true, "the number of images to download at the same time ($DEFAULT_DOWNLOAD_BATCH_SIZE)")
		addOption(null, "infotechnique", true, "the strategy for gaining information on images ($DEFAULT_INFOTECHNIQUE)")
		addOption(null, "satellites", false, "prints a list of satellites")
		addOption(null, "types", false, "prints a list of types")
		addOption(null, "resolutions", false, "prints a list of resolutions")
		addOption(null, "bands", false, "prints a list of bands")
		addOption(null, "infotechniques", false, "prints a list of strategies for gaining information on images")
		addOption(null, "help", false, "prints this help message")
		
	}
	
	// the parser
	// val parser: CommandLineParser = GnuParser()
	val parser: CommandLineParser = DefaultParser()
	val cliargs = parser.parse(options, args)
	
	// help information
	if (cliargs.hasOption("help")) {
		val helpFormatter = HelpFormatter()
		helpFormatter.printHelp("java -jar jarName.jar [OPTIONS]", options)
		return
	}
	// lists of things
	val helpList = listOf("satellites", "types", "resolutions", "bands", "infotechniques", "fileexts")
	helpList
			.filter { cliargs.hasOption(it) }
			.map { readFromJar("/text/$it.txt") }
			.onEach(::println)
			.forEach { return }
	
	// get command line args or default values
	cliargs.run {
		
		val outputDir = getOptionValue("output") ?: DEFAULT_OUTPUT_DIRECTORY
		val satellite = getOptionValue("satellite") ?: DEFAULT_SATELLITE
		val type = getOptionValue("type") ?: DEFAULT_TYPE
		val res = getOptionValue("resolution") ?: DEFAULT_RESOLUTION
		val band = getOptionValue("band") ?: DEFAULT_BAND
		val fileExt = getOptionValue("fileext") ?: DEFAULT_FILEEXT
		val infoTechnique = getOptionValue("infotechnique") ?: DEFAULT_INFOTECHNIQUE
		val sleepTime = getOptionValue("sleeptime")?.toLong() ?: DEFAULT_SLEEP_TIME
		val checkSleepTime = getOptionValue("checksleeptime")?.toLong() ?: DEFAULT_CHECK_SLEEP_TIME
		val beforeDownloadTime = getOptionValue("beforedownloadtime")?.toLong() ?: DEFAULT_SLEEP_TIME_BEFORE_DOWNLOAD
		val downloadBatchSize = getOptionValue("downloadbatchsize")?.toInt() ?: DEFAULT_DOWNLOAD_BATCH_SIZE
		
		val imageOptions = ImageOptions(File(outputDir), satellite, type, res, band, infoTechnique, fileExt)
		
		// start a weather worker with the options
		val weatherWorker = WeatherWorker(sleepTime, checkSleepTime, beforeDownloadTime, downloadBatchSize, imageOptions)
		weatherWorker.run()
		
	}
	
}
