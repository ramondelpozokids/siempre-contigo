package com.siemprecontigo.app

import com.siemprecontigo.app.model.PendingAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Cliente del backend propio. La app NUNCA llama a Claude directamente:
 * solo habla con /api/interpret (Vercel), donde vive ANTHROPIC_API_KEY.
 */
class LlmClient(
    private val baseUrl: String = BuildConfig.API_BASE_URL,
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    data class HistoryTurn(val role: String, val content: String)

    sealed class Result {
        data class Message(val text: String) : Result()
        data class Action(val text: String, val action: PendingAction) : Result()
        data class Error(val text: String) : Result()
    }

    suspend fun interpretar(history: List<HistoryTurn>): Result = withContext(Dispatchers.IO) {
        val messages = JSONArray()
        history.forEach { turn ->
            messages.put(
                JSONObject()
                    .put("role", turn.role)
                    .put("content", turn.content),
            )
        }
        val bodyJson = JSONObject().put("messages", messages).toString()
        val url = baseUrl.trimEnd('/') + "/api/interpret"
        val request = Request.Builder()
            .url(url)
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val err = runCatching { JSONObject(raw).optString("error") }.getOrNull()
                    return@withContext Result.Error(
                        err?.takeIf { it.isNotBlank() }
                            ?: "No he podido conectar ahora. Comprueba tu internet e inténtalo otra vez.",
                    )
                }
                parseResponse(raw)
            }
        } catch (_: Exception) {
            Result.Error(
                "No he podido conectar ahora. Comprueba tu internet e inténtalo otra vez.",
            )
        }
    }

    private fun parseResponse(raw: String): Result {
        val json = JSONObject(raw)
        return when (json.optString("type")) {
            "action" -> {
                val actionObj = json.getJSONObject("action")
                val name = actionObj.getString("name")
                val input = actionObj.optJSONObject("input") ?: JSONObject()
                val pending = toPendingAction(name, input)
                    ?: return Result.Message(
                        json.optString(
                            "text",
                            "Todavía no puedo ayudarte con eso. Puedo enviar un WhatsApp, escribir un correo, crear un recordatorio o hacer una llamada.",
                        ),
                    )
                Result.Action(
                    text = json.optString(
                        "text",
                        "Esto es lo que voy a preparar. Revísalo antes de continuar:",
                    ),
                    action = pending,
                )
            }
            else -> Result.Message(
                json.optString(
                    "text",
                    "¿Me lo puedes decir de otra forma?",
                ),
            )
        }
    }

    private fun toPendingAction(name: String, input: JSONObject): PendingAction? = when (name) {
        "enviar_whatsapp" -> PendingAction.EnviarWhatsapp(
            contacto = input.optString("contacto"),
            mensaje = input.optString("mensaje"),
        ).takeIf { it.contacto.isNotBlank() && it.mensaje.isNotBlank() }
        "escribir_correo" -> PendingAction.EscribirCorreo(
            destinatario = input.optString("destinatario"),
            asunto = input.optString("asunto"),
            mensaje = input.optString("mensaje"),
        ).takeIf {
            it.destinatario.isNotBlank() && it.asunto.isNotBlank() && it.mensaje.isNotBlank()
        }
        "crear_recordatorio" -> PendingAction.CrearRecordatorio(
            titulo = input.optString("titulo"),
            fechaHoraIso = input.optString("fecha_hora"),
            repetir = input.optString("repetir", "no"),
        ).takeIf { it.titulo.isNotBlank() && it.fechaHoraIso.isNotBlank() }
        "iniciar_llamada" -> PendingAction.IniciarLlamada(
            contacto = input.optString("contacto"),
        ).takeIf { it.contacto.isNotBlank() }
        else -> null
    }
}
