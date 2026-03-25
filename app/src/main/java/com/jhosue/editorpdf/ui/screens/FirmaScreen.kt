package com.jhosue.editorpdf.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jhosue.editorpdf.data.db.SignatureEntity
import com.jhosue.editorpdf.viewmodel.FirmaViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de gestión y creación de firmas digitales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirmaScreen(
    onNavigateBack: () -> Unit,
    onInsertarFirma: ((Int) -> Unit)? = null,
    firmaViewModel: FirmaViewModel = viewModel()
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val firmas by firmaViewModel.firmas.collectAsState()
    var mostrarDialogoNombre by remember { mutableStateOf(false) }
    var firmaBitmapActual by remember { mutableStateOf<Bitmap?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Firma digital", style = MaterialTheme.typography.titleMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(bottom = 24.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(24.dp)
        ) {
            NuevaFirmaCard(
                onFirmaCompletada = { bitmap ->
                    firmaBitmapActual = bitmap
                    mostrarDialogoNombre = true
                },
                onLimpiar = {
                    firmaBitmapActual = null
                }
            )

            FirmasGuardadasSection(
                firmas = firmas,
                onEliminar = { firma ->
                    firmaViewModel.eliminarFirma(firma.id)
                },
                onInsertar = { firma ->
                    onInsertarFirma?.invoke(firma.id)
                    onNavigateBack()
                }
            )
        }
    }

    if (mostrarDialogoNombre && firmaBitmapActual != null) {
        DialogoGuardarFirma(
            onConfirmar = { nombre ->
                firmaBitmapActual?.let { bitmap ->
                    firmaViewModel.guardarFirma(bitmap, nombre)
                }
                mostrarDialogoNombre = false
                firmaBitmapActual = null
            },
            onCancelar = {
                mostrarDialogoNombre = false
                firmaBitmapActual = null
            }
        )
    }
}

@Composable
fun NuevaFirmaCard(
    onFirmaCompletada: (Bitmap) -> Unit,
    onLimpiar: () -> Unit
) {
    val density = LocalDensity.current
    var pathHistory by remember { mutableStateOf<List<Pair<Path, Int>>>(emptyList()) }
    var currentPath by remember { mutableStateOf<Pair<Path, Int>?>(null) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }

    val strokeWidthDp = 3.dp
    val strokeWidthPx = with(density) { strokeWidthDp.toPx() }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Nueva firma",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, RoundedCornerShape(8.dp))
                    .drawBehind {
                        val stroke = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        drawRoundRect(
                            color = Color.LightGray,
                            style = stroke
                        )
                    }
            ) {
                CanvasCompose(
                    modifier = Modifier.fillMaxSize(),
                    strokeWidthPx = strokeWidthPx,
                    pathHistory = pathHistory,
                    currentPath = currentPath,
                    lastPoint = lastPoint,
                    onPathHistoryChange = { pathHistory = it },
                    onCurrentPathChange = { currentPath = it },
                    onLastPointChange = { lastPoint = it }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(onClick = {
                    pathHistory = emptyList()
                    currentPath = null
                    onLimpiar()
                }) {
                    Text("Limpiar")
                }
                Button(
                    onClick = {
                        if (pathHistory.isNotEmpty()) {
                            val width = 800
                            val height = 300
                            val bitmap = crearBitmapDesdePathsFirma(width, height, pathHistory)
                            onFirmaCompletada(bitmap)
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    enabled = pathHistory.isNotEmpty()
                ) {
                    Text("Guardar firma")
                }
                TextButton(onClick = {
                    pathHistory = emptyList()
                    currentPath = null
                    onLimpiar()
                }) {
                    Text("Cancelar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun CanvasCompose(
    modifier: Modifier,
    strokeWidthPx: Float,
    pathHistory: List<Pair<Path, Int>>,
    currentPath: Pair<Path, Int>?,
    lastPoint: Offset?,
    onPathHistoryChange: (List<Pair<Path, Int>>) -> Unit,
    onCurrentPathChange: (Pair<Path, Int>?) -> Unit,
    onLastPointChange: (Offset?) -> Unit
) {
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val newPath = Path().apply {
                            moveTo(offset.x, offset.y)
                        }
                        onCurrentPathChange(Pair(newPath, Color.Black.toArgb()))
                        onLastPointChange(offset)
                    },
                    onDrag = { change, _ ->
                        val current = currentPath ?: return@detectDragGestures
                        val previousPoint = lastPoint ?: change.position

                        val newPath = Path().apply {
                            addPath(current.first)
                            val midX = (previousPoint.x + change.position.x) / 2
                            val midY = (previousPoint.y + change.position.y) / 2
                            quadraticTo(
                                previousPoint.x,
                                previousPoint.y,
                                midX,
                                midY
                            )
                        }
                        onCurrentPathChange(Pair(newPath, current.second))
                        onLastPointChange(change.position)
                    },
                    onDragEnd = {
                        currentPath?.let { onPathHistoryChange(pathHistory + it) }
                        onCurrentPathChange(null)
                        onLastPointChange(null)
                    }
                )
            }
    ) {
        pathHistory.forEach { (path, color) ->
            drawPath(
                path = path,
                color = Color(color),
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
        currentPath?.let { (path, color) ->
            drawPath(
                path = path,
                color = Color(color),
                style = Stroke(
                    width = strokeWidthPx,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

private fun crearBitmapDesdePathsFirma(
    width: Int,
    height: Int,
    paths: List<Pair<Path, Int>>
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    paths.forEach { (composePath, color) ->
        paint.color = color
        val androidPath = AndroidPath()
        val pathIterator = composePath.iterator()
        while (pathIterator.hasNext()) {
            val segment = pathIterator.next()
            when (segment.type) {
                androidx.compose.ui.graphics.PathSegment.Type.Move -> {
                    androidPath.moveTo(segment.points[0], segment.points[1])
                }
                androidx.compose.ui.graphics.PathSegment.Type.Line -> {
                    androidPath.lineTo(segment.points[0], segment.points[1])
                }
                androidx.compose.ui.graphics.PathSegment.Type.Quadratic -> {
                    androidPath.quadTo(
                        segment.points[0], segment.points[1],
                        segment.points[2], segment.points[3]
                    )
                }
                androidx.compose.ui.graphics.PathSegment.Type.Conic -> {
                    androidPath.quadTo(
                        segment.points[0], segment.points[1],
                        segment.points[2], segment.points[3]
                    )
                }
                androidx.compose.ui.graphics.PathSegment.Type.Cubic -> {
                    androidPath.cubicTo(
                        segment.points[0], segment.points[1],
                        segment.points[2], segment.points[3],
                        segment.points[4], segment.points[5]
                    )
                }
                androidx.compose.ui.graphics.PathSegment.Type.Close -> {
                    androidPath.close()
                }
                androidx.compose.ui.graphics.PathSegment.Type.Done -> {}
            }
        }
        canvas.drawPath(androidPath, paint)
    }

    return bitmap
}

@Composable
private fun Arrangement.SpaceEvenly(content: @Composable () -> Unit) {
    // Placeholder - Arrangement.SpaceEvenly no tiene content en Compose
    content()
}

@Composable
fun FirmasGuardadasSection(
    firmas: List<SignatureEntity>,
    onEliminar: (SignatureEntity) -> Unit,
    onInsertar: (SignatureEntity) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Firmas guardadas",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
        )

        if (firmas.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.AddCircleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Text(
                    "Sin firmas guardadas",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Crea tu primera firma arriba",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            firmas.forEach { firma ->
                FirmaItem(
                    firma = firma,
                    onEliminar = { onEliminar(firma) },
                    onInsertar = { onInsertar(firma) }
                )
            }
        }
    }
}

@Composable
fun FirmaItem(
    firma: SignatureEntity,
    onEliminar: () -> Unit,
    onInsertar: () -> Unit
) {
    val bitmap = remember(firma.bitmapBytes) {
        BitmapFactory.decodeByteArray(
            firma.bitmapBytes, 0, firma.bitmapBytes.size
        )
    }
    val fechaFormateada = remember(firma.fechaCreacion) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(firma.fechaCreacion))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp, 44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFFF8F9FA)),
                contentAlignment = Alignment.Center
            ) {
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "Firma ${firma.nombre}",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            ) {
                Text(firma.nombre, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(
                    fechaFormateada,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Row {
                IconButton(onClick = onInsertar) {
                    Icon(
                        Icons.Default.AddCircleOutline,
                        contentDescription = "Insertar",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun DialogoGuardarFirma(
    onConfirmar: (String) -> Unit,
    onCancelar: () -> Unit
) {
    var nombre by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Guardar firma") },
        text = {
            Column {
                Text(
                    "Ingresa un nombre para identificar esta firma:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre de la firma") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nombre.isNotBlank()) {
                        onConfirmar(nombre.trim())
                    }
                },
                enabled = nombre.isNotBlank()
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}
