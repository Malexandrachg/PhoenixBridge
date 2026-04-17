package com.hansa.phoenixbridge.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

class OcrProcessor {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    interface OnOcrCompleted {
        fun onSuccess(orden: String, ot: String, nodo: String, estado: String, tecnico: String)
        fun onError(e: Exception)
    }

    fun processImage(bitmap: Bitmap, callback: OnOcrCompleted) {
        val image = InputImage.fromBitmap(bitmap, 0)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val allText = visionText.text
                
                // Orden: "Orden N°: 830794"
                val orden = extractRegex(allText, "Orden N°:\\s*(\\d+)")
                
                // OT: "Seguimiento Cliente: BOLT...-28882944" o "/--/ Orden: 28882944" o suelto
                var ot = extractRegex(allText, "BOLT\\w+-(\\d+)")
                if (ot.isEmpty()) ot = extractRegex(allText, "/--/\\s*Orden:\\s*(\\d+)")
                if (ot.isEmpty()) ot = extractRegex(allText, "\\n(\\d{8})\\n") // Por si está suelto y tiene 8 digitos

                // Estado
                val estado = if (allText.contains("FINALIZADA", ignoreCase = true)) "FINALIZADA" else ""
                
                // Nodo -> Priorizar el mas largo o el que dice NODO
                val nodo = extractRegex(allText, "(NODO [A-Z0-9 ]+)")
                
                // Tecnico -> Ej: TEC MAURICIO SENZANO
                val tecnico = extractRegex(allText, "TEC ([A-Z ]+)")

                callback.onSuccess(orden, ot, nodo, estado, tecnico)
            }
            .addOnFailureListener { e -> callback.onError(e) }
    }

    private fun extractRegex(text: String, patternString: String): String {
        val pattern = Regex(patternString, RegexOption.IGNORE_CASE)
        val match = pattern.find(text)
        return match?.groupValues?.getOrNull(1)?.trim() ?: ""
    }
}
