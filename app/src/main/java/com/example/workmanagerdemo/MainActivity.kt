package com.example.workmanagerdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import coil.compose.rememberImagePainter
import com.example.workmanagerdemo.data.WorkerKeys
import com.example.workmanagerdemo.framework.ColorFilterWorker
import com.example.workmanagerdemo.framework.DownloadWorker
import com.example.workmanagerdemo.ui.theme.WorkManagerDemoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //in here we specify the work management and how
        // this are only the instances the workers do not start in here their job

        // our download worker
        val downloadRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(
                        NetworkType.CONNECTED
                    )
                    .build()
            )
            .build()

        // our color worker
        val colorFilterRequest = OneTimeWorkRequestBuilder<ColorFilterWorker>()
            .build()

        // has the information of all the work manager tasks
        val workManager = WorkManager.getInstance(applicationContext)

        setContent {
            WorkManagerDemoTheme {

                val worksInfo = workManager
                    // this means only one instance of the worker at the same time
                    .getWorkInfosForUniqueWorkLiveData("download")
                    .observeAsState() // because we are using compose
                    .value

                // whenever the key changes it triggers the inner code
                val downloadInfo = remember(key1 = worksInfo) {
                    worksInfo?.find { it.id == downloadRequest.id }
                }
                val filterInfo = remember(key1 = worksInfo) {
                    worksInfo?.find { it.id == colorFilterRequest.id }
                }

                // derivedStateOf is a side effect handler of compose
                // if no filter uri then use the download one meaning the filter job is not complete
                val imageUri by derivedStateOf {
                    val downloadUri = downloadInfo?.outputData?.getString(WorkerKeys.IMAGE_URI)
                        ?.toUri()

                    val filterUri = filterInfo?.outputData?.getString(WorkerKeys.FILTER_URI)
                        ?.toUri()
                    filterUri ?: downloadUri
                }

                // just a column that fills the whole screen
                // top to bottom items
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // if there is uri available
                    // an image should appear with an space
                    imageUri?.let { uri ->
                        Image(
                            painter = rememberImagePainter(
                                data = uri
                            ),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    Button(
                        onClick = {
                            // we use our work manager instance
                            workManager
                                .beginUniqueWork(
                                    "download",
                                    ExistingWorkPolicy.KEEP, // if the same work relaunched while this still keep it and ignore the new
                                    downloadRequest // the one to launch
                                )
                                .then(colorFilterRequest) // when finishes do this one
                                .enqueue() // to init the work
                        },
                        // if not running current work
                        enabled = downloadInfo?.state != WorkInfo.State.RUNNING
                    ) {
                        Text(text = "Start download")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // texts that will appear in every state of the workers
                    // because of the remember trigger they will show all the different
                    // stages of the process
                    when (downloadInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Downloading...")
                        WorkInfo.State.SUCCEEDED -> Text("Download succeeded")
                        WorkInfo.State.FAILED -> Text("Download failed")
                        WorkInfo.State.CANCELLED -> Text("Download cancelled")
                        WorkInfo.State.ENQUEUED -> Text("Download enqueued")
                        WorkInfo.State.BLOCKED -> Text("Download blocked")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (filterInfo?.state) {
                        WorkInfo.State.RUNNING -> Text("Applying filter...")
                        WorkInfo.State.SUCCEEDED -> Text("Filter succeeded")
                        WorkInfo.State.FAILED -> Text("Filter failed")
                        WorkInfo.State.CANCELLED -> Text("Filter cancelled")
                        WorkInfo.State.ENQUEUED -> Text("Filter enqueued")
                        WorkInfo.State.BLOCKED -> Text("Filter blocked")
                    }

                }


            }
        }
    }
}

