package com.librompaz.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import java.util.Locale

// Recurso nativo 1: vibración. Sirve de respuesta táctil cuando algo falla o se lee la cobertura.
// El permiso VIBRATE es "normal": se declara en el manifest y no se pide en pantalla.
@Suppress("DEPRECATION")
fun vibrar(contexto: Context, milisegundos: Long = 80L) {
    val vibrador: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        contexto.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        contexto.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }
    if (vibrador == null || !vibrador.hasVibrator()) return

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrador.vibrate(VibrationEffect.createOneShot(milisegundos, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrador.vibrate(milisegundos)
    }
}

// Recurso nativo 2: texto a voz (motor TextToSpeech del sistema). Lee la cobertura en voz alta
// por el parlante. No usa el micrófono, no pide permisos y no envía datos fuera del teléfono.
class LectorDeVoz(contexto: Context) {

    private var listo = false

    private val motor = TextToSpeech(contexto.applicationContext) { estado ->
        listo = estado == TextToSpeech.SUCCESS && configurarIdioma()
    }

    // Primero intento español de Chile; si el teléfono no lo tiene, cualquier español.
    private fun configurarIdioma(): Boolean =
        listOf(Locale("es", "CL"), Locale("es")).any { idioma ->
            motor.setLanguage(idioma) >= TextToSpeech.LANG_AVAILABLE
        }

    // Devuelve false si el motor todavía no está listo o no hay voz en español.
    fun leer(texto: String): Boolean {
        if (!listo) return false
        motor.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "cobertura")
        return true
    }

    // Hay que liberar el motor al salir de la pantalla.
    fun cerrar() {
        motor.stop()
        motor.shutdown()
    }
}
