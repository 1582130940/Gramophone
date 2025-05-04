package org.akanework.gramophone.logic.utils

import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.getSystemService
import androidx.core.util.Consumer
import java.io.File
import java.io.IOException

/**
 * Rewrite of jerickson314's great SD Scanner for Lollipop as utility class
 * https://github.com/jerickson314/sdscanner/blob/master/src/com/gmail/jerickson314/sdscanner/ScanFragment.java
 */
class SdScanner(private val context: Context, var progressFrequencyMs: Int = 250) {
	val progress = SimpleProgress()
	private val filesToProcess = hashSetOf<File>()
	private var lastUpdate = 0L
	private var roots: Set<File>? = null
	private var ignoreDb: Boolean? = null

	fun scan(inRoots: Set<File>, inIgnoreDb: Boolean) {
		roots = inRoots
		this.ignoreDb = inIgnoreDb
		lastUpdate = System.currentTimeMillis()
		for (root in roots!!) {
			progress.set(SimpleProgress.Step.DIR_SCAN, root.path, null)
			recursiveAddFiles(root)
		}
		context.contentResolver.query(
			MediaStore.Files.getContentUri("external"),
			arrayOf(
				MediaStore.MediaColumns.DATA,
				MediaStore.MediaColumns.DATE_MODIFIED
			),
			null,
			null,
			null
		)?.use { cursor ->
			val dataColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATA)
			val modifiedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED)
			val totalSize = cursor.count
			while (cursor.moveToNext()) {
				val mediaFile = File(cursor.getString(dataColumn)).getCanonicalFile()
				System.currentTimeMillis().apply {
					if (lastUpdate + progressFrequencyMs/*ms*/ < this) {
						lastUpdate = this
						progress.set(
							SimpleProgress.Step.DATABASE, mediaFile.path,
							(100 * cursor.position) / totalSize)
					}
				}
				if ((!mediaFile.exists() ||
							mediaFile.lastModified() / 1000L >
							cursor.getLong(modifiedColumn))
					&& shouldScan(mediaFile, true)) {
					filesToProcess.add(mediaFile)
				} else {
					filesToProcess.remove(mediaFile)
				}
			}
		}
		if (filesToProcess.isEmpty()) {
			scannerEnded()
		} else {
			val pathsToProcess = filesToProcess.map { it.absolutePath }.toMutableList()
			MediaScannerConnection.scanFile(
				context,
				pathsToProcess.toTypedArray(),
				null) { path: String, _: Uri ->
				if (!pathsToProcess.remove(path)) {
					Log.w("SdScanner", "Android scanned $path but we never asked it to do so")
				}
				System.currentTimeMillis().apply {
					if (lastUpdate + progressFrequencyMs/*ms*/ < this) {
						lastUpdate = this
						progress.set(
							SimpleProgress.Step.SCAN, path,
							(100 * (filesToProcess.size - pathsToProcess.size))
									/ filesToProcess.size
						)
					}
				}
				if (pathsToProcess.isEmpty()) {
					scannerEnded()
				}
			}
		}
	}

	private fun scannerEnded() {
		progress.set(SimpleProgress.Step.DONE, null, 100)
		progress.reset()
		filesToProcess.clear()
		roots = null
		ignoreDb = null
	}

	@Throws(IOException::class)
	private fun recursiveAddFiles(file: File) {
		System.currentTimeMillis().apply {
			if (lastUpdate + progressFrequencyMs/*ms*/ < this) {
				lastUpdate = this
				progress.set(
					SimpleProgress.Step.DIR_SCAN, file.path, null)
			}
		}
		if (!shouldScan(file, false)) {
			// If we got here, there file was either outside the scan
			// directory, or was an empty directory.
			return
		}
		if (!filesToProcess.add(file)) {
			// Avoid infinite recursion caused by symlinks.
			// If mFilesToProcess already contains this file, add() will
			// return false.
			return
		}
		if (!file.canRead()) {
			Log.w("SdScanner", "cannot read $file")
		}
		if (file.isDirectory) {
			val files = file.listFiles()
			if (files != null) {
				for (nextFile in files) {
					recursiveAddFiles(nextFile.canonicalFile)
				}
			}
		}
	}

	@Throws(IOException::class)
	fun shouldScan(inFile: File?, fromDb: Boolean): Boolean {
		var file = inFile
		if (ignoreDb != false && fromDb) {
			return true
		}
		while (file != null) {
			if (roots!!.contains(file)) {
				return true
			}
			file = file.parentFile
		}
		return false
	}

	fun cleanup() {
		progress.cleanup()
	}

	class SimpleProgress {
		var step = Step.NOT_STARTED
			private set
		var path: String? = null
			private set
		var percentage: Int? = null
			private set
		private val listeners = arrayListOf<Consumer<SimpleProgress>>()

		fun set(step: Step, path: String?, percentage: Int?) {
			this.step = step
			this.path = path
			this.percentage = percentage
			listeners.forEach { it.accept(this) }
		}

		fun addListener(listener: Consumer<SimpleProgress>) {
			listeners.add(listener)
		}

		fun removeListener(listener: Consumer<SimpleProgress>) {
			listeners.remove(listener)
		}

		fun reset() {
			step = Step.NOT_STARTED
			path = null
			percentage = null
		}

		fun cleanup() {
			listeners.clear()
		}

		enum class Step {
			NOT_STARTED, DIR_SCAN, DATABASE, SCAN, DONE
		}
	}

	companion object {
		fun scan(context: Context, root: File, ignoreDb: Boolean, progressFrequencyMs: Int = 250,
                 listener: Consumer<SimpleProgress>? = null) {
			val scanner = SdScanner(context, progressFrequencyMs)
			if (listener != null) {
				scanner.progress.addListener { t ->
					if (t.step == SimpleProgress.Step.DONE) {
						// remove listener again to avoid leaking memory
						scanner.cleanup()
					}
					listener.accept(t)
				}
			}
			scanner.scan(setOf(root), ignoreDb)
		}

		fun scanEverything(context: Context, progressFrequencyMs: Int = 250, listener: Consumer<SimpleProgress>? = null) {
			val scanner = SdScanner(context, progressFrequencyMs)
			if (listener != null) {
				scanner.progress.addListener { t ->
					if (t.step == SimpleProgress.Step.DONE) {
						// remove listener again to avoid leaking memory
						scanner.cleanup()
					}
					listener.accept(t)
				}
			}
			val roots = hashSetOf<File>()
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
				for (volume in context.getSystemService<StorageManager>()!!.storageVolumes) {
					if (volume.mediaStoreVolumeName == null) continue
					if (!volume.state.startsWith(Environment.MEDIA_MOUNTED)) continue
					roots.add(volume.directory!!)
				}
			} else {
				val volumes = context.getExternalFilesDirs(null).map { it.parentFile!!.parentFile!! }
				for (volume in volumes) {
					if (!Environment.getExternalStorageState(volume)
						.startsWith(Environment.MEDIA_MOUNTED)) continue
					roots.add(volume)
				}
			}
			scanner.scan(roots, false)
		}
	}
}