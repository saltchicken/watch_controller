package com.example.watchtestapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.watchtestapp.presentation.theme.WatchTestAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            WearApp("Pixel Watch 2")
        }
    }
}

@Composable
fun WearApp(greetingName: String) {
    WatchTestAppTheme {
        val coroutineScope = rememberCoroutineScope()

        fun sendToPython(message: String) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val socket = Socket("10.0.0.19", 5001)
                    val output = PrintWriter(socket.getOutputStream(), true)
                    output.println(message)
                    socket.close()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .pointerInput(Unit) {
                    var offsetX = 0f
                    var offsetY = 0f

                    detectDragGestures(
                        onDragStart = {
                            offsetX = 0f
                            offsetY = 0f
                        },
                        onDragEnd = {
                            val minSwipeDist = 20f // Threshold to ignore small jitters

                            // Check if horizontal movement was greater than vertical
                            if (abs(offsetX) > abs(offsetY)) {
                                if (abs(offsetX) > minSwipeDist) {
                                    if (offsetX > 0) sendToPython("Swipe Right") else sendToPython("Swipe Left")
                                }
                            } else {
                                // Vertical movement was greater
                                if (abs(offsetY) > minSwipeDist) {
                                    if (offsetY > 0) sendToPython("Swipe Down") else sendToPython("Swipe Up")
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            TimeText()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Greeting(greetingName = greetingName)
                Spacer(modifier = Modifier.height(16.dp))

                Text(text = "Swipe or Click", style = MaterialTheme.typography.caption1)
                Spacer(modifier = Modifier.height(8.dp))

                Chip(
                    onClick = {
                        sendToPython("Button Pressed")
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
        text = "Hello $greetingName!"
    )
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Pixel Watch 2")
}