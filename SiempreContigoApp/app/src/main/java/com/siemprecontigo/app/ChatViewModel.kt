package com.siemprecontigo.app

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.siemprecontigo.app.model.ChatMessage
import com.siemprecontigo.app.model.ContactChoice
import com.siemprecontigo.app.model.PendingAction
import com.siemprecontigo.app.model.Sender
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados de conversación para resolver contactos (guion técnico, sección 4)
 * y para pedir permiso READ_CONTACTS la primera vez que hace falta.
 */
sealed class ConversationState {
    data object Idle : ConversationState()

    /** Varios contactos coinciden: esperamos que elija uno. */
    data class AwaitingContactChoice(
        val action: PendingAction,
        val options: List<ContactChoice>,
        val introText: String,
    ) : ConversationState()

    /** No hay coincidencia: pedimos el número (o correo) directamente. */
    data class AwaitingManualContact(
        val action: PendingAction,
        val introText: String,
    ) : ConversationState()

    /** La UI debe mostrar el diálogo de permiso antes de continuar. */
    data class NeedsContactsPermission(
        val resumeWithText: String?,
        val resumeAction: PendingAction?,
    ) : ConversationState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val executor = ActionExecutor(application)
    private val contacts = ContactResolver(application)
    private val llm = LlmClient()

    private val history = mutableListOf<LlmClient.HistoryTurn>()

    private val _messages = MutableStateFlow(
        listOf(
            ChatMessage(
                from = Sender.AI,
                text = "Hola, soy tu asistente. ¿En qué te ayudo hoy?",
            ),
        ),
    )
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _isThinking = MutableStateFlow(false)
    val isThinking: StateFlow<Boolean> = _isThinking.asStateFlow()

    private val _conversationState = MutableStateFlow<ConversationState>(ConversationState.Idle)
    val conversationState: StateFlow<ConversationState> = _conversationState.asStateFlow()

    fun onUserSend(texto: String) {
        if (texto.isBlank()) return
        val trimmed = texto.trim()
        addMessage(ChatMessage(from = Sender.USER, text = trimmed))

        when (val state = _conversationState.value) {
            is ConversationState.AwaitingManualContact -> {
                handleManualContact(trimmed, state.action)
                return
            }
            is ConversationState.AwaitingContactChoice -> {
                // Si escribe en vez de pulsar, intentar emparejar por nombre
                val match = state.options.firstOrNull {
                    it.nombre.contains(trimmed, ignoreCase = true) ||
                        it.etiqueta.contains(trimmed, ignoreCase = true)
                }
                if (match != null) {
                    onContactChosen(match)
                } else {
                    addMessage(
                        ChatMessage(
                            from = Sender.AI,
                            text = "No estoy seguro de a quién te refieres. Pulsa uno de los nombres de la lista.",
                            contactChoices = state.options,
                        ),
                    )
                }
                return
            }
            else -> Unit
        }

        viewModelScope.launch {
            _isThinking.value = true
            history.add(LlmClient.HistoryTurn("user", trimmed))
            when (val result = llm.interpretar(history.toList())) {
                is LlmClient.Result.Message -> {
                    history.add(LlmClient.HistoryTurn("assistant", result.text))
                    addMessage(ChatMessage(from = Sender.AI, text = result.text))
                }
                is LlmClient.Result.Action -> {
                    history.add(LlmClient.HistoryTurn("assistant", result.text))
                    resolveAndPresent(result.text, result.action)
                }
                is LlmClient.Result.Error -> {
                    addMessage(ChatMessage(from = Sender.AI, text = result.text))
                }
            }
            _isThinking.value = false
        }
    }

    fun onContactChosen(choice: ContactChoice) {
        val state = _conversationState.value as? ConversationState.AwaitingContactChoice ?: return
        val resolved = withPhoneOrEmail(state.action, choice)
        _conversationState.value = ConversationState.Idle
        presentConfirmation(state.introText, resolved)
    }

    @Suppress("UNUSED_PARAMETER")
    fun onConfirm(mensajeId: String, accion: PendingAction) {
        val ok = executor.execute(accion)
        addMessage(
            ChatMessage(
                from = Sender.AI,
                text = if (ok) {
                    when (accion) {
                        is PendingAction.EnviarWhatsapp ->
                            "Se ha abierto WhatsApp con el mensaje ya escrito. Solo falta pulsar enviar."
                        is PendingAction.EscribirCorreo ->
                            "Se ha abierto tu correo con el mensaje listo. Revísalo y pulsa enviar si te parece bien."
                        is PendingAction.CrearRecordatorio ->
                            "Se ha abierto el calendario. Guarda el recordatorio si está correcto."
                        is PendingAction.IniciarLlamada ->
                            "Se ha abierto el teléfono con el número. Pulsa llamar cuando quieras."
                    }
                } else {
                    "No he podido abrir esa aplicación. ¿Lo intentamos de otra forma?"
                },
            ),
        )
    }

    @Suppress("UNUSED_PARAMETER")
    fun onCancel(mensajeId: String) {
        _conversationState.value = ConversationState.Idle
        addMessage(
            ChatMessage(
                from = Sender.AI,
                text = "Vale, no he hecho nada. ¿Te ayudo con otra cosa?",
            ),
        )
    }

    fun onContactsPermissionResult(granted: Boolean) {
        val state = _conversationState.value as? ConversationState.NeedsContactsPermission
        _conversationState.value = ConversationState.Idle
        if (!granted) {
            addMessage(
                ChatMessage(
                    from = Sender.AI,
                    text = "Sin acceso a tus contactos no puedo buscar nombres. " +
                        "Si quieres, dime el número directamente (por ejemplo 600123456).",
                ),
            )
            state?.resumeAction?.let { action ->
                if (action.needsPhoneOrEmail()) {
                    _conversationState.value = ConversationState.AwaitingManualContact(
                        action = action,
                        introText = "Dime el número o el correo, por favor.",
                    )
                }
            }
            return
        }
        val action = state?.resumeAction
        if (action != null) {
            resolveAndPresent("Esto es lo que voy a preparar. Revísalo antes de continuar:", action)
        }
    }

    private fun resolveAndPresent(introText: String, action: PendingAction) {
        when (action) {
            is PendingAction.CrearRecordatorio -> {
                presentConfirmation(introText, action)
            }
            is PendingAction.EscribirCorreo -> {
                if (ContactResolver.pareceEmail(action.destinatario)) {
                    presentConfirmation(
                        introText,
                        action.copy(email = action.destinatario),
                    )
                    return
                }
                if (!hasContactsPermission()) {
                    _conversationState.value = ConversationState.NeedsContactsPermission(
                        resumeWithText = introText,
                        resumeAction = action,
                    )
                    return
                }
                val emails = contacts.buscarEmailPorNombre(action.destinatario)
                val phones = contacts.buscarCoincidencias(action.destinatario)
                when {
                    emails.size == 1 -> presentConfirmation(
                        introText,
                        action.copy(
                            destinatario = emails[0].first,
                            email = emails[0].second,
                        ),
                    )
                    emails.size > 1 -> askContactChoice(
                        introText,
                        action,
                        emails.map { (nombre, email) ->
                            ContactChoice(nombre, email, "$nombre · $email")
                        },
                    )
                    phones.size == 1 && !phones[0].email.isNullOrBlank() -> presentConfirmation(
                        introText,
                        action.copy(destinatario = phones[0].nombre, email = phones[0].email),
                    )
                    else -> {
                        _conversationState.value = ConversationState.AwaitingManualContact(
                            action = action,
                            introText = introText,
                        )
                        addMessage(
                            ChatMessage(
                                from = Sender.AI,
                                text = "No encuentro el correo de \"${action.destinatario}\" en tus contactos. " +
                                    "¿Me dices la dirección de correo completa?",
                            ),
                        )
                    }
                }
            }
            is PendingAction.EnviarWhatsapp, is PendingAction.IniciarLlamada -> {
                if (!hasContactsPermission()) {
                    _conversationState.value = ConversationState.NeedsContactsPermission(
                        resumeWithText = introText,
                        resumeAction = action,
                    )
                    return
                }
                val nombre = when (action) {
                    is PendingAction.EnviarWhatsapp -> action.contacto
                    is PendingAction.IniciarLlamada -> action.contacto
                    else -> ""
                }
                val matches = contacts.buscarCoincidencias(nombre)
                when {
                    matches.isEmpty() -> {
                        _conversationState.value = ConversationState.AwaitingManualContact(
                            action = action,
                            introText = introText,
                        )
                        addMessage(
                            ChatMessage(
                                from = Sender.AI,
                                text = "No encuentro a \"$nombre\" en tus contactos. " +
                                    "¿Quieres que busque por otro nombre, o prefieres decirme su número?",
                            ),
                        )
                    }
                    matches.size == 1 -> {
                        val m = matches[0]
                        presentConfirmation(introText, withPhoneOrEmail(action, ContactChoice(m.nombre, m.telefono, m.nombre)))
                    }
                    else -> {
                        val options = matches.map { m ->
                            val digitos = m.telefono.filter { it.isDigit() }
                            val cola = if (digitos.length >= 4) digitos.takeLast(4) else digitos
                            ContactChoice(
                                nombre = m.nombre,
                                telefono = m.telefono,
                                etiqueta = "${m.nombre} · …$cola",
                            )
                        }
                        askContactChoice(introText, action, options)
                    }
                }
            }
        }
    }

    private fun askContactChoice(
        introText: String,
        action: PendingAction,
        options: List<ContactChoice>,
    ) {
        _conversationState.value = ConversationState.AwaitingContactChoice(
            action = action,
            options = options,
            introText = introText,
        )
        val nombres = options.joinToString(" o ") { it.etiqueta }
        addMessage(
            ChatMessage(
                from = Sender.AI,
                text = "Hay más de una persona parecida. ¿Cuál de estas es? $nombres",
                contactChoices = options,
            ),
        )
    }

    private fun handleManualContact(texto: String, action: PendingAction) {
        when (action) {
            is PendingAction.EscribirCorreo -> {
                if (!ContactResolver.pareceEmail(texto)) {
                    addMessage(
                        ChatMessage(
                            from = Sender.AI,
                            text = "Eso no parece un correo. ¿Me lo escribes otra vez? Por ejemplo: ana@correo.com",
                        ),
                    )
                    return
                }
                _conversationState.value = ConversationState.Idle
                presentConfirmation(
                    "Voy a preparar este correo. Revísalo antes de continuar:",
                    action.copy(email = texto, destinatario = texto),
                )
            }
            is PendingAction.EnviarWhatsapp, is PendingAction.IniciarLlamada -> {
                if (!ContactResolver.pareceTelefono(texto)) {
                    // Puede ser otro nombre para reintentar búsqueda
                    if (hasContactsPermission()) {
                        val matches = contacts.buscarCoincidencias(texto)
                        if (matches.size == 1) {
                            _conversationState.value = ConversationState.Idle
                            presentConfirmation(
                                "Esto es lo que voy a preparar. Revísalo antes de continuar:",
                                withPhoneOrEmail(
                                    action,
                                    ContactChoice(matches[0].nombre, matches[0].telefono, matches[0].nombre),
                                ),
                            )
                            return
                        }
                        if (matches.size > 1) {
                            askContactChoice(
                                "Esto es lo que voy a preparar. Revísalo antes de continuar:",
                                action,
                                matches.map {
                                    val digitos = it.telefono.filter { c -> c.isDigit() }
                                    ContactChoice(it.nombre, it.telefono, "${it.nombre} · …${digitos.takeLast(4)}")
                                },
                            )
                            return
                        }
                    }
                    addMessage(
                        ChatMessage(
                            from = Sender.AI,
                            text = "No encuentro ese nombre. Dime el número completo, por favor.",
                        ),
                    )
                    return
                }
                _conversationState.value = ConversationState.Idle
                val resolved = when (action) {
                    is PendingAction.EnviarWhatsapp -> action.copy(
                        contacto = action.contacto,
                        telefono = texto,
                    )
                    is PendingAction.IniciarLlamada -> action.copy(telefono = texto)
                    else -> action
                }
                presentConfirmation(
                    "Esto es lo que voy a preparar. Revísalo antes de continuar:",
                    resolved,
                )
            }
            else -> {
                _conversationState.value = ConversationState.Idle
            }
        }
    }

    private fun presentConfirmation(introText: String, action: PendingAction) {
        addMessage(
            ChatMessage(
                from = Sender.AI,
                text = introText,
                pendingAction = action,
            ),
        )
    }

    private fun withPhoneOrEmail(action: PendingAction, choice: ContactChoice): PendingAction =
        when (action) {
            is PendingAction.EnviarWhatsapp -> action.copy(
                contacto = choice.nombre,
                telefono = choice.telefono,
            )
            is PendingAction.IniciarLlamada -> action.copy(
                contacto = choice.nombre,
                telefono = choice.telefono,
            )
            is PendingAction.EscribirCorreo -> action.copy(
                destinatario = choice.nombre,
                email = choice.telefono,
            )
            else -> action
        }

    private fun PendingAction.needsPhoneOrEmail(): Boolean = when (this) {
        is PendingAction.EnviarWhatsapp, is PendingAction.IniciarLlamada, is PendingAction.EscribirCorreo -> true
        is PendingAction.CrearRecordatorio -> false
    }

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            getApplication(),
            Manifest.permission.READ_CONTACTS,
        ) == PackageManager.PERMISSION_GRANTED

    private fun addMessage(message: ChatMessage) {
        _messages.value = _messages.value + message
    }
}
