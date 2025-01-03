package com.dartmedia.byoncallsdk.utils.recorder

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.net.toUri
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MediaRecorderWrapper(
    private val context: Context
) {

    private var mediaRecorder: MediaRecorder? = null
    private var outputFilePath = getOutputFilePath()


    companion object {
        fun instance(context: Context): MediaRecorderWrapper {
            return MediaRecorderWrapper(context)
        }


        // TODO : For Testing Only Config
        const val IS_SAVE_TO_CACHE = false
        const val IS_DELETE_FILE = false

        // save path folder name
        const val CHILD_FOLDER_NAME = "recorded_voice_call"

        // file naming
        const val FILE_HEAD_NAME = "RECORDED_"
        const val FILE_TYPE = ".aac"

        // file encoding settings
        const val FILE_AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
        const val FILE_OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
        const val FILE_AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC
    }

    init {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
    }

    fun startRecording() {
        try {
            mediaRecorder?.apply {
                reset()
                setAudioSource(FILE_AUDIO_SOURCE)
                setOutputFormat(FILE_OUTPUT_FORMAT)
                setAudioEncoder(FILE_AUDIO_ENCODER)
                setOutputFile(outputFilePath)

                prepare()
                start()
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            reset()
            release()
        }
        mediaRecorder = null
        saveAudioToInternalStorage()
    }

    private fun saveAudioToInternalStorage() {
        val outputFile = File(outputFilePath)
        if (outputFile.exists()) {
            val internalStorageDir = getInternalStorageDir()
            try {
                val internalFile = File(internalStorageDir, outputFile.name)
                outputFile.copyTo(internalFile, true)

                // TODO(Zal): Add record recorded to DB (Server) for call log when DB is available
                uploadRecordedCallToServer(internalFile.toUri())

                // delete the original file after copying
                if (IS_DELETE_FILE) {
                    outputFile.delete()
                }

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private fun getInternalStorageDir(): File {
        return File(context.filesDir, CHILD_FOLDER_NAME)
    }

    private fun getOutputFilePath(): String {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())

        val saveDirectory =
            if (!IS_SAVE_TO_CACHE && Build.VERSION.SDK_INT != Build.VERSION_CODES.Q) {
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            } else {
                context.cacheDir
            }


        val fileName = "$FILE_HEAD_NAME$timeStamp$FILE_TYPE"
        return File(saveDirectory, fileName).absolutePath
    }

    private fun uploadRecordedCallToServer(fileUri: Uri) {
//        TODO(Zal): Upload recorded call file to Server
    }
}
