package com.n9mtq4.goes16scraper.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Created by will on 12/22/2017 at 8:15 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */

private val DATE_FORMAT = SimpleDateFormat("yyyy/MM/dd HH:mm:ss")

/**
 * gets the timestamp from ms milliseconds absolute
 * @return the timestamp in the format of yyyy/MM/dd HH:mm:ss
 * */
internal fun getTimestampAbsolute(ms: Long): String {
	return DATE_FORMAT.format(Date(ms))
}

/**
 * gets the timestamp in ms milliseconds in the future
 * @return the timestamp in the format of yyyy/MM/dd HH:mm:ss
 * */
internal fun getFutureTimestamp(ms: Long): String {
	return getTimestampAbsolute(System.currentTimeMillis() + ms)
}

/**
 * gets the current timestamp
 * @return the current timestamp in the format of yyyy/MM/dd HH:mm:ss
 * @see getFutureTimestamp
 * @see getTimestampAbsolute
 * */
internal fun getFutureTimestamp(): String {
	return getFutureTimestamp(0)
}
