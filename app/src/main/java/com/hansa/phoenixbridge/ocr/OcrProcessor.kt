package com.hansa.phoenixbridge.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Interfaz (Callback) para devolver los datos leídos
    interface OnOcrCompleted {
        fun onSuccess(orden: String, ot: String, nodo: String, estado: String)
        fun onError(e: Exception)
    }

    /**
     * Procesa la captura de la pantalla
     */
    fun processImage(bitmap: Bitmap, callback: OnOcrCompleted) {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val allText = visionText.text
                Log.d("OCR_TEXT", allText)

                // Extraer variables usando RegEx (Expresiones Regulares base)
                val orden = extractRegex(allText, "Orden N°:\\s*(\\d+)")
                val ot = extractRegex(allText, "Seguimiento Cliente:\\s*(\\d+)")
                val estado = if (allText.contains("FINALIZADA", ignoreCase = true)) "FINALIZADA" else "PENDIENTE"
                val nodo = extractRegex(allText, "Z/([^\\n]+)") // Buscar por Zona Z/

                callback.onSuccess(
                    orden = orden,
                    ot = ot,
                    nodo = nodo,
                    estado = estado
                )
            }
            .addOnFailureListener { e ->
                callback.onError(e)
            }
    }

    /**
     * Función utilitaria para encontrar texto usando un patrón
     */
    private fun extractRegex(text: String, patternString: String): String {
        val pattern = Regex(patternString, RegexOption.IGNORE_CASE)
        val matchResult = pattern.find(text)
        return matchResult?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
