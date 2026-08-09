package com.siemprecontigo.app

import android.content.Context
import android.provider.ContactsContract

/**
 * Busca un número (o correo) a partir de un nombre dicho/escrito por el
 * usuario ("Ana", "mi hija Ana"...). Ver guion técnico, sección 4.
 */
class ContactResolver(private val context: Context) {

    data class Coincidencia(
        val nombre: String,
        val telefono: String,
        val email: String? = null,
    )

    fun buscarCoincidencias(nombreBuscado: String): List<Coincidencia> {
        val token = extraerNombre(nombreBuscado)
        if (token.isBlank()) return emptyList()

        val porId = linkedMapOf<Long, MutableList<Pair<String, String>>>()
        val nombres = linkedMapOf<Long, String>()

        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
            ),
            null,
            null,
            null,
        )
        cursor?.use {
            val idIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nombreIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numeroIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val nombre = it.getString(nombreIdx) ?: continue
                if (!coincide(nombre, token)) continue
                val id = it.getLong(idIdx)
                val numero = it.getString(numeroIdx) ?: continue
                nombres[id] = nombre
                porId.getOrPut(id) { mutableListOf() }.add(numero to (buscarEmail(id) ?: ""))
            }
        }

        return porId.map { (id, phones) ->
            val telefono = phones.first().first
            val email = phones.firstOrNull { it.second.isNotBlank() }?.second
                ?: buscarEmail(id)
            Coincidencia(
                nombre = nombres[id] ?: token,
                telefono = telefono,
                email = email,
            )
        }.distinctBy { normalizarTelefono(it.telefono) to it.nombre.lowercase() }
    }

    fun buscarEmailPorNombre(nombreBuscado: String): List<Pair<String, String>> {
        val token = extraerNombre(nombreBuscado)
        if (token.isBlank()) return emptyList()
        val out = mutableListOf<Pair<String, String>>()
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
            ),
            null,
            null,
            null,
        )
        cursor?.use {
            val nombreIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME)
            val emailIdx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (it.moveToNext()) {
                val nombre = it.getString(nombreIdx) ?: continue
                val email = it.getString(emailIdx) ?: continue
                if (coincide(nombre, token)) out.add(nombre to email)
            }
        }
        return out.distinctBy { it.second.lowercase() }
    }

    private fun buscarEmail(contactId: Long): String? {
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.ADDRESS),
            "${ContactsContract.CommonDataKinds.Email.CONTACT_ID}=?",
            arrayOf(contactId.toString()),
            null,
        )
        cursor?.use {
            val idx = it.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            if (it.moveToFirst()) return it.getString(idx)
        }
        return null
    }

    companion object {
        private val PREFIJOS = listOf(
            "mi hija ", "mi hijo ", "mi mujer ", "mi marido ", "mi esposa ",
            "mi esposo ", "mi nieta ", "mi nieto ", "mi hermano ", "mi hermana ",
            "mi amigo ", "mi amiga ", "a ", "al ", "la ", "el ",
        )

        fun extraerNombre(raw: String): String {
            var s = raw.trim()
            val lower = s.lowercase()
            for (p in PREFIJOS) {
                if (lower.startsWith(p)) {
                    s = s.substring(p.length).trim()
                    break
                }
            }
            // Si queda "Ana diciendo...", quedarse con la primera palabra con mayúscula/nombre
            return s.split(Regex("\\s+")).firstOrNull().orEmpty()
        }

        fun coincide(nombreContacto: String, token: String): Boolean {
            if (token.length < 2) return false
            val partes = nombreContacto.split(Regex("\\s+"))
            return nombreContacto.contains(token, ignoreCase = true) ||
                partes.any { it.equals(token, ignoreCase = true) }
        }

        fun normalizarTelefono(raw: String): String {
            val digits = raw.filter { it.isDigit() || it == '+' }
            val onlyDigits = digits.filter { it.isDigit() }
            // WhatsApp espera código de país sin '+'. Si parece español (9 dígitos), anteponer 34.
            return when {
                onlyDigits.length == 9 && onlyDigits.firstOrNull() in listOf('6', '7', '8', '9') ->
                    "34$onlyDigits"
                digits.startsWith("+") -> onlyDigits
                else -> onlyDigits
            }
        }

        fun pareceEmail(valor: String): Boolean =
            valor.contains("@") && valor.contains(".")

        fun pareceTelefono(valor: String): Boolean {
            val digits = valor.filter { it.isDigit() }
            return digits.length >= 9
        }
    }
}
