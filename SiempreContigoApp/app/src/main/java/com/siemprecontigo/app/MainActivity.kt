package com.siemprecontigo.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.siemprecontigo.app.model.ChatMessage
import com.siemprecontigo.app.model.ContactChoice
import com.siemprecontigo.app.model.PendingAction
import com.siemprecontigo.app.model.Sender

private val BrandBlue = Color(0xFF1B4F9C)
private val BrandBlueDark = Color(0xFF0F3A78)
private val SoftBg = Color(0xFFF4F7FC)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("siempre_contigo", MODE_PRIVATE)
        setContent {
            SiempreContigoTheme {
                var onboardingDone by remember {
                    mutableStateOf(prefs.getBoolean("onboarding_done", false))
                }
                if (!onboardingDone) {
                    OnboardingScreen(
                        onFinished = {
                            prefs.edit().putBoolean("onboarding_done", true).apply()
                            onboardingDone = true
                        },
                    )
                } else {
                    ChatScreen()
                }
            }
        }
    }
}

@Composable
private fun SiempreContigoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = BrandBlue,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD8E4F8),
            secondary = BrandBlueDark,
            background = SoftBg,
            surface = Color.White,
            surfaceVariant = Color(0xFFE8EEF8),
            onSurface = Color(0xFF152238),
            onSurfaceVariant = Color(0xFF24314A),
        ),
        content = content,
    )
}

private val BaseFontSize = 18.sp
private val TitleFontSize = 22.sp
private val MinTouchTarget = 48.dp

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* da igual el resultado: el usuario puede continuar */ onFinished() }

    val titles = listOf(
        stringResource(R.string.onboarding_welcome_title),
        stringResource(R.string.onboarding_how_title),
        stringResource(R.string.onboarding_contacts_title),
    )
    val bodies = listOf(
        stringResource(R.string.onboarding_welcome_body),
        stringResource(R.string.onboarding_how_body),
        stringResource(R.string.onboarding_contacts_body),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftBg)
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                text = "Siempre Contigo",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlueDark,
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = titles[step],
                fontSize = TitleFontSize,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF152238),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = bodies[step],
                fontSize = BaseFontSize,
                color = Color(0xFF24314A),
                lineHeight = 26.sp,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            when (step) {
                0, 1 -> Button(
                    onClick = { step += 1 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = MinTouchTarget),
                ) { Text(stringResource(R.string.onboarding_next), fontSize = BaseFontSize) }
                else -> {
                    val alreadyGranted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.READ_CONTACTS,
                    ) == PackageManager.PERMISSION_GRANTED
                    if (alreadyGranted) {
                        Button(
                            onClick = onFinished,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = MinTouchTarget),
                        ) { Text(stringResource(R.string.onboarding_start), fontSize = BaseFontSize) }
                    } else {
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = MinTouchTarget),
                        ) {
                            Text(stringResource(R.string.onboarding_allow_contacts), fontSize = BaseFontSize)
                        }
                        OutlinedButton(
                            onClick = onFinished,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = MinTouchTarget),
                        ) {
                            Text(stringResource(R.string.onboarding_skip_contacts), fontSize = BaseFontSize)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel = viewModel()) {
    val messages by viewModel.messages.collectAsState()
    val isThinking by viewModel.isThinking.collectAsState()
    val conversationState by viewModel.conversationState.collectAsState()
    var textoEscrito by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    var showContactsRationale by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        showContactsRationale = false
        viewModel.onContactsPermissionResult(granted)
    }

    LaunchedEffect(conversationState) {
        if (conversationState is ConversationState.NeedsContactsPermission) {
            showContactsRationale = true
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    if (showContactsRationale) {
        AlertDialog(
            onDismissRequest = {
                showContactsRationale = false
                viewModel.onContactsPermissionResult(false)
            },
            title = {
                Text(stringResource(R.string.contacts_rationale_title), fontSize = TitleFontSize)
            },
            text = {
                Text(stringResource(R.string.contacts_rationale_body), fontSize = BaseFontSize)
            },
            confirmButton = {
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                ) {
                    Text(stringResource(R.string.contacts_rationale_allow), fontSize = 16.sp)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showContactsRationale = false
                        viewModel.onContactsPermissionResult(false)
                    },
                    modifier = Modifier.heightIn(min = MinTouchTarget),
                ) {
                    Text(stringResource(R.string.contacts_rationale_cancel), fontSize = 16.sp)
                }
            },
        )
    }

    Scaffold(
        containerColor = SoftBg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Siempre Contigo",
                            fontSize = TitleFontSize,
                            fontWeight = FontWeight.Bold,
                        )
                        Text("Tu asistente", fontSize = 14.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = BrandBlueDark,
                ),
            )
        },
        bottomBar = {
            InputBar(
                texto = textoEscrito,
                onTextoChange = { textoEscrito = it },
                onEnviar = {
                    viewModel.onUserSend(textoEscrito)
                    textoEscrito = ""
                },
                enabled = !isThinking,
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageRow(
                    message = message,
                    onConfirm = viewModel::onConfirm,
                    onCancel = viewModel::onCancel,
                    onContactChosen = viewModel::onContactChosen,
                )
            }
            if (isThinking) {
                item { Text("Escribiendo…", fontSize = 16.sp, color = BrandBlueDark) }
            }
        }
    }
}

@Composable
private fun InputBar(
    texto: String,
    onTextoChange: (String) -> Unit,
    onEnviar: () -> Unit,
    enabled: Boolean,
) {
    Surface(tonalElevation = 3.dp, color = Color.White) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = texto,
                onValueChange = onTextoChange,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MinTouchTarget),
                placeholder = { Text("Escribe aquí…", fontSize = BaseFontSize) },
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = BaseFontSize),
                singleLine = true,
                enabled = enabled,
            )
            Button(
                onClick = onEnviar,
                enabled = enabled && texto.isNotBlank(),
                modifier = Modifier
                    .heightIn(min = MinTouchTarget)
                    .widthIn(min = MinTouchTarget),
                contentPadding = PaddingValues(horizontal = 16.dp),
            ) {
                Text("Enviar", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: ChatMessage,
    onConfirm: (String, PendingAction) -> Unit,
    onCancel: (String) -> Unit,
    onContactChosen: (ContactChoice) -> Unit,
) {
    val isUser = message.from == Sender.USER
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            Surface(
                color = if (isUser) BrandBlue else Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.widthIn(max = 320.dp),
                shadowElevation = if (isUser) 0.dp else 1.dp,
            ) {
                Text(
                    text = message.text,
                    fontSize = BaseFontSize,
                    color = if (isUser) Color.White else Color(0xFF152238),
                    modifier = Modifier.padding(14.dp),
                    lineHeight = 24.sp,
                )
            }

            message.pendingAction?.let { accion ->
                Spacer(Modifier.height(8.dp))
                ConfirmationCard(
                    accion = accion,
                    onConfirm = { onConfirm(message.id, accion) },
                    onCancel = { onCancel(message.id) },
                )
            }

            message.contactChoices?.let { choices ->
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    choices.take(3).forEach { choice ->
                        Button(
                            onClick = { onContactChosen(choice) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = MinTouchTarget),
                        ) {
                            Text(choice.etiqueta, fontSize = 16.sp, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationCard(
    accion: PendingAction,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    var yaRespondido by remember { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.widthIn(max = 320.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(accion.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(detalleDe(accion), fontSize = BaseFontSize, lineHeight = 24.sp)
            Spacer(Modifier.height(12.dp))

            if (!yaRespondido) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { yaRespondido = true; onConfirm() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouchTarget),
                    ) { Text(accion.confirmLabel, fontSize = 16.sp) }
                    OutlinedButton(
                        onClick = { yaRespondido = true; onCancel() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = MinTouchTarget),
                    ) { Text("Cancelar", fontSize = 16.sp) }
                }
            }
        }
    }
}

private fun detalleDe(accion: PendingAction): String = when (accion) {
    is PendingAction.EnviarWhatsapp ->
        "Para: ${accion.contacto}\nMensaje: ${accion.mensaje}"
    is PendingAction.EscribirCorreo ->
        "Para: ${accion.email ?: accion.destinatario}\nAsunto: ${accion.asunto}\n${accion.mensaje}"
    is PendingAction.CrearRecordatorio ->
        "${accion.titulo}\nCuándo: ${accion.fechaHoraIso}\nRepetir: ${accion.repetir}"
    is PendingAction.IniciarLlamada ->
        "Contacto: ${accion.contacto}" +
            (accion.telefono?.let { "\nNúmero: $it" } ?: "")
}
