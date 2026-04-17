package com.hansa.phoenixbridge

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = "Phoenix OCR Bridge", style = MaterialTheme.typography.headlineMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "Esta aplicación requiere permisos de Botón Flotante (Mostrar sobre otras apps).")
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(onClick = { checkOverlayPermission() }) {
                            Text("Iniciar Botón Flotante")
                        }
                    }
                }
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivity(intent)
            Toast.makeText(this, "Otorga el permiso y vuelve", Toast.LENGTH_LONG).show()
        } else {
            startFloatingService()
        }
    }

    private fun startFloatingService() {
        val intent = Intent(this, FloatingOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        finish()
    }
}
