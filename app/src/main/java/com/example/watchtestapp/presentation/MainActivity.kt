package com.example.watchtestapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.watchtestapp.presentation.theme.WatchTestAppTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            WearApp()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        SocketClient.close()
    }
}

@Composable
fun WearApp() {
    WatchTestAppTheme {
        LaunchedEffect(Unit) {
            SocketClient.init(this)
        }

        fun sendToPython(message: String) {
            SocketClient.send(message)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    var offsetX = 0f
                    var offsetY = 0f
                    detectDragGestures(
                        onDragStart = { offsetX = 0f; offsetY = 0f },
                        onDragEnd = {
                            if (abs(offsetX) > abs(offsetY)) {
                                if (offsetX > 0) sendToPython("Swipe Right") else sendToPython("Swipe Left")
                            } else {
                                if (offsetY > 0) sendToPython("Swipe Down") else sendToPython("Swipe Up")
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
            // 1. UP ZONE (K)
            InvisibleTouchArea(
                command = "k",
                sendToPython = ::sendToPython,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.6f)
                    .height(80.dp),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )

            // 2. DOWN ZONE (J)
            InvisibleTouchArea(
                command = "j",
                sendToPython = ::sendToPython,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.6f)
                    .height(80.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )

            // 3. LEFT ZONE (H)
            InvisibleTouchArea(
                command = "h",
                sendToPython = ::sendToPython,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(70.dp)
                    .fillMaxHeight(0.5f),
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
            )

            // 4. RIGHT ZONE (L)
            InvisibleTouchArea(
                command = "l",
                sendToPython = ::sendToPython,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(70.dp)
                    .fillMaxHeight(0.5f),
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            )

            // 5. MIDDLE ZONE (Action)
            InvisibleTouchArea(
                command = "Button Pressed",
                sendToPython = ::sendToPython,
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center),
                shape = CircleShape
            )

            Text("", color = Color.Gray, style = MaterialTheme.typography.caption2)
        }
    }
}

@Composable
fun InvisibleTouchArea(
    command: String,
    sendToPython: (String) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape
) {
    Box(
        modifier = modifier
            .clip(shape)
            .pointerInput(command) {
                detectTapGestures(
                    onPress = {
                        sendToPython(command)
                    }
                )
            }
            .background(Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = "", color = Color.White.copy(alpha = 0.3f))
    }
}

object SocketClient {
    private const val IP = "10.0.0.19"
    private const val PORT = 5001
    private val channel = Channel<String>(Channel.UNLIMITED)
    private var job: kotlinx.coroutines.Job? = null

    fun init(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val socket = Socket(IP, PORT)
                    val output = PrintWriter(socket.getOutputStream(), true)
                    for (msg in channel) {
                        output.println(msg)
                        if (output.checkError()) throw Exception("Connection Broken")
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(1000)
                }
            }
        }
    }

    fun send(msg: String) {
        channel.trySend(msg)
    }

    fun close() {
        job?.cancel()
    }
}