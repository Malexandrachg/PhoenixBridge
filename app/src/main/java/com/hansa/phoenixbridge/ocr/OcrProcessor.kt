package com.hansa.phoenixbridge.ocr

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    interface OnOcrCompleted {
        fun onSuccess(orden: String, ot: String, nodo: String, estado: String)
        fun onError(e: Exception)
    }

    fun processImage(bitmap: Bitmap, callback: OnOcrCompleted) {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val allText = visionText.text
                val orden = extractRegex(allText, "Orden N°:\\s*(\\d+)")
                val ot = extractRegex(allText, "Seguimiento Cliente:\\s*(\\d+)")
                val estado = if (allText.contains("FINALIZADA", ignoreCase = true)) "FINALIZADA" else "PENDIENTE"
                val nodo = extractRegex(allText, "Z/([^\\n]+)")

                callback.onSuccess(orden, ot, nodo, estado)
            }
            .addOnFailureListener { e -> callback.onError(e) }
    }

    private fun extractRegex(text: String, patternString: String): String {
        val pattern = Regex(patternString, RegexOption.IGNORE_CASE)
        return pattern.find(text)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
