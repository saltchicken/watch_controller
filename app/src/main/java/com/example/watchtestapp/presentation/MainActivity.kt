package com.example.watchtestapp.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
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
import kotlinx.coroutines.withContext
import java.io.PrintWriter
import java.net.Socket
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

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

        // LIFECYCLE OBSERVER TO DETECT REOPENING
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    SocketClient.send("WATCH_CONNECTED")
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
            }
        }

        fun sendToPython(message: String) {
            SocketClient.send(message)
        }

        // HAPTIC FEEDBACK SETUP START
        val context = LocalContext.current
        val vibrator = remember { context.getSystemService(Vibrator::class.java) }

        fun triggerHaptic(type: Int = VibrationEffect.EFFECT_CLICK) {
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    vibrator.vibrate(VibrationEffect.createPredefined(type))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(20) // Fallback for older watches
                }
            }
        }

        var hasPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            )
        }

        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { isGranted -> hasPermission = isGranted }
        )

        // Ask for permission immediately when app opens
        LaunchedEffect(Unit) {
            if (!hasPermission) {
                launcher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        // State to track if we are currently recording
        var isRecording by remember { mutableStateOf(false) }

        // Background effect that streams audio while isRecording is true
        LaunchedEffect(isRecording) {
            if (isRecording && hasPermission) {
                streamAudio(shouldRecord = { isRecording })
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
                            triggerHaptic(VibrationEffect.EFFECT_CLICK)
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
                onInteract = { triggerHaptic() },
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
                onInteract = { triggerHaptic() },
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
                onInteract = { triggerHaptic() },
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
                onInteract = { triggerHaptic() },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(70.dp)
                    .fillMaxHeight(0.5f),
                shape = RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
            )

            // 5. MIDDLE ZONE (PUSH TO TALK)
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.Center)
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording -> Color.Red.copy(alpha = 0.6f)
                            !hasPermission -> Color.Gray.copy(alpha = 0.3f)
                            else -> Color.White.copy(alpha = 0.1f)
                        }
                    )
                    .pointerInput(hasPermission) {
                        if (hasPermission) {
                            detectTapGestures(
                                onPress = {
                                    triggerHaptic(VibrationEffect.EFFECT_CLICK)
                                    isRecording = true
                                    tryAwaitRelease()
                                    isRecording = false
                                    triggerHaptic(VibrationEffect.EFFECT_TICK)
                                }
                            )
                        } else {
                            detectTapGestures(onTap = { launcher.launch(Manifest.permission.RECORD_AUDIO) })
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRecording) "MIC ON" else if (!hasPermission) "PERM?" else "",
                    color = Color.White,
                    style = MaterialTheme.typography.caption2
                )
            }
            Text("", color = Color.Gray, style = MaterialTheme.typography.caption2)
        }
    }
}

@Composable
fun InvisibleTouchArea(
    command: String,
    sendToPython: (String) -> Unit,
    onInteract: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape
) {
    Box(
        modifier = modifier
            .clip(shape)
            .pointerInput(command) {
                detectTapGestures(
                    onPress = {
                        onInteract()
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

    private val channel = Channel<String>(
        capacity = 64,
        onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
    )
    private var job: kotlinx.coroutines.Job? = null
    @Volatile private var isConnected = false

    fun init(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.IO) {
            while (isActive) {
                try {
                    val socket = Socket(IP, PORT)
                    val output = PrintWriter(socket.getOutputStream(), true)
                    isConnected = true

                    output.println("WATCH_CONNECTED")
                    if (output.checkError()) throw Exception("Handshake Failed")

                    for (msg in channel) {
                        output.println(msg)
                        if (output.checkError()) throw Exception("Connection Broken")
                    }
                } catch (e: Exception) {
                    isConnected = false
                    e.printStackTrace()
                    delay(1000)
                } finally {
                    isConnected = false
                }
            }
        }
    }

    fun send(msg: String) {
        if (isConnected) {
            channel.trySend(msg)
        }
    }

    fun close() {
        isConnected = false
        job?.cancel()
    }
}

@SuppressLint("MissingPermission")
suspend fun streamAudio(shouldRecord: () -> Boolean) {
    withContext(Dispatchers.IO) {
        val sampleRate = 16000
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

        val buffer = ByteArray(minBufSize)
        var recorder: AudioRecord? = null

        try {
            recorder = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                minBufSize
            )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) return@withContext

            SocketClient.send("AUDIO_START")
            recorder.startRecording()

            while (shouldRecord()) {
                val readCount = recorder.read(buffer, 0, minBufSize)
                if (readCount > 0) {
                    val base64Audio = Base64.encodeToString(buffer, 0, readCount, Base64.NO_WRAP)
                    SocketClient.send("AUDIO:$base64Audio")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                recorder?.stop()
                recorder?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            SocketClient.send("AUDIO_END")
        }
    }
}