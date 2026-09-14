package ec.edu.uteq.scli.mobile.features.qr.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import ec.edu.uteq.scli.mobile.features.qr.data.LaboratorioDetalle
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
            Text(
                text = "Se necesita permiso de cámara para escanear el código QR de asistencia o del laboratorio.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { solicitarPermiso.launch(Manifest.permission.CAMERA) }) {
                Text("Conceder permiso")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            }) {
                Text("Abrir configuración de la app")
            }
        }
        state.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val mensajeError = when (state.error) {
                QrError.INVALIDO -> "El código QR no es válido para asistencia ni corresponde a un laboratorio conocido."
                QrError.RED -> "No se pudo conectar con el servicio. Comprueba tu conexión a Internet."
                QrError.SERVICIO -> "El servicio no está disponible en este momento. Inténtalo de nuevo."
                QrError.REGISTRO_NO_DISPONIBLE -> "La sesión de asistencia ya no está disponible."
                QrError.YA_REGISTRADO -> "Tu asistencia ya fue registrada previamente en esta sesión."
                QrError.EXPIRADO -> "El código QR o la sesión de asistencia ha expirado."
                QrError.NO_AUTORIZADO -> "No tienes permiso para registrar asistencia en esta sesión."
                QrError.SOLO_ESTUDIANTES -> "Este código es para registro de asistencia de estudiantes."
                null -> "No se pudo procesar el código QR."
            }
            Text(
                text = mensajeError,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = viewModel::reintentar) { Text("Reintentar escaneo") }
        }
        state.asistenciaRegistrada -> Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "¡Asistencia registrada exitosamente!",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tu presencia ha sido confirmada en el sistema.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = viewModel::reintentar) { Text("Aceptar") }
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
            executor.shutdown()
            runCatching { cameraProvider.get().unbindAll() }
        }
    }
    AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())
}

@Composable
private fun QrDetailContent(detalle: LaboratorioDetalle, onScanAgain: () -> Unit) {
    val lab = detalle.laboratorio
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = lab.nombre, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Código: ${lab.codigo}")
        Text(text = "Capacidad: ${lab.capacidad} personas")
        Text(text = "Estado: ${lab.estado}")
        if (detalle.equipos.isNotEmpty()) {
            Text(text = "Equipos (${detalle.equipos.size}):", style = MaterialTheme.typography.titleMedium)
            for (equipo in detalle.equipos) {
                val nombreEquipo = listOfNotNull(equipo.marca, equipo.modelo).filter { it.isNotBlank() }.joinToString(" ").ifBlank { equipo.codigoInventario ?: equipo.id }
                Text(text = "• $nombreEquipo — ${equipo.estado ?: ""}")
            }
        } else {
            Text(text = "Sin equipos registrados")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onScanAgain, modifier = Modifier.fillMaxWidth()) {
            Text("Escanear otro código")
        }
    }
}
