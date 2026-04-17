package com.hansa.phoenixbridge.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

object WhatsappHelper {
    fun shareToWhatsapp(context: Context, nodo: String, nombreTecnico: String, estado: String) {
        val mensaje = "SOSSAJ\n$nodo\n$nombreTecnico\n$estado"
        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(mensaje)}"))
        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(fallbackIntent)
    }
}
