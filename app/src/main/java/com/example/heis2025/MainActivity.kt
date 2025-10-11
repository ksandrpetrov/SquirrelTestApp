package com.example.heis2025

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.database.Cursor
import android.net.Uri
import android.os.BatteryManager
import android.os.Bundle
import android.provider.ContactsContract
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.heis2025.ui.theme.Heis2025Theme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    private lateinit var batteryReceiver: BroadcastReceiver
    private var showBatteryMessage by mutableStateOf(false)
    private var lowBatteryAlertShown by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Heis2025Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        Greeting(
                            modifier = Modifier.padding(innerPadding)
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

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(batteryReceiver)
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
fun Greeting(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var numpadWidth by remember { mutableStateOf(Dp.Unspecified) }
    val density = LocalDensity.current

    val portraitScrollState = rememberScrollState()

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val context = LocalContext.current

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

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted: Boolean ->
            if (isGranted) {
                pickContactLauncher.launch()
            }
        }
    )

    val buttonModifier = if (numpadWidth != Dp.Unspecified) {
        Modifier.width(numpadWidth)
    } else {
        Modifier
    }

    val getPhoneButton = @Composable {
        Button(
            onClick = {
                if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    pickContactLauncher.launch()
                } else {
                    requestPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                }
            },
            modifier = buttonModifier
        ) {
            Text("Получить телефон")
        }
    }

    val clearButton = @Composable {
        Button(
            onClick = { text = "" },
            modifier = buttonModifier
        ) {
            Text("Очистить поле")
        }
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
                    .fillMaxSize()
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Центрируем контент
            ) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Введите текст") },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp)
                )
                // Используем меньший отступ
                Spacer(modifier = Modifier.height(4.dp))
                Numpad(
                    onNumberClick = onNumberClick,
                    // Передаем флаг isLandscape для изменения стиля
                    isLandscape = true,
                    modifier = Modifier.onSizeChanged {
                        numpadWidth = with(density) { it.width.toDp() }
                    }
                )
                // Используем меньший отступ
                Spacer(modifier = Modifier.height(8.dp))
                getPhoneButton()
                Spacer(modifier = Modifier.height(4.dp))
                clearButton()
            }
        }
    } else { // Portrait
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(portraitScrollState)
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
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Введите текст") },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Numpad(
                onNumberClick = onNumberClick,
                // Для портретного режима isLandscape = false
                isLandscape = false,
                modifier = Modifier.onSizeChanged {
                    numpadWidth = with(density) { it.width.toDp() }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            getPhoneButton()
            Spacer(modifier = Modifier.height(8.dp))
            clearButton()
        }
    }
}


@Composable
fun Numpad(
    onNumberClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    // Добавляем параметр для определения ориентации
    isLandscape: Boolean = false
) {
    // Определяем стиль кнопок в зависимости от ориентации
    val buttonFontSize = if (isLandscape) 24.sp else 42.sp
    val buttonPadding = if (isLandscape) PaddingValues(horizontal = 8.dp, vertical = 4.dp) else ButtonDefaults.ContentPadding
    val rowPadding = if (isLandscape) 4.dp else 8.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        (1..9).chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = rowPadding)
            ) {
                row.forEach { number ->
                    Button(
                        onClick = { onNumberClick(number.toString()) },
                        // Применяем кастомные отступы
                        contentPadding = buttonPadding
                    ) {
                        Text(
                            text = number.toString(),
                            // Применяем кастомный размер шрифта
                            fontSize = buttonFontSize
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Heis2025Theme {
        Greeting()
    }
}
