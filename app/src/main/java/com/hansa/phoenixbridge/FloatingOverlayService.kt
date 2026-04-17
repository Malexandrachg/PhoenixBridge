package com.hansa.phoenixbridge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.*
import com.hansa.phoenixbridge.ocr.OcrProcessor
import com.hansa.phoenixbridge.ui.OcrResultDialog
import com.hansa.phoenixbridge.api.RetrofitClient
import com.hansa.phoenixbridge.api.PhoenixPayload
import com.hansa.phoenixbridge.api.PhoenixResponse
import com.hansa.phoenixbridge.utils.WhatsappHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FloatingOverlayService : Service(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {

    private lateinit var windowManager: WindowManager
    private lateinit var composeView: ComposeView
    
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    
    private val ocrProcessor = OcrProcessor()

    // Estado Reactivo
    private var showDialog by mutableStateOf(false)
    private var buttonText by mutableStateOf("🔴 Escanear")
    private var isCapturing by mutableStateOf(false)

    // "Memoria" de las pantallas escaneadas
    private var scanOrden by mutableStateOf("")
    private var scanOt by mutableStateOf("")
    private var scanNodo by mutableStateOf("")
    private var scanEstado by mutableStateOf("")
    private var scanTecnico by mutableStateOf("")

    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        
        createFloatingButton()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        
        val resultCode = intent?.getIntExtra("RESULT_CODE", 0) ?: 0
        val data = intent?.getParcelableExtra<Intent>("DATA")
        if (resultCode != 0 && data != null && mediaProjection == null) {
            setupMediaProjection(resultCode, data)
        }
        return START_NOT_STICKY
    }

    private fun setupMediaProjection(resultCode: Int, data: Intent) {
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)
        
        val metrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(metrics)
        val density = metrics.densityDpi
        val width = metrics.widthPixels
        val height = metrics.heightPixels

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )
        Toast.makeText(this, "¡Ojo activado! Toma la foto a la app", Toast.LENGTH_SHORT).show()
    }

    private fun captureScreenAndProcess() {
        if (imageReader == null) {
            Toast.makeText(this, "Motor visual no inicializado", Toast.LENGTH_SHORT).show()
            return
        }
        
        isCapturing = true
        buttonText = "⏳ ..."

        // Esperar un instante para que el botón refleje el tap
        Handler(Looper.getMainLooper()).postDelayed({
            val image = imageReader?.acquireLatestImage()
            if (image == null) {
                isCapturing = false
                buttonText = "🔴 Escanear"
                Toast.makeText(this, "Retenta el escáner", Toast.LENGTH_SHORT).show()
                return@postDelayed
            }

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width
            val bitmapWidth = image.width + rowPadding / pixelStride
            
            val bitmap = Bitmap.createBitmap(bitmapWidth, image.height, Bitmap.Config.ARGB_8888)
            bitmap.copyPixelsFromBuffer(buffer)
            image.close()

            val exactBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)

            ocrProcessor.processImage(exactBitmap, object : OcrProcessor.OnOcrCompleted {
                override fun onSuccess(orden: String, ot: String, nodo: String, estado: String, tecnico: String) {
                    isCapturing = false
                    
                    // Solo sobreescribir si la IA encontró texto válido en esta foto
                    if (orden.isNotEmpty()) scanOrden = orden
                    if (ot.isNotEmpty()) scanOt = ot
                    if (nodo.isNotEmpty() && nodo.length > scanNodo.length) scanNodo = nodo // Guarda el nodo más largo/detallado
                    if (estado.isNotEmpty()) scanEstado = estado
                    if (tecnico.isNotEmpty()) scanTecnico = tecnico
                    
                    showDialog = true
                    buttonText = "➕ Añadir"
                }
                override fun onError(e: Exception) {
                    isCapturing = false
                    buttonText = "🔴 Escanear"
                }
            })
        }, 500)
    }

    private fun createNotification() {
        val channelId = "floating_service_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Servicio Flotante", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
        startForeground(1, NotificationCompat.Builder(this, channelId).setContentTitle("Phoenix Bridge Activo").build())
    }

    private fun createFloatingButton() {
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 0
        params.y = 200

        composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@FloatingOverlayService)
            setViewTreeViewModelStoreOwner(this@FloatingOverlayService)
            setViewTreeSavedStateRegistryOwner(this@FloatingOverlayService)
            setContent { 
                if (showDialog) {
                    OcrResultDialog(
                        initialOrden = scanOrden,
                        initialOt = scanOt,
                        initialNodo = scanNodo,
                        initialEstado = scanEstado,
                        initialNombreTecnico = scanTecnico,
                        onDismiss = { showDialog = false },
                        onSend = { ord, ot, no, est, tec ->
                            showDialog = false
                            buttonText = "🔴 Escanear"
                            enviarExcel(ord, ot, no, est, tec)
                        }
                    )
                } else {
                    FloatingActionButton(onClick = { if (!isCapturing) captureScreenAndProcess() }) {
                        Text(buttonText, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
        windowManager.addView(composeView, params)
    }

    private fun enviarExcel(ord: String, ot: String, no: String, est: String, tec: String) {
        val payload = PhoenixPayload(ord, ot, no, est, tec)
        RetrofitClient.instance.sendData(payload).enqueue(object : Callback<PhoenixResponse> {
            override fun onResponse(call: Call<PhoenixResponse>, response: Response<PhoenixResponse>) {}
            override fun onFailure(call: Call<PhoenixResponse>, t: Throwable) {}
        })
        WhatsappHelper.shareToWhatsapp(this, no, tec, est)
        
        // Limpiamos memoria para la próxima Orden
        scanOrden = ""; scanOt = ""; scanNodo = ""; scanEstado = ""; scanTecnico = ""
    }

    override fun onDestroy() {
        super.onDestroy()
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
        if (::composeView.isInitialized) windowManager.removeView(composeView)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
}
