package com.example.watchtestapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
                text = "",
                onClick = { sendToPython("k") },
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.6f)
                    .height(80.dp),
                shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
            )

            // 2. DOWN ZONE (J)
            InvisibleTouchArea(
                text = "",
                onClick = { sendToPython("j") },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(0.6f)
                    .height(80.dp),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )

            // 3. LEFT ZONE (H)
            InvisibleTouchArea(
                text = "",
                onClick = { sendToPython("h") },
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(70.dp)
                    .fillMaxHeight(0.5f),
                shape = RoundedCornerShape(topEnd = 20.dp, bottomEnd = 20.dp)
            )

            // 4. RIGHT ZONE (L)
            InvisibleTouchArea(
                text = "",
                onClick = { sendToPython("l") },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(70.dp)
                    .fillMaxHeight(0.5f),
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            )

            // 5. MIDDLE ZONE (Action)
            InvisibleTouchArea(
                text = "",
                onClick = { sendToPython("Button Pressed") },
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
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            // Debug Color: Set alpha to 0.0f to hide completely
            .background(Color.White.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.White.copy(alpha = 0.3f))
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
fun DefaultPreview() {
    WearApp("Pixel Watch 2")
}