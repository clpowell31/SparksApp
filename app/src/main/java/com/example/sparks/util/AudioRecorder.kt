package com.example.sparks.util

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File
import java.io.FileOutputStream

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    fun startRecording(): File? {
        // Create a temp file in cache
        outputFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.mp3")

        recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(FileOutputStream(outputFile).fd)
            prepare()
            start()
        }
        return outputFile
    }

    fun stopRecording() {
        try {
            recorder?.stop()
            recorder?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        recorder = null
    }
}