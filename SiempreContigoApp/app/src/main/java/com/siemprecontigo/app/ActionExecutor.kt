package com.siemprecontigo.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import com.siemprecontigo.app.model.PendingAction
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Ejecuta cada [PendingAction] mediante Intents que dejan el último paso
 * en manos del usuario. Ver guion-tecnico-ia-siempre-contigo.md, sección 2.
 */
class ActionExecutor(private val context: Context) {

    fun execute(action: PendingAction): Boolean = try {
        when (action) {
            is PendingAction.EnviarWhatsapp -> enviarWhatsapp(action)
            is PendingAction.EscribirCorreo -> escribirCorreo(action)
            is PendingAction.CrearRecordatorio -> crearRecordatorio(action)
            is PendingAction.IniciarLlamada -> iniciarLlamada(action)
        }
        true
    } catch (_: Exception) {
        false
    }

    private fun enviarWhatsapp(action: PendingAction.EnviarWhatsapp) {
        val telefonoRaw = action.telefono
            ?: ContactResolver(context).buscarCoincidencias(action.contacto).firstOrNull()?.telefono
            ?: throw IllegalStateException("Sin teléfono")
        val telefono = ContactResolver.normalizarTelefono(telefonoRaw)
        val uri = Uri.parse(
            "https://api.whatsapp.com/send?phone=$telefono&text=${Uri.encode(action.mensaje)}",
        )
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isAppInstalled("com.whatsapp")) setPackage("com.whatsapp")
        }
        context.startActivity(intent)
    }

    private fun escribirCorreo(action: PendingAction.EscribirCorreo) {
        val email = action.email
            ?: action.destinatario.takeIf { ContactResolver.pareceEmail(it) }
            ?: throw IllegalStateException("Sin correo")
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, action.asunto)
            putExtra(Intent.EXTRA_TEXT, action.mensaje)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Elige tu app de correo").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun crearRecordatorio(action: PendingAction.CrearRecordatorio) {
        val startMillis = parseFechaHora(action.fechaHoraIso)
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, startMillis + 30 * 60 * 1000)
            putExtra(CalendarContract.Events.TITLE, action.titulo)
            rruleFor(action.repetir)?.let { putExtra(CalendarContract.Events.RRULE, it) }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun parseFechaHora(iso: String): Long {
        val cleaned = iso.trim().removeSuffix("Z")
        return try {
            val ldt = LocalDateTime.parse(cleaned, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            ldt.atZone(ZoneId.of("Europe/Madrid")).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis() + 60 * 60 * 1000
        }
    }

    private fun rruleFor(repetir: String): String? = when (repetir.lowercase()) {
        "diario" -> "FREQ=DAILY"
        "semanal" -> "FREQ=WEEKLY"
        "mensual" -> "FREQ=MONTHLY"
        else -> null
    }

    private fun iniciarLlamada(action: PendingAction.IniciarLlamada) {
        val telefono = action.telefono
            ?: ContactResolver(context).buscarCoincidencias(action.contacto).firstOrNull()?.telefono
            ?: throw IllegalStateException("Sin teléfono")
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$telefono")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun isAppInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }
}
