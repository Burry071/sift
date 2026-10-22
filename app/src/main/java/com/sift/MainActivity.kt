package com.sift

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.sift.data.Db
import com.sift.engine.ModelRuntime
import com.sift.engine.SiftEngine
import com.sift.ui.CatalogScreen
import com.sift.ui.SiftTheme
import kotlinx.coroutines.flow.flowOf

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val snippetsFlow = remember { Db.get(applicationContext).dao().all() }
            val unfiledFlow = remember { Db.get(applicationContext).dao().unfiledCount() }
            val snippets by snippetsFlow.collectAsState(initial = emptyList())
            val unfiledCount by unfiledFlow.collectAsState(initial = 0)
            val filing by SiftEngine.filing.collectAsState()
            val modelStatus by SiftEngine.modelStatus.collectAsState()

            val pickModel = rememberLauncherForActivityResult(
                ActivityResultContracts.OpenDocument()
            ) { uri ->
                if (uri != null) ModelRuntime.importFrom(applicationContext, uri)
            }
            val askNotifications = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { }
            var notificationAsk by rememberSaveable {
                mutableStateOf(
                    Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS,
                    ) != PackageManager.PERMISSION_GRANTED
                )
            }

            SiftTheme {
                CatalogScreen(
                    snippets = snippets,
                    unfiledCount = unfiledCount,
                    filing = filing,
                    modelStatus = modelStatus,
                    notificationAsk = notificationAsk,
                    onFileNow = { SiftApp.fileNow(context) },
                    onGetModelFile = {
                        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(MODEL_PAGE)))
                    },
                    onPickModelFile = { pickModel.launch(arrayOf("*/*")) },
                    onAllowNotifications = {
                        notificationAsk = false
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    },
                )
            }
        }
    }

    private companion object {
        const val MODEL_PAGE = "https://huggingface.co/litert-community/gemma-4-E2B-it"
    }
}
