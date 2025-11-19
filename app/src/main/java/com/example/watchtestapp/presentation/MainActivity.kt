package com.example.watchtestapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.background
// ‼️ Added imports for Layouts, Button(Chip), and Coroutines
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
// ‼️ Removed stringResource import as we are using direct strings for this example
// import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.watchtestapp.R
import com.example.watchtestapp.presentation.theme.WatchTestAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.Socket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            // ‼️ Changed "Android" to "Pixel Watch 2" to customize the greeting
            WearApp("Pixel Watch 2")
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    WatchTestAppTheme {
        // ‼️ Create a coroutine scope to run network operations off the main thread
        val coroutineScope = rememberCoroutineScope()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            // ‼️ Wrapped content in a Column to stack the Greeting and the Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Greeting(greetingName = greetingName)

                Spacer(modifier = Modifier.height(16.dp))

                // ‼️ Added a Chip (Standard Wear OS Button with text)
                Chip(
                    onClick = {
                        // ‼️ Launch TCP connection on IO thread
                        coroutineScope.launch(Dispatchers.IO) {
                            try {
                                val socket = Socket("10.0.0.19", 5001)
                                val output = PrintWriter(socket.getOutputStream(), true)
                                output.println("Button Pressed on Pixel Watch 2")
                                socket.close()
                            } catch (e: Exception) {
                                e.printStackTrace() // Log errors to Logcat
                            }
                        }
                    },
                    label = { Text("Send TCP") }
                )
            }
        }
    }
}

@Composable
fun Greeting(greetingName: String) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
        color = MaterialTheme.colors.primary,
        // ‼️ Changed from stringResource to a direct string template for immediate visibility
        text = "Hello $greetingName!"
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    // ‼️ Updated Preview to reflect the new specific device name
    WearApp("Pixel Watch 2")
}