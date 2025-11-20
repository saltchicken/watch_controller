package com.example.watchtestapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import com.example.watchtestapp.presentation.theme.WatchTestAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
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
                        onDragStart = { offsetX = 0f; offsetY = 0f },
                        onDragEnd = {
                            val minSwipeDist = 20f
                            if (abs(offsetX) > abs(offsetY)) {
                                if (abs(offsetX) > minSwipeDist) {
                                    if (offsetX > 0) sendToPython("Swipe Right") else sendToPython("Swipe Left")
                                }
                            } else {
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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                // Top: K (Up)
                VimButton(text = "K", onClick = { sendToPython("k") })

                Spacer(modifier = Modifier.height(5.dp))

                // Middle: H (Left) and L (Right)
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    VimButton(text = "H", onClick = { sendToPython("h") })
                    Spacer(modifier = Modifier.width(40.dp)) // Wider gap for the middle row
                    VimButton(text = "L", onClick = { sendToPython("l") })
                }

                Spacer(modifier = Modifier.height(5.dp))

                // Bottom: J (Down)
                VimButton(text = "J", onClick = { sendToPython("j") })
            }
        }
    }
}

@Composable
fun VimButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier.size(50.dp) // Slightly smaller buttons to fit the diamond better
    ) {
        Text(text = text, style = MaterialTheme.typography.title2)
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