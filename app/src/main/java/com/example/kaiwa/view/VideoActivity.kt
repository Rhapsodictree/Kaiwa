package com.example.kaiwa.view

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.TextureView
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.agora.rtc2.*
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.kaiwa.config.APP_ID
import com.example.kaiwa.config.token



private const val TAG = "AgoraVideoCall"
private val REQUIRED_PERMISSIONS = arrayOf(
    Manifest.permission.RECORD_AUDIO,
    Manifest.permission.CAMERA

)

class VideoActivity : ComponentActivity() {
    private var engine: RtcEngine? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val channelName = intent.getStringExtra("ChannelName") ?: return finish()
        val userRole = intent.getStringExtra("UserRole") ?: return finish()

        setContent {
            MaterialTheme {
                VideoScreen(channelName, userRole)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        engine?.leaveChannel()
        RtcEngine.destroy()
    }
}

@Composable
fun VideoScreen(channelName: String, userRole: String) {
    var permissionsGranted by remember { mutableStateOf(false) }
    var engineInitialized by remember { mutableStateOf(false) }
    var connectionState by remember { mutableStateOf<ConnectionState>(ConnectionState.Disconnected) }
    var error by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Permission handling
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissionsGranted = permissions.values.all { it }
        if (permissionsGranted) {
            scope.launch {
                try {
                    initializeAndJoinChannel(context, channelName, userRole) { state ->
                        connectionState = state
                    }
                    engineInitialized = true
                } catch (e: Exception) {
                    error = e.message
                }
            }
        }
    }

    // Check initial permissions
    LaunchedEffect(Unit) {
        val hasPermissions = REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (hasPermissions) {
            permissionsGranted = true
            try {
                initializeAndJoinChannel(context, channelName, userRole) { state ->
                    connectionState = state
                }
                engineInitialized = true
            } catch (e: Exception) {
                error = e.message
            }
        } else {
            permissionLauncher.launch(REQUIRED_PERMISSIONS)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            error != null -> ErrorScreen(error!!)
            !permissionsGranted -> PermissionScreen { permissionLauncher.launch(REQUIRED_PERMISSIONS) }
            connectionState == ConnectionState.Connecting -> LoadingScreen()
            connectionState == ConnectionState.Connected && engineInitialized -> {
                VideoCallContent(
                    channelName = channelName,
                    userRole = userRole,
                    onError = { error = it }
                )
            }
        }
    }
}

private sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
}

@Composable
private fun VideoCallContent(
    channelName: String,
    userRole: String,
    onError: (String) -> Unit
) {
    val context = LocalContext.current
    var engine by remember { mutableStateOf<RtcEngine?>(null) }
    var remoteUsers by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(Unit) {
        try {
            engine = setupVideoEngine(context, channelName, userRole,
                onUserJoined = { uid -> remoteUsers = remoteUsers + uid },
                onUserLeft = { uid -> remoteUsers = remoteUsers - uid }
            )
        } catch (e: Exception) {
            onError(e.message ?: "Failed to initialize video engine")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Local video view for broadcasters
        if (userRole == "Broadcaster" && engine != null) {
            LocalVideoView(engine!!)
        }

        // Remote video views
        engine?.let { safeEngine ->
            remoteUsers.forEach { uid ->
                RemoteVideoView(
                    engine = safeEngine,
                    uid = uid,
                    modifier = Modifier
                        .align(if (remoteUsers.size == 1) Alignment.Center else Alignment.TopStart)
                        .fillMaxSize(if (remoteUsers.size == 1) 1f else 0.3f)
                )
            }
        }

        // Control panel
        engine?.let { safeEngine ->
            ControlPanel(
                engine = safeEngine,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LocalVideoView(engine: RtcEngine) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                engine.setupLocalVideo(VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, 0))
                engine.startPreview()
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun RemoteVideoView(
    engine: RtcEngine,
    uid: Int,
    modifier: Modifier = Modifier
) {
    AndroidView(
        factory = { context ->
            TextureView(context).apply {
                engine.setupRemoteVideo(VideoCanvas(this, VideoCanvas.RENDER_MODE_HIDDEN, uid))
            }
        },
        modifier = modifier
    )
}

@Composable
private fun ControlPanel(
    engine: RtcEngine,
    modifier: Modifier = Modifier
) {
    var isMuted by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val activity = context as? Activity

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = {
                isMuted = !isMuted
                engine.muteLocalAudioStream(isMuted)
            },
            modifier = Modifier
                .size(56.dp)
                .background(if (isMuted) Color.Red else Color.Gray, CircleShape)
        ) {
            Icon(
                imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                contentDescription = "Toggle Microphone",
                tint = Color.White
            )
        }

        IconButton(
            onClick = {
                engine.leaveChannel()
                activity?.finish()
            },
            modifier = Modifier
                .size(72.dp)
                .background(Color.Red, CircleShape)
        ) {
            Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "End Call",
                tint = Color.White
            )
        }

        IconButton(
            onClick = {
                isVideoEnabled = !isVideoEnabled
                engine.muteLocalVideoStream(!isVideoEnabled)
            },
            modifier = Modifier
                .size(56.dp)
                .background(if (!isVideoEnabled) Color.Red else Color.Gray, CircleShape)
        ) {
            Icon(
                imageVector = if (isVideoEnabled) Icons.Rounded.Videocam else Icons.Rounded.VideocamOff,
                contentDescription = "Toggle Video",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorScreen(error: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Text(error, color = Color.Red)
    }
}

@Composable
private fun PermissionScreen(onRequestPermissions: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Button(onClick = onRequestPermissions) {
            Text("Grant Permissions")
        }
    }
}

private suspend fun initializeAndJoinChannel(
    context: Context,
    channelName: String,
    userRole: String,
    onStateChange: (ConnectionState) -> Unit
) {
    onStateChange(ConnectionState.Connecting)
    delay(1000) // Give some time for the engine to initialize
    setupVideoEngine(context, channelName, userRole, {}, {})
    onStateChange(ConnectionState.Connected)
}

private fun setupVideoEngine(
    context: Context,
    channelName: String,
    userRole: String,
    onUserJoined: (Int) -> Unit,
    onUserLeft: (Int) -> Unit
): RtcEngine {
    if (APP_ID.isEmpty()) {
        throw IllegalStateException("APP_ID is not set in config.kt")
    }

    return RtcEngine.create(context, APP_ID, object : IRtcEngineEventHandler() {
        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined channel success: $channel")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            Log.d(TAG, "Remote user joined: $uid")
            onUserJoined(uid)
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            Log.d(TAG, "Remote user offline: $uid reason: $reason")
            onUserLeft(uid)
        }

        override fun onError(err: Int) {
            Log.e(TAG, "Error: $err")
            val message = when (err) {
                ErrorCode.ERR_INVALID_TOKEN -> "Invalid token"
                ErrorCode.ERR_TOKEN_EXPIRED -> "Token expired"
                else -> "Error code: $err"
            }
            (context as? Activity)?.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            }
        }
    }).apply {
        enableVideo()
        enableAudio()
        setChannelProfile(Constants.CHANNEL_PROFILE_LIVE_BROADCASTING)
        setClientRole(
            if (userRole == "Broadcaster") Constants.CLIENT_ROLE_BROADCASTER
            else Constants.CLIENT_ROLE_AUDIENCE
        )
        joinChannel(token, channelName, "", 0)
    }
}