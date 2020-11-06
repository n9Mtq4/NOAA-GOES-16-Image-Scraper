package com.n9mtq4.goes16scraper

/**
 * Created by will on 12/22/2017 at 8:19 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

const val CONNECTION_TIMEOUT_MS = 1000 * 60 * 5 // 5 minute timeout
const val READ_TIMEOUT_MS = CONNECTION_TIMEOUT_MS * 10 // 10 times connection timeout

const val DEFAULT_OUTPUT_DIRECTORY = "imgs/"

const val DEFAULT_SATELLITE = "GOES16"
const val DEFAULT_TYPE = "FD"
const val DEFAULT_BAND = "GeoColor"
const val DEFAULT_RESOLUTION = "339x339"
const val DEFAULT_INFOTECHNIQUE = "directorylist"
const val DEFAULT_FILEEXT = "jpg"

const val DEFAULT_SLEEP_TIME: Long = 1000 * 60 * 60 * 2 // 2 hour sleep time (4 hours default)
const val DEFAULT_CHECK_SLEEP_TIME: Long = DEFAULT_SLEEP_TIME / 12 // accuracy of target time is 1/12th of the original sleep time
const val DEFAULT_SLEEP_TIME_BEFORE_DOWNLOAD: Long = 1000 * 60 * 10 // 10 minutes
const val DEFAULT_DOWNLOAD_BATCH_SIZE: Int = 4 // images to download at the same time (# of threads in pool for download)
