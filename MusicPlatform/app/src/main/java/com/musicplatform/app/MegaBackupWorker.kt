package com.musicplatform.app

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File

class MegaBackupWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val prefs = applicationContext.getSharedPreferences("wave_integrations", Context.MODE_PRIVATE)
        val treeValue = prefs.getString("mega_tree_uri", null) ?: return Result.failure()
        val tree = DocumentFile.fromTreeUri(applicationContext, Uri.parse(treeValue)) ?: return Result.failure()
        val source = File(applicationContext.filesDir, "imports")
        if (!source.exists()) return Result.success()
        return try {
            source.listFiles()?.filter { it.isFile }?.forEach { file ->
                if (tree.findFile(file.name) == null) {
                    val mime = when (file.extension.lowercase()) {
                        "mp3" -> "audio/mpeg"
                        "ogg", "oga" -> "audio/ogg"
                        "m4a", "mp4" -> "audio/mp4"
                        "wav" -> "audio/wav"
                        "flac" -> "audio/flac"
                        else -> "application/octet-stream"
                    }
                    val target = tree.createFile(mime, file.name) ?: throw IllegalStateException("Could not create audio file in the selected backup folder")
                    applicationContext.contentResolver.openOutputStream(target.uri, "w")?.use { output ->
                        file.inputStream().use { input -> input.copyTo(output) }
                    } ?: throw IllegalStateException("Could not write audio file to the selected backup folder")
                }
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
