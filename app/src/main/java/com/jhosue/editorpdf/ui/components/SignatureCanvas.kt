package com.jhosue.editorpdf.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.sqrt

/**
 * Estado de un trazo individual en el canvas de firma.
 */
data class PathData(
    val path: androidx.compose.ui.graphics.Path,
    val color: Int,
    val strokeWidth: Float
)

/**
 * Canvas interactivo para dibujar firmas digitales.
 * Permite trazar líneas suaves con el dedo y exportarlas como Bitmap.
 * @param modifier Modificador de Compose.
 * @param onFirmaCompletada Callback que retorna el Bitmap de la firma completada.
 */
@Composable
fun SignatureCanvas(
    modifier: Modifier = Modifier,
    onFirmaCompletada: ((Bitmap) -> Unit)? = null
) {
    val pathHistory = remember { mutableStateListOf<PathData>() }
    var currentPath by remember { mutableStateOf<PathData?>(null) }
    var lastPoint by remember { mutableStateOf<Offset?>(null) }

    Box(
        modifier = modifier
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
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val newPath = androidx.compose.ui.graphics.Path().apply {
                                moveTo(offset.x, offset.y)
                            }
                            currentPath = PathData(
                                path = newPath,
                                color = Color.Black.toArgb(),
                                strokeWidth = 4.dp.toPx()
                            )
                            lastPoint = offset
                        },
                        onDrag = { change, _ ->
                            val current = currentPath ?: return@detectDragGestures
                            val previousPoint = lastPoint ?: change.position

                            val newPath = androidx.compose.ui.graphics.Path().apply {
                                addPath(current.path)
                                val midX = (previousPoint.x + change.position.x) / 2
                                val midY = (previousPoint.y + change.position.y) / 2
                                quadraticTo(
                                    previousPoint.x,
                                    previousPoint.y,
                                    midX,
                                    midY
                                )
                            }
                            currentPath = current.copy(path = newPath)
                            lastPoint = change.position
                        },
                        onDragEnd = {
                            currentPath?.let { pathHistory.add(it) }
                            currentPath = null
                            lastPoint = null
                        }
                    )
                }
        ) {
            pathHistory.forEach { pathData ->
                drawPath(
                    path = pathData.path,
                    color = Color(pathData.color),
                    style = Stroke(
                        width = pathData.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
            currentPath?.let { pathData ->
                drawPath(
                    path = pathData.path,
                    color = Color(pathData.color),
                    style = Stroke(
                        width = pathData.strokeWidth,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }
    }
}

/**
 * Exporta el contenido del canvas de firma a un Bitmap.
 * @param width Ancho del bitmap a generar.
 * @param height Alto del bitmap a generar.
 * @param pathHistory Lista de paths dibujados.
 * @return Bitmap con la firma dibujada sobre fondo transparente.
 */
fun exportarFirmaABitmap(
    width: Int,
    height: Int,
    pathHistory: List<PathData>
): Bitmap {
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    pathHistory.forEach { pathData ->
        paint.color = pathData.color
        paint.strokeWidth = pathData.strokeWidth

        val androidPath = Path()
        val pathIterator = pathData.path.iterator()
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

/**
 * Versión simplificada para exportar firma a bitmap usando Canvas nativo de Android.
 * Es más confiable para exportar paths de Compose a Bitmap.
 */
fun crearBitmapDesdePaths(
    width: Int,
    height: Int,
    paths: List<Pair<androidx.compose.ui.graphics.Path, Int>>
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
        val androidPath = Path()
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
