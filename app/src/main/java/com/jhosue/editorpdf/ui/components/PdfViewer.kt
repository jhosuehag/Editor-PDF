package com.jhosue.editorpdf.ui.components

import android.graphics.Bitmap
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jhosue.editorpdf.data.models.ShapeType
import com.jhosue.editorpdf.utils.CoordinateMapper
import kotlin.math.abs
import kotlin.math.pow

/**
 * Callbacks para el visor de PDF con anotaciones.
 */
data class PdfViewerCallbacks(
    val onTap: (Offset, Float, Float) -> Unit,
    val onDrawingStart: () -> Unit,
    val onDrawingUpdate: (Offset) -> Unit,
    val onDrawingEnd: (List<PointF>) -> Unit,
    val onShapeStart: () -> Unit,
    val onShapeUpdate: (Offset) -> Unit,
    val onShapeEnd: (PointF, PointF) -> Unit,
    val onTextoLibreConfirm: (Offset, Float, Float, String) -> Unit,
    val onNotaConfirm: (Offset, Float, Float, String) -> Unit
)

/**
 * Visor de PDF con soporte para zoom, scroll y anotaciones.
 * Maneja gestos según la herramienta activa:
 * - Zoom con pinza
 * - Scroll vertical para navegar entre páginas
 * - Tap para resaltar/subrayar/tachar
 * - Drag para dibujo libre y formas
 * - Tap para texto libre y notas
 */
@Composable
fun PdfViewer(
    bitmap: Bitmap?,
    paginaActual: Int,
    totalPaginas: Int,
    zoomLevel: Float,
    offsetY: Float,
    herramientaActiva: String?,
    colorAnotacion: Int,
    grosorDibujo: Float,
    formaSeleccionada: ShapeType,
    onZoomChange: (Float) -> Unit,
    onOffsetChange: (Float) -> Unit,
    onSiguientePagina: () -> Unit,
    onAnteriorPagina: () -> Unit,
    callbacks: PdfViewerCallbacks,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current
    var imageSize by remember { mutableStateOf(IntSize.Zero) }
    var containerHeight by remember { mutableStateOf(0f) }
    var containerWidth by remember { mutableStateOf(0f) }
    
    // Estado para dibujo en progreso
    var isDrawing by remember { mutableStateOf(false) }
    var drawingPoints by remember { mutableStateOf(listOf<PointF>()) }
    
    // Estado para forma en progreso
    var isDrawingShape by remember { mutableStateOf(false) }
    var shapeStartPoint by remember { mutableStateOf<PointF?>(null) }
    var shapeEndPoint by remember { mutableStateOf<PointF?>(null) }
    
    // Estado para diálogos de texto/nota
    var showTextoLibreDialog by remember { mutableStateOf(false) }
    var showNotaDialog by remember { mutableStateOf(false) }
    var pendingTextPosition by remember { mutableStateOf<Offset?>(null) }
    var pendingTextPositionPdf by remember { mutableStateOf<PointF?>(null) }
    
    // Texto ingresado
    var textoIngresado by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onSizeChanged { size ->
                containerHeight = size.height.toFloat()
                containerWidth = size.width.toFloat()
            }
    ) {
        if (bitmap != null) {
            val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            if (herramientaActiva == null) {
                                val newZoom = (zoomLevel * zoom).coerceIn(0.5f, 3f)
                                onZoomChange(newZoom)
                                val newOffset = offsetY + pan.y
                                onOffsetChange(newOffset)
                            }
                        }
                    }
                    .then(
                        if (herramientaActiva == "DIBUJO") {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val pdfPoint = CoordinateMapper.screenToPdf(
                                            offset, containerWidth, containerHeight,
                                            bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                                        )
                                        isDrawing = true
                                        drawingPoints = listOf(pdfPoint)
                                        callbacks.onDrawingStart()
                                    },
                                    onDrag = { _, dragAmount ->
                                        val currentPoint = drawingPoints.lastOrNull()
                                        if (currentPoint != null) {
                                            val pdfDelta = PointF(
                                                dragAmount.x / (containerWidth * zoomLevel) * bitmap.width,
                                                dragAmount.y / (containerHeight * zoomLevel) * bitmap.height
                                            )
                                            val newPoint = PointF(
                                                currentPoint.x + pdfDelta.x,
                                                currentPoint.y + pdfDelta.y
                                            )
                                            drawingPoints = drawingPoints + newPoint
                                            callbacks.onDrawingUpdate(offsetOf(dragAmount.x, dragAmount.y))
                                        }
                                    },
                                    onDragEnd = {
                                        if (drawingPoints.size >= 2) {
                                            callbacks.onDrawingEnd(drawingPoints)
                                        }
                                        isDrawing = false
                                        drawingPoints = emptyList()
                                    }
                                )
                            }
                        } else if (herramientaActiva == "FORMA") {
                            Modifier.pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val pdfPoint = CoordinateMapper.screenToPdf(
                                            offset, containerWidth, containerHeight,
                                            bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                                        )
                                        isDrawingShape = true
                                        shapeStartPoint = pdfPoint
                                        shapeEndPoint = pdfPoint
                                        callbacks.onShapeStart()
                                    },
                                    onDrag = { change, _ ->
                                        val pdfPoint = CoordinateMapper.screenToPdf(
                                            change.position, containerWidth, containerHeight,
                                            bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                                        )
                                        shapeEndPoint = pdfPoint
                                        callbacks.onShapeUpdate(change.position)
                                    },
                                    onDragEnd = {
                                        shapeStartPoint?.let { start ->
                                            shapeEndPoint?.let { end ->
                                                callbacks.onShapeEnd(start, end)
                                            }
                                        }
                                        isDrawingShape = false
                                        shapeStartPoint = null
                                        shapeEndPoint = null
                                    }
                                )
                            }
                        } else if (herramientaActiva in listOf("RESALTAR", "SUBRAYAR", "TACHAR")) {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    callbacks.onTap(offset, containerWidth, containerHeight)
                                }
                            }
                        } else if (herramientaActiva == "TEXTO_LIBRE") {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val pdfPoint = CoordinateMapper.screenToPdf(
                                        offset, containerWidth, containerHeight,
                                        bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                                    )
                                    pendingTextPosition = offset
                                    pendingTextPositionPdf = pdfPoint
                                    showTextoLibreDialog = true
                                }
                            }
                        } else if (herramientaActiva == "NOTA") {
                            Modifier.pointerInput(Unit) {
                                detectTapGestures { offset ->
                                    val pdfPoint = CoordinateMapper.screenToPdf(
                                        offset, containerWidth, containerHeight,
                                        bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                                    )
                                    pendingTextPosition = offset
                                    pendingTextPositionPdf = pdfPoint
                                    showNotaDialog = true
                                }
                            }
                        } else {
                            Modifier
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragEnd = {
                                            val threshold = containerHeight / 4
                                            when {
                                                offsetY < -threshold && paginaActual < totalPaginas - 1 -> {
                                                    onSiguientePagina()
                                                }
                                                offsetY > threshold && paginaActual > 0 -> {
                                                    onAnteriorPagina()
                                                }
                                            }
                                        }
                                    ) { _, dragAmount ->
                                        val newOffset = offsetY + dragAmount.y
                                        onOffsetChange(newOffset)
                                    }
                                }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Página $paginaActual de $totalPaginas",
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = zoomLevel
                            scaleY = zoomLevel
                            translationY = offsetY
                        }
                        .onSizeChanged { size ->
                            imageSize = size
                        },
                    contentScale = ContentScale.FillWidth
                )
                
                // Canvas para dibujar preview de annotaciones en tiempo real
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            scaleX = zoomLevel
                            scaleY = zoomLevel
                            translationY = offsetY
                        }
                ) {
                    // Preview de dibujo libre
                    if (isDrawing && drawingPoints.size >= 2) {
                        val path = Path()
                        val firstScreen = CoordinateMapper.pdfToScreen(
                            drawingPoints[0], containerWidth, containerHeight,
                            bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                        )
                        path.moveTo(firstScreen.x, firstScreen.y)
                        drawingPoints.drop(1).forEach { point ->
                            val screenPoint = CoordinateMapper.pdfToScreen(
                                point, containerWidth, containerHeight,
                                bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                            )
                            path.lineTo(screenPoint.x, screenPoint.y)
                        }
                        drawPath(path, Color(colorAnotacion), style = Stroke(width = grosorDibujo))
                    }
                    
                    // Preview de forma
                    if (isDrawingShape) {
                        val start = shapeStartPoint
                        val end = shapeEndPoint
                        if (start != null && end != null) {
                            val startScreen = CoordinateMapper.pdfToScreen(
                                start, containerWidth, containerHeight,
                                bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                            )
                            val endScreen = CoordinateMapper.pdfToScreen(
                                end, containerWidth, containerHeight,
                                bitmap.width, bitmap.height, zoomLevel, 0f, offsetY
                            )
                            val shapeColor = Color(colorAnotacion)
                        
                            when (formaSeleccionada) {
                                ShapeType.RECTANGULO -> {
                                    val topLeft = Offset(
                                        minOf(startScreen.x, endScreen.x),
                                        minOf(startScreen.y, endScreen.y)
                                    )
                                    val size = Size(
                                        abs(endScreen.x - startScreen.x),
                                        abs(endScreen.y - startScreen.y)
                                    )
                                    drawRect(shapeColor, topLeft, size, style = Stroke(width = grosorDibujo))
                                }
                                ShapeType.CIRCULO -> {
                                    val center = Offset(
                                        (startScreen.x + endScreen.x) / 2,
                                        (startScreen.y + endScreen.y) / 2
                                    )
                                    val dx = (endScreen.x - startScreen.x).toDouble()
                                    val dy = (endScreen.y - startScreen.y).toDouble()
                                    val radius = kotlin.math.sqrt(dx * dx + dy * dy).toFloat() / 2
                                    drawCircle(shapeColor, radius, center, style = Stroke(width = grosorDibujo))
                                }
                                ShapeType.LINEA -> {
                                    drawLine(shapeColor, startScreen, endScreen, strokeWidth = grosorDibujo)
                                }
                                ShapeType.FLECHA -> {
                                    drawLine(shapeColor, startScreen, endScreen, strokeWidth = grosorDibujo)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Cargando página...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }

        // Indicador de página en la parte inferior
        PageIndicator(
            paginaActual = paginaActual,
            totalPaginas = totalPaginas,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
        )
    }
    
    // Diálogo para texto libre
    if (showTextoLibreDialog) {
        AlertDialog(
            onDismissRequest = { 
                showTextoLibreDialog = false
                textoIngresado = ""
            },
            title = { Text("Agregar Texto") },
            text = {
                OutlinedTextField(
                    value = textoIngresado,
                    onValueChange = { textoIngresado = it },
                    label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pos = pendingTextPosition
                        if (textoIngresado.isNotBlank() && pos != null) {
                            callbacks.onTextoLibreConfirm(
                                pos, 
                                containerWidth, 
                                containerHeight,
                                textoIngresado
                            )
                        }
                        showTextoLibreDialog = false
                        textoIngresado = ""
                        pendingTextPosition = null
                    }
                ) {
                    Text("Colocar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTextoLibreDialog = false
                        textoIngresado = ""
                        pendingTextPosition = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Diálogo para nota adhesiva
    if (showNotaDialog) {
        AlertDialog(
            onDismissRequest = { 
                showNotaDialog = false
                textoIngresado = ""
            },
            title = { Text("Agregar Nota") },
            text = {
                OutlinedTextField(
                    value = textoIngresado,
                    onValueChange = { textoIngresado = it },
                    label = { Text("Contenido de la nota") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pos = pendingTextPosition
                        if (textoIngresado.isNotBlank() && pos != null) {
                            callbacks.onNotaConfirm(
                                pos, 
                                containerWidth, 
                                containerHeight,
                                textoIngresado
                            )
                        }
                        showNotaDialog = false
                        textoIngresado = ""
                        pendingTextPosition = null
                    }
                ) {
                    Text("Colocar")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNotaDialog = false
                        textoIngresado = ""
                        pendingTextPosition = null
                    }
                ) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Indicador de página actual en la parte inferior de la pantalla.
 */
@Composable
fun PageIndicator(
    paginaActual: Int,
    totalPaginas: Int,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = "Página ${paginaActual + 1} de $totalPaginas",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}

private fun offsetOf(x: Float, y: Float): Offset = Offset(x, y)
