package com.n9mtq4.goes16scraper

import java.io.File

/**
 * Created by will on 12/22/2017 at 9:49 PM.
 *
 * @author Will "n9Mtq4" Bresnahan
 */
data class ImageOptions(
	var outputDir: File,
	var satellite: String,
	var type: String,
	var res: String,
	var band: String,
	var infoTechnique: String,
	var fileExt: String
) {
	
	fun sanitize() {
		// make sure that the number is two chars or its uppercase
		satellite = satellite.toUpperCase()
		band = (band.toIntOrNull()?.toString()?.padStart(2, '0') ?: band).toUpperCase()
	}
	
}
