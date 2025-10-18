package com.example.heis2025

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.palette.graphics.Palette
import com.example.heis2025.ui.theme.Heis2025Theme
import com.example.heis2025.data.TextRecordsRepository
import com.example.heis2025.data.TextRecord
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var batteryReceiver: BroadcastReceiver
    private var showBatteryMessage by mutableStateOf(false)
    private var lowBatteryAlertShown by mutableStateOf(false)
    
    // Состояние для работы с текстом
    private var textRepository: TextRecordsRepository? = null
    private var currentTextRecord: TextRecord? = null
    private var isEditMode by mutableStateOf(false)
    private var showDataView by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Запрашиваем разрешение на уведомления
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
        }

        // Автоматически запускаем классический Background Service
        GreetingBackgroundService.startService(this)
        
        // Инициализируем репозиторий для работы с текстом
        textRepository = TextRecordsRepository(this)

        setContent {
            Heis2025Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Greeting(
                            modifier = Modifier.padding(innerPadding),
                            textRepository = textRepository,
                            currentTextRecord = currentTextRecord,
                            isEditMode = isEditMode,
                            showDataView = showDataView,
                            onTextRecordChanged = { currentTextRecord = it },
                            onEditModeChanged = { isEditMode = it },
                            onShowDataViewChanged = { showDataView = it }
                        )
                        if (showBatteryMessage) {
                            LargeBatteryMessage(message = "30% батареи, бегу за орехами!") {
                                showBatteryMessage = false
                            }
                        }
                    }
                }
            }
        }

        batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val batteryPct = level * 100 / scale.toFloat()

                if (batteryPct.toInt() <= 30) {
                    if (!lowBatteryAlertShown) {
                        showBatteryMessage = true
                        lowBatteryAlertShown = true
                    }
                } else {
                    lowBatteryAlertShown = false
                }
            }
        }
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }

    override fun onStop() {
        super.onStop()
        // todo: release controller
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
        // Останавливаем Background Service
        GreetingBackgroundService.stopService(this)
    }
}

@Composable
fun LargeBatteryMessage(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(4000L)
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            shadowElevation = 8.dp,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
        ) {
            Text(
                text = message,
                fontSize = 28.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp)
            )
        }
    }
}

@Composable
fun Greeting(
    modifier: Modifier = Modifier,
    textRepository: TextRecordsRepository?,
    currentTextRecord: TextRecord?,
    isEditMode: Boolean,
    showDataView: Boolean,
    onTextRecordChanged: (TextRecord?) -> Unit,
    onEditModeChanged: (Boolean) -> Unit,
    onShowDataViewChanged: (Boolean) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var textInput by remember { mutableStateOf("") }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    
    // Инициализируем текстовое поле при загрузке записи
    LaunchedEffect(currentTextRecord) {
        textInput = currentTextRecord?.text ?: ""
    }

    val onNumberClick: (String) -> Unit = { number ->
        text += number
    }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact(),
        onResult = { contactUri: Uri? ->
            contactUri?.let {
                val cursor: Cursor? = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val hasPhoneNumberIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                        if (hasPhoneNumberIndex < 0 || c.getInt(hasPhoneNumberIndex) == 0) return@use

                        val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                        if (idIndex < 0) return@use
                        val contactId = c.getString(idIndex)

                        val phoneCursor: Cursor? = context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            arrayOf(contactId),
                            null
                        )
                        phoneCursor?.use { pc ->
                            if (pc.moveToFirst()) {
                                val phoneIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                if (phoneIndex != -1) {
                                    text = pc.getString(phoneIndex)
                                }
                            }
                        }
                    }
                }
            }
        }
    )

    val requestContactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                pickContactLauncher.launch()
            }
        }
    )

    val pickAudioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri: Uri? ->
            audioUri = uri
        }
    )

    val requestPermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = {
            val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            if (context.checkSelfPermission(audioPermission) == PackageManager.PERMISSION_GRANTED) {
                pickAudioLauncher.launch("audio/mpeg")
            }
        }
    )

    val getPhoneButton = @Composable {
        ElevatedButton(
            onClick = {
                if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    pickContactLauncher.launch()
                } else {
                    requestContactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Получить телефон")
        }
    }

    val launchMusicButton = @Composable {
        ElevatedButton(
            onClick = {
                val permissionsToRequest = mutableListOf<String>()
                val audioPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Manifest.permission.READ_MEDIA_AUDIO
                } else {
                    Manifest.permission.READ_EXTERNAL_STORAGE
                }

                if (context.checkSelfPermission(audioPermission) != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(audioPermission)
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                if (permissionsToRequest.isNotEmpty()) {
                    requestPermissionsLauncher.launch(permissionsToRequest.toTypedArray())
                } else {
                    pickAudioLauncher.launch("audio/mpeg")
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Запустить музыку")
        }
    }

    val startServiceButton = @Composable {
        ElevatedButton(
            onClick = {
                GreetingBackgroundService.startService(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🔄 Запустить Background Service")
        }
    }

    val stopServiceButton = @Composable {
        ElevatedButton(
            onClick = {
                GreetingBackgroundService.stopService(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("🛑 Остановить Background Service")
        }
    }
    
    // Функции для работы с текстом
    val saveText: () -> Unit = {
        if (textInput.isNotBlank()) {
            textRepository?.let { repo ->
                val recordId = repo.saveText(textInput)
                if (recordId > 0) {
                    val newRecord = TextRecord(id = recordId, text = textInput)
                    onTextRecordChanged(newRecord)
                    onEditModeChanged(true)
                }
            }
        }
    }
    
    val updateText: () -> Unit = {
        if (textInput.isNotBlank() && currentTextRecord != null) {
            textRepository?.let { repo ->
                val success = repo.updateText(currentTextRecord!!.id, textInput)
                if (success) {
                    val updatedRecord = currentTextRecord!!.copy(text = textInput)
                    onTextRecordChanged(updatedRecord)
                }
            }
        }
    }
    
    val deleteText: () -> Unit = {
        currentTextRecord?.let { record ->
            textRepository?.let { repo ->
                val success = repo.deleteText(record.id)
                if (success) {
                    onTextRecordChanged(null)
                    onEditModeChanged(false)
                    textInput = ""
                }
            }
        }
    }
    
    val toggleDataView: () -> Unit = {
        onShowDataViewChanged(!showDataView)
    }


    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = R.drawable.bel),
                contentDescription = "bel",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .weight(1f)
                    .clickable { text = "" }
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Введите номер телефона") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Numpad(onNumberClick = onNumberClick)
                Spacer(modifier = Modifier.height(16.dp))
                getPhoneButton()
                Spacer(modifier = Modifier.height(8.dp))
                launchMusicButton()
                Spacer(modifier = Modifier.height(8.dp))
                startServiceButton()
                Spacer(modifier = Modifier.height(8.dp))
                stopServiceButton()
                
                // Секция для работы с текстом
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "📝 Работа с текстом",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Введите текст") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!isEditMode) {
                        ElevatedButton(
                            onClick = saveText,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("💾 Save")
                        }
                    } else {
                        ElevatedButton(
                            onClick = updateText,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("✏️ Update")
                        }
                        ElevatedButton(
                            onClick = deleteText,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("🗑️ Delete")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                ElevatedButton(
                    onClick = toggleDataView,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (showDataView) "📋 Скрыть данные" else "📋 Показать данные")
                }
            }
        }
    } else { // Portrait
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.bel),
                contentDescription = "bel",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .clip(RoundedCornerShape(24.dp))
                    .onSizeChanged { imageSize = it }
                    .clickable { text = "" }
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (imageSize != IntSize.Zero) {
                val density = LocalDensity.current
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Введите номер телефона") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .width(with(density) { imageSize.width.toDp() })
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Numpad(onNumberClick = onNumberClick)
            Spacer(modifier = Modifier.height(16.dp))
            getPhoneButton()
            Spacer(modifier = Modifier.height(8.dp))
            launchMusicButton()
            Spacer(modifier = Modifier.height(8.dp))
            startServiceButton()
            Spacer(modifier = Modifier.height(8.dp))
            stopServiceButton()
            
            // Секция для работы с текстом
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "📝 Работа с текстом",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = textInput,
                onValueChange = { textInput = it },
                label = { Text("Введите текст") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (!isEditMode) {
                    ElevatedButton(
                        onClick = saveText,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("💾 Save")
                    }
                } else {
                    ElevatedButton(
                        onClick = updateText,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("✏️ Update")
                    }
                    ElevatedButton(
                        onClick = deleteText,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("🗑️ Delete")
                    }
                }
            }
        }
    }

    audioUri?.let {
        BackHandler {
            audioUri = null
        }
        AudioPlayer(audioUri = it, onDismiss = { audioUri = null })
    }
}

@Composable
fun Numpad(onNumberClick: (String) -> Unit) {
    val numbers = (1..9).toList()
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        numbers.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { number ->
                    FilledTonalButton(
                        onClick = { onNumberClick(number.toString()) },
                        shape = CircleShape,
                        modifier = Modifier.size(80.dp)
                    ) {
                        Text(text = number.toString(), fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun AudioPlayer(audioUri: Uri, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var mediaController by remember { mutableStateOf<MediaController?>(null) }

    DisposableEffect(context) {
        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture.addListener(
            {
                mediaController = controllerFuture.get()
            },
            MoreExecutors.directExecutor()
        )

        onDispose {
            mediaController?.release()
        }
    }

    var isPlaying by remember { mutableStateOf(false) }
    var totalDuration by remember { mutableStateOf(0L) }
    var currentTime by remember { mutableStateOf(0L) }
    var songTitle by remember { mutableStateOf("Unknown Title") }
    var songArtist by remember { mutableStateOf("Unknown Artist") }
    var albumArt by remember { mutableStateOf<Bitmap?>(null) }
    var palette by remember { mutableStateOf<Palette?>(null) }

    var isMinimized by remember { mutableStateOf(true) }

    LaunchedEffect(mediaController, audioUri) {
        mediaController?.let {
            val mediaItem = MediaItem.fromUri(audioUri)
            it.setMediaItem(mediaItem)
            it.prepare()
            it.play()
        }
    }

    DisposableEffect(mediaController) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingValue: Boolean) {
                isPlaying = isPlayingValue
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    totalDuration = mediaController?.duration ?: 0L
                }
            }

            override fun onMediaMetadataChanged(mediaMetadata: androidx.media3.common.MediaMetadata) {
                songTitle = mediaMetadata.title?.toString() ?: "Unknown Title"
                songArtist = mediaMetadata.artist?.toString() ?: "Unknown Artist"
                val art = mediaMetadata.artworkData
                albumArt = art?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                albumArt?.let { bmp ->
                    Palette.from(bmp).generate { p ->
                        palette = p
                    }
                }
            }
        }

        mediaController?.addListener(listener)

        onDispose {
            mediaController?.removeListener(listener)
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentTime = mediaController?.currentPosition ?: 0L
            delay(1000L)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = isMinimized,
            modifier = Modifier.align(Alignment.BottomCenter),
            label = "PlayerAnimation",
            transitionSpec = {
                (slideInVertically { height -> height } + fadeIn())
                    .togetherWith(slideOutVertically { height -> height } + fadeOut())
                    .using(SizeTransform(clip = false))
            }
        ) {
            minimized ->
            if (minimized) {
                MiniPlayer(
                    title = songTitle,
                    artist = songArtist,
                    albumArt = albumArt,
                    isPlaying = isPlaying,
                    totalDuration = totalDuration,
                    currentTime = currentTime,
                    onPlayPause = { mediaController?.playWhenReady = !isPlaying },
                    onClose = onDismiss,
                    onExpand = { isMinimized = false },
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            } else {
                FullScreenPlayer(
                    title = songTitle,
                    artist = songArtist,
                    albumArt = albumArt,
                    isPlaying = isPlaying,
                    currentTime = currentTime,
                    totalDuration = totalDuration,
                    palette = palette,
                    onPlayPause = { mediaController?.playWhenReady = !isPlaying },
                    onSeek = { mediaController?.seekTo(it.toLong()) },
                    onMinimize = { isMinimized = true },
                    onNext = { /* TODO */ },
                    onPrevious = { /* TODO */ }
                )
            }
        }
    }
}

@Composable
fun FullScreenPlayer(
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    currentTime: Long,
    totalDuration: Long,
    palette: Palette?,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onMinimize: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    val context = LocalContext.current
    val fallbackAlbumArt = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.bel)
    }

    val defaultColor = MaterialTheme.colorScheme.surface
    val backgroundColor = MaterialTheme.colorScheme.background

    val gradientBrush = remember(palette, backgroundColor) {
        val color = palette?.dominantSwatch?.rgb?.let { Color(it) } ?: defaultColor
        Brush.verticalGradient(listOf(color.copy(alpha = 0.99f), backgroundColor))
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBrush)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            IconButton(
                onClick = onMinimize,
                modifier = Modifier
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Minimize",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        Image(
            bitmap = (albumArt ?: fallbackAlbumArt).asImageBitmap(),
            contentDescription = "Album Art",
            modifier = Modifier
                .size(300.dp)
                .clip(RoundedCornerShape(16.dp)),
            contentScale = ContentScale.Crop
        )

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(4.dp))
            Text(artist, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        }
        Column(Modifier.fillMaxWidth()) {
            Slider(
                value = currentTime.toFloat(),
                onValueChange = onSeek,
                valueRange = 0f..totalDuration.toFloat().coerceAtLeast(0f)
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentTime))
                Text(formatTime(totalDuration))
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, enabled = false) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(48.dp)) }
            IconButton(onClick = onPlayPause) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    modifier = Modifier.size(64.dp)
                )
            }
            IconButton(onClick = onNext, enabled = false) { Icon(Icons.Default.SkipNext, contentDescription = "Next", modifier = Modifier.size(48.dp)) }
        }
    }
}

@Composable
fun MiniPlayer(
    title: String,
    artist: String,
    albumArt: Bitmap?,
    isPlaying: Boolean,
    totalDuration: Long,
    currentTime: Long,
    onPlayPause: () -> Unit,
    onClose: () -> Unit,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fallbackAlbumArt = remember {
        BitmapFactory.decodeResource(context.resources, R.drawable.bel)
    }

    Surface(
        modifier = modifier
            .padding(horizontal = 8.dp)
            .fillMaxWidth()
            .clickable(onClick = onExpand),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = (albumArt ?: fallbackAlbumArt).asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = { onPlayPause() }) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = "Play/Pause")
                }
                IconButton(onClick = { onClose() }) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            val progress = if (totalDuration > 0) currentTime.toFloat() / totalDuration.toFloat() else 0f
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
            )
        }
    }
}

fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Heis2025Theme {
        Greeting(
            textRepository = null,
            currentTextRecord = null,
            isEditMode = false,
            showDataView = false,
            onTextRecordChanged = {},
            onEditModeChanged = {},
            onShowDataViewChanged = {}
        )
    }
}
