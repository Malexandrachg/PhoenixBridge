package com.hansa.phoenixbridge

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hansa.phoenixbridge.api.PhoenixPayload
import com.hansa.phoenixbridge.api.PhoenixResponse
import com.hansa.phoenixbridge.api.RetrofitClient
import com.hansa.phoenixbridge.ocr.OcrProcessor
import com.hansa.phoenixbridge.ui.OcrResultDialog
import com.hansa.phoenixbridge.utils.WhatsappHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : ComponentActivity() {

    private val ocrProcessor = OcrProcessor()

    private var scanOrden by mutableStateOf("")
    private var scanOt by mutableStateOf("")
    private var scanNodo by mutableStateOf("")
    private var scanEstado by mutableStateOf("")
    private var scanTecnico by mutableStateOf("")
    
    private var showDialog by mutableStateOf(false)
    private var isProcessing by mutableStateOf(false)

    // Lanzador para el botón "Seleccionar manual"
    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            processImageUris(uris)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Atrapamos si la app se abrió desde el menú Compartir del teléfono (Compartiendo Capturas directamente)
        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null && type.startsWith("image/")) {
            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            }
            if (imageUri != null) processImageUris(listOf(imageUri))
            
        } else if (Intent.ACTION_SEND_MULTIPLE == action && type != null && type.startsWith("image/")) {
            val imageUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
            }
            if (!imageUris.isNullOrEmpty()) processImageUris(imageUris)
        }
        
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    if (showDialog) {
                        Box(contentAlignment = Alignment.Center) {
                            OcrResultDialog(
                                initialOrden = scanOrden,
                                initialOt = scanOt,
                                initialNodo = scanNodo,
                                initialEstado = scanEstado,
                                initialNombreTecnico = scanTecnico,
                                onDismiss = { showDialog = false; resetData() },
                                onSend = { ord, ot, no, est, tec ->
                                    showDialog = false
                                    enviarExcel(ord, ot, no, est, tec)
                                    resetData()
                                    // Si venimos del menú compartir, cerramos la app tras enviar para mayor fluidez.
                                    if (action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) {
                                        finish()
                                    }
                                }
                            )
                        }
                    } else {
                        Column(
                            modifier = Modifier.padding(16.dp).fillMaxSize(), 
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Phoenix Bridge", style = MaterialTheme.typography.headlineLarge)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Selecciona las 2 capturas de Phoenix. También puedes mandarlas directamente usando el botón Compartir de tu Galería.", textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(modifier = Modifier.height(32.dp))
                            
                            if (isProcessing) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Extrayendo variables...")
                            } else {
                                Button(
                                    onClick = { pickImagesLauncher.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth().height(60.dp)
                                ) {
                                    Text("Seleccionar Imágenes", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun processImageUris(uris: List<Uri>) {
        isProcessing = true
        var processedCount = 0

        for (uri in uris) {
            val bitmap = getBitmapFromUri(uri)
            if (bitmap != null) {
                ocrProcessor.processImage(bitmap, object : OcrProcessor.OnOcrCompleted {
                    override fun onSuccess(orden: String, ot: String, nodo: String, estado: String, tecnico: String) {
                        if (orden.isNotEmpty()) scanOrden = orden
                        if (ot.isNotEmpty()) scanOt = ot
                        if (nodo.isNotEmpty() && nodo.length > scanNodo.length) scanNodo = nodo
                        if (estado.isNotEmpty()) scanEstado = estado
                        if (tecnico.isNotEmpty()) scanTecnico = tecnico
                        
                        processedCount++
                        if (processedCount == uris.size) finalizeProcessing()
                    }
                    override fun onError(e: Exception) {
                        processedCount++
                        if (processedCount == uris.size) finalizeProcessing()
                    }
                })
            } else {
                processedCount++
                if (processedCount == uris.size) finalizeProcessing()
            }
        }
    }

    private fun finalizeProcessing() {
        isProcessing = false
        if (scanOrden.isNotEmpty() || scanOt.isNotEmpty() || scanTecnico.isNotEmpty()) {
            showDialog = true
        } else {
            Toast.makeText(this, "No se encontraron datos útiles.", Toast.LENGTH_LONG).show()
        }
    }

    private fun getBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri))
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(contentResolver, uri)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun enviarExcel(ord: String, ot: String, no: String, est: String, tec: String) {
        Toast.makeText(this, "Enviando a Base de Datos Central...", Toast.LENGTH_SHORT).show()
        val payload = PhoenixPayload(ord, ot, no, est, tec)
        RetrofitClient.instance.sendData(payload).enqueue(object : Callback<PhoenixResponse> {
            override fun onResponse(call: Call<PhoenixResponse>, response: Response<PhoenixResponse>) {}
            override fun onFailure(call: Call<PhoenixResponse>, t: Throwable) {}
        })
        WhatsappHelper.shareToWhatsapp(this, no, tec, est)
    }

    private fun resetData() {
        scanOrden = ""; scanOt = ""; scanNodo = ""; scanEstado = ""; scanTecnico = ""
    }
}
