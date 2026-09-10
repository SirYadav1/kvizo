package com.kvizo.app.util

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

/**
 * Catches uncaught exceptions, saves a readable crash log to
 * files/crash/crash_<timestamp>.txt so it can be shared for debugging.
 */
object CrashLogger {

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                save(context, throwable)
            } catch (_: Exception) {
            }
            previous?.uncaughtException(thread, throwable)
        }
    }

    private fun save(context: Context, throwable: Throwable) {
        val dir = File(context.filesDir, "crash").apply { mkdirs() }
        val file = File(dir, "crash_${System.currentTimeMillis()}.txt")

        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))

        val text = buildString {
            appendLine("Kvizo crash @ ${System.currentTimeMillis()}")
            appendLine("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("App ${context.packageName}")
            appendLine()
            append(sw.toString())
        }
        file.writeText(text)
    }

    fun latest(context: Context): File? =
        File(context.filesDir, "crash").listFiles()?.maxByOrNull { it.lastModified() }
}
