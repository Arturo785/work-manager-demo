package com.example.workmanagerdemo.framework

import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.workmanagerdemo.R
import com.example.workmanagerdemo.data.FileApi
import com.example.workmanagerdemo.data.WorkerKeys
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Suppress("BlockingMethodInNonBlockingContext") // this is needed because old java api
// does not know the work is being made in a suspend fun


// this worker acts separate from the app, we can close the app and the worker will still
// do its job
class DownloadWorker(
    private val context: Context,
    private val workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {


    override suspend fun doWork(): Result {
        // tells us if success, failed or retry
        // to create the notification we need the notification channel made in the application class
        startForegroundService()

        delay(5000) // this delay is because of testing purposes to see the action
        val response = FileApi.instance.downloadImage() // normally this goes by use case

        response.body()?.let { body ->
            return withContext(Dispatchers.IO) {
                // this is made to save the file in our device
                val file = File(context.cacheDir, "image.jpg")
                val outputStream = FileOutputStream(file)

                outputStream.use { stream ->
                    try {
                        stream.write(body.bytes())
                    } catch (e: IOException) { // the exception needed for this occasion
                        return@withContext Result.failure(
                            // we return a special way of telling what went wrong
                            workDataOf(
                                WorkerKeys.ERROR_MSG to e.localizedMessage
                            )
                        )
                    }
                }
                // if ok we return this result
                // in other part we can retrieve this result with the key that we passed in this file
                Result.success(
                    workDataOf(
                        WorkerKeys.IMAGE_URI to file.toUri().toString() // where we stored our image
                    )
                )
            }
        }

        if (!response.isSuccessful) {
            // server error, try again
            if (response.code().toString().startsWith("5")) {
                return Result.retry()
            }
            return Result.failure(
                // in other part we can retrieve this result with the key that we passed in this file
                workDataOf(
                    WorkerKeys.ERROR_MSG to "Network error"
                )
            )
        }
        // any other kind of error
        return Result.failure(
            workDataOf(WorkerKeys.ERROR_MSG to "Unknown error")
        )
    }

    // the one that shows the notification like a service running
    private suspend fun startForegroundService() {
        setForeground(
            ForegroundInfo(
                Random.nextInt(), // the channel notification, not important here
                NotificationCompat.Builder(context, "download_channel")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentText("Downloading...")
                    .setContentTitle("Download in progress")
                    .build()
            )
        )
    }
}