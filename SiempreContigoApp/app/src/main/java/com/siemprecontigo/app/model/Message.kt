package com.siemprecontigo.app.model

/**
 * Un mensaje dentro de la conversación.
 * `pendingAction`, si no es null, hace que la UI muestre la tarjeta
 * de confirmación (Cancelar / Confirmar).
 * `contactChoices`, si no es null, muestra botones para elegir entre contactos.
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val from: Sender,
    val text: String,
    val pendingAction: PendingAction? = null,
    val contactChoices: List<ContactChoice>? = null,
)

data class ContactChoice(
    val nombre: String,
    val telefono: String,
    val etiqueta: String,
)

enum class Sender { USER, AI }

/**
 * Acción propuesta por la IA a la espera de que el usuario la confirme.
 * Corresponde 1:1 con las 4 herramientas del guion técnico.
 */
sealed class PendingAction {

    abstract val title: String
    abstract val confirmLabel: String

    data class EnviarWhatsapp(
        val contacto: String,
        val mensaje: String,
        val telefono: String? = null,
    ) : PendingAction() {
        override val title = "WhatsApp a $contacto"
        override val confirmLabel = "Enviar WhatsApp"
    }

    data class EscribirCorreo(
        val destinatario: String,
        val asunto: String,
        val mensaje: String,
        val email: String? = null,
    ) : PendingAction() {
        override val title = "Correo a $destinatario"
        override val confirmLabel = "Abrir correo"
    }

    data class CrearRecordatorio(
        val titulo: String,
        val fechaHoraIso: String,
        val repetir: String,
    ) : PendingAction() {
        override val title = "Recordatorio: $titulo"
        override val confirmLabel = "Crear recordatorio"
    }

    data class IniciarLlamada(
        val contacto: String,
        val telefono: String? = null,
    ) : PendingAction() {
        override val title = "Llamar a $contacto"
        override val confirmLabel = "Llamar ahora"
    }
}
