package com.hansa.phoenixbridge.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsappHelper {

    /**
     * Crea un Intent para abrir WhatsApp con un mensaje predefinido.
     * El usuario luego de esto solo debe elegir su grupo de Tigo/Hansa.
     */
    fun shareToWhatsapp(
        context: Context,
        nodo: String,
        nombreTecnico: String,
        estado: String
    ) {
        val mensaje = "SOSSAJ\n" +
                      "$nodo\n" +
                      "$nombreTecnico\n" +
                      "$estado"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, mensaje)
            type = "text/plain"
            setPackage("com.whatsapp")
        }

        // Evitar que la app crashee si el celular no tiene WhatsApp instalado
        try {
            val shareIntent = Intent.createChooser(sendIntent, "Enviar reporte a...")
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(shareIntent)
        } catch (e: Exception) {
            // Fallback si falla
            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(mensaje)}"))
            fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        }
    }
}
