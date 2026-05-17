package com.bilimusic.app.util

import android.content.Context
import android.util.Log
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLog {
    private const val TAG = "BiliMusic"
    private const val MAX_LINES = 500
    private var logFile: File? = null

    fun init(context: Context) {
        logFile = File(context.filesDir, "debug_log.txt")
        if (logFile!!.length() > 50_000) {
            logFile!!.delete()
        }
        i("DebugLog initialized")
    }

    fun i(msg: String) = write("I", msg)
    fun e(msg: String, tr: Throwable? = null) {
        val full = if (tr != null) "$msg\n${Log.getStackTraceString(tr)}" else msg
        write("E", full)
    }

    fun getLog(): String {
        return logFile?.readText() ?: "(no log)"
    }

    fun clear() {
        logFile?.delete()
    }

    private fun write(level: String, msg: String) {
        val time = SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val line = "[$time][$level] $msg"
        Log.d(TAG, msg)
        try {
            val file = logFile ?: return
            file.appendText("$line\n")
            val lines = file.readLines()
            if (lines.size > MAX_LINES) {
                file.writeText(lines.takeLast(MAX_LINES / 2).joinToString("\n") + "\n")
            }
        } catch (_: Exception) { }
    }
}

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {
    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        DebugLog.e("CRASH: ${throwable.message}", throwable)
        defaultHandler?.uncaughtException(thread, throwable)
    }
}
