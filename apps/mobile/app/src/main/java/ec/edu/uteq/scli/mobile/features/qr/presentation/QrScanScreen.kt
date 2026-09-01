package ec.edu.uteq.scli.mobile.features.qr.presentation

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@Composable
fun QrScanScreen(viewModel: QrViewModel) {
    val context = LocalContext.current
    var permisoConcedido by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var permisoSolicitado by remember { mutableStateOf(false) }
    val solicitarPermiso = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        permisoConcedido = concedido
        permisoSolicitado = true
    }
    LaunchedEffect(Unit) {
        if (!permisoConcedido && !permisoSolicitado) solicitarPermiso.launch(Manifest.permission.CAMERA)
    }

    val state by viewModel.uiState.collectAsState()
    val detalle = state.detalle
    when {
        !permisoConcedido -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Se necesita permiso de cámara para escanear un laboratorio.")
            Button(onClick = { solicitarPermiso.launch(Manifest.permission.CAMERA) }) { Text("Conceder permiso") }
        }
        state.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                when (state.error) {
                    QrError.INVALIDO -> "El QR no contiene un UUID de laboratorio válido."
                    QrError.RED -> "No se pudo conectar con el Gateway."
                    QrError.SERVICIO -> "No se encontró el laboratorio o el servicio no está disponible."
                    null -> "No se pudo procesar el QR."
                },
            )
            Button(onClick = viewModel::reintentar) { Text("Reintentar escaneo") }
        }
        state.asistenciaRegistrada -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("Asistencia registrada correctamente.")
            Button(onClick = viewModel::reintentar) { Text("Escanear otro QR") }
        }
        detalle != null -> QrDetailContent(detalle, onScanAgain = viewModel::reintentar)
        else -> CameraPreview(viewModel::procesarQr)
    }
}

@Composable
@OptIn(markerClass = [ExperimentalGetImage::class])
private fun CameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProvider = remember { ProcessCameraProvider.getInstance(context) }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val scanner = remember { BarcodeScanning.getClient() }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner) {
        val futureListener = Runnable {
            val provider = cameraProvider.get()
            val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build().also {
                it.setAnalyzer(executor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                    } else {
                        scanner.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
                            .addOnSuccessListener { barcodes -> barcodes.firstOrNull()?.rawValue?.let(onQrDetected) }
                            .addOnCompleteListener { imageProxy.close() }
                    }
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
        }
        cameraProvider.addListener(futureListener, ContextCompat.getMainExecutor(context))
        onDispose {
            cameraProvider.get().unbindAll()
            scanner.close()
            executor.shutdown()
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun QrDetailContent(
    detalle: ec.edu.uteq.scli.mobile.features.qr.data.LaboratorioDetalle,
    onScanAgain: () -> Unit,
) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Detalle del laboratorio")
        Text(detalle.laboratorio.nombre.ifBlank { detalle.laboratorio.codigo })
        Text("Código: ${detalle.laboratorio.codigo}")
        detalle.laboratorio.capacidad?.let { Text("Capacidad: $it") }
        detalle.laboratorio.estado?.let { Text("Estado: $it") }
        detalle.laboratorio.descripcion?.takeIf { it.isNotBlank() }?.let { Text(it) }
        detalle.campus?.nombre?.let { Text("Campus: $it") }
        detalle.bloque?.nombre?.let { Text("Bloque: $it") }
        detalle.piso?.nombre?.let { Text("Piso: $it") }
        Text("Equipos: ${detalle.equipos.size}")
        detalle.equipos.forEach { equipo ->
            Text("${equipo.codigoInventario ?: equipo.marca.orEmpty()} ${equipo.modelo.orEmpty()} - ${equipo.estado.orEmpty()}")
        }
        Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) { Text("Escanear otro QR") }
    }
}
