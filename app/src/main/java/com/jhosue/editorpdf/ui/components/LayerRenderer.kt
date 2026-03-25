package com.jhosue.editorpdf.ui.components

import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.jhosue.editorpdf.data.models.AnnotationElement
import com.jhosue.editorpdf.data.models.LayerData
import com.jhosue.editorpdf.data.models.ShapeType
import com.jhosue.editorpdf.data.models.TextBlock
import com.jhosue.editorpdf.utils.CoordinateMapper
import kotlin.math.pow

/**
 * Compositor que renderiza las anotaciones sobre el PDF.
 * Recibe los datos de la capa y los dibuja usando Canvas.
 * Las posiciones se transforman de coordenadas PDF a pantalla
 * usando CoordinateMapper.
 * @param layerData Datos de la capa con anotaciones.
 * @param screenWidth Ancho de la pantalla.
 * @param screenHeight Alto de la pantalla.
 * @param pdfPageWidth Ancho de la página PDF.
 * @param pdfPageHeight Alto de la página PDF.
 * @param zoom Nivel de zoom actual.
 * @param offsetX Offset horizontal.
 * @param offsetY Offset vertical.
 * @param textBlocks Lista de bloques de texto a mostrar (opcional).
 * @param bloqueSeleccionado Bloque de texto seleccionado actualmente (opcional).
 * @param colorPrimario Color primario para destacar bloques seleccionados.
 * @param modifier Modificador de Compose.
 */
@Composable
fun LayerRenderer(
    layerData: LayerData,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    textBlocks: List<TextBlock> = emptyList(),
    bloqueSeleccionado: TextBlock? = null,
    colorPrimario: Color = Color(0xFF2563EB),
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Dibujar anotaciones existentes
        layerData.anotaciones.forEach { anotacion ->
            dibujarAnotacion(
                anotacion = anotacion,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                pdfPageWidth = pdfPageWidth,
                pdfPageHeight = pdfPageHeight,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY
            )
        }

        // Dibujar bloques de texto si hay
        textBlocks.forEach { bloque ->
            dibujarBloqueTexto(
                bloque = bloque,
                esSeleccionado = bloque == bloqueSeleccionado,
                screenWidth = screenWidth,
                screenHeight = screenHeight,
                pdfPageWidth = pdfPageWidth,
                pdfPageHeight = pdfPageHeight,
                zoom = zoom,
                offsetX = offsetX,
                offsetY = offsetY,
                colorPrimario = colorPrimario
            )
        }
    }
}

/**
 * Dibuja el borde de un bloque de texto.
 * Borde punteado si no está seleccionado, sólido si está seleccionado.
 */
private fun DrawScope.dibujarBloqueTexto(
    bloque: TextBlock,
    esSeleccionado: Boolean,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float,
    colorPrimario: Color
) {
    val topLeft = CoordinateMapper.pdfToScreen(
        PointF(bloque.rect.left, bloque.rect.top),
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )
    val bottomRight = CoordinateMapper.pdfToScreen(
        PointF(bloque.rect.right, bloque.rect.bottom),
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )

    val left = minOf(topLeft.x, bottomRight.x)
    val top = minOf(topLeft.y, bottomRight.y)
    val width = kotlin.math.abs(bottomRight.x - topLeft.x)
    val height = kotlin.math.abs(bottomRight.y - topLeft.y)

    if (esSeleccionado) {
        // Fondo semitransparente para bloque seleccionado
        drawRect(
            color = colorPrimario.copy(alpha = 0.1f),
            topLeft = Offset(left, top),
            size = Size(width, height)
        )
        // Borde sólido 2dp
        drawRect(
            color = colorPrimario,
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(width = 2f * zoom)
        )
    } else {
        // Borde punteado para bloques no seleccionados
        drawRect(
            color = colorPrimario.copy(alpha = 0.5f),
            topLeft = Offset(left, top),
            size = Size(width, height),
            style = Stroke(
                width = 1f * zoom,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
            )
        )
    }
}

/**
 * Dibuja una anotación específica según su tipo.
 */
private fun DrawScope.dibujarAnotacion(
    anotacion: AnnotationElement,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    when (anotacion) {
        is AnnotationElement.Highlight -> dibujarHighlight(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        is AnnotationElement.Underline -> dibujarUnderline(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        is AnnotationElement.Strikethrough -> dibujarStrikethrough(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        is AnnotationElement.FreeDrawing -> dibujarFreeDrawing(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        is AnnotationElement.FreeText -> dibujarFreeText(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        is AnnotationElement.StickyNote -> dibujarStickyNote(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        is AnnotationElement.Shape -> dibujarShape(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        is AnnotationElement.Signature -> dibujarSignature(
            anotacion, screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
    }
}

/**
 * Dibuja un resaltado: rectángulos semitransparentes.
 */
private fun DrawScope.dibujarHighlight(
    highlight: AnnotationElement.Highlight,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    val color = Color(highlight.color)
    highlight.rects.forEach { rectF ->
        val topLeft = CoordinateMapper.pdfToScreen(
            PointF(rectF.left, rectF.top),
            screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        val bottomRight = CoordinateMapper.pdfToScreen(
            PointF(rectF.right, rectF.bottom),
            screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        drawRect(
            color = color.copy(alpha = 0.4f),
            topLeft = topLeft,
            size = androidx.compose.ui.geometry.Size(
                bottomRight.x - topLeft.x,
                bottomRight.y - topLeft.y
            )
        )
    }
}

/**
 * Dibuja un subrayado.
 */
private fun DrawScope.dibujarUnderline(
    underline: AnnotationElement.Underline,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    val color = Color(underline.color)
    underline.rects.forEach { rectF ->
        val start = CoordinateMapper.pdfToScreen(
            PointF(rectF.left, rectF.bottom),
            screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        val end = CoordinateMapper.pdfToScreen(
            PointF(rectF.right, rectF.bottom),
            screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = 2f * zoom
        )
    }
}

/**
 * Dibuja un tachado.
 */
private fun DrawScope.dibujarStrikethrough(
    strikethrough: AnnotationElement.Strikethrough,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    val color = Color(strikethrough.color)
    strikethrough.rects.forEach { rectF ->
        val start = CoordinateMapper.pdfToScreen(
            PointF(rectF.left, rectF.centerY()),
            screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        val end = CoordinateMapper.pdfToScreen(
            PointF(rectF.right, rectF.centerY()),
            screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        drawLine(
            color = color,
            start = start,
            end = end,
            strokeWidth = 2f * zoom
        )
    }
}

/**
 * Dibuja trazo libre.
 */
private fun DrawScope.dibujarFreeDrawing(
    freeDrawing: AnnotationElement.FreeDrawing,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    if (freeDrawing.puntos.size < 2) return

    val color = Color(freeDrawing.color)
    val path = androidx.compose.ui.graphics.Path()

    val firstPoint = CoordinateMapper.pdfToScreen(
        freeDrawing.puntos[0],
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )
    path.moveTo(firstPoint.x, firstPoint.y)

    freeDrawing.puntos.drop(1).forEach { punto ->
        val screenPoint = CoordinateMapper.pdfToScreen(
            punto,
            screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
        )
        path.lineTo(screenPoint.x, screenPoint.y)
    }

    drawPath(
        path = path,
        color = color
    )
}

/**
 * Dibuja texto libre.
 */
private fun DrawScope.dibujarFreeText(
    freeText: AnnotationElement.FreeText,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    val screenPos = CoordinateMapper.pdfToScreen(
        freeText.posicion,
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )
    val color = Color(freeText.color).toArgb()

    val paint = Paint().apply {
        this.color = color
        textSize = freeText.tamanio * zoom
        isAntiAlias = true
    }

    drawContext.canvas.nativeCanvas.drawText(
        freeText.texto,
        screenPos.x,
        screenPos.y,
        paint
    )
}

/**
 * Dibuja una nota adhesiva (ícono).
 */
private fun DrawScope.dibujarStickyNote(
    stickyNote: AnnotationElement.StickyNote,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    val screenPos = CoordinateMapper.pdfToScreen(
        stickyNote.posicion,
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )
    val size = 24f * zoom

    drawCircle(
        color = Color(0xFFFFEB3B.toInt()),
        radius = size,
        center = screenPos
    )
}

/**
 * Dibuja una forma geométrica.
 */
private fun DrawScope.dibujarShape(
    shape: AnnotationElement.Shape,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    val start = CoordinateMapper.pdfToScreen(
        shape.puntoInicio,
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )
    val end = CoordinateMapper.pdfToScreen(
        shape.puntoFin,
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )
    val color = Color(shape.color)

    when (shape.tipo) {
        ShapeType.RECTANGULO -> {
            val topLeft = Offset(
                minOf(start.x, end.x),
                minOf(start.y, end.y)
            )
            val size = androidx.compose.ui.geometry.Size(
                kotlin.math.abs(end.x - start.x),
                kotlin.math.abs(end.y - start.y)
            )
            drawRect(color = color, topLeft = topLeft, size = size)
        }
        ShapeType.CIRCULO -> {
            val center = Offset(
                (start.x + end.x) / 2,
                (start.y + end.y) / 2
            )
            val radius = kotlin.math.sqrt(
                (end.x - start.x).toDouble().pow(2.0) +
                (end.y - start.y).toDouble().pow(2.0)
            ).toFloat() / 2
            drawCircle(color = color, radius = radius, center = center)
        }
        ShapeType.LINEA -> {
            drawLine(color = color, start = start, end = end, strokeWidth = shape.grosor * zoom)
        }
        ShapeType.FLECHA -> {
            drawLine(color = color, start = start, end = end, strokeWidth = shape.grosor * zoom)
            // TODO: Dibujar cabeza de flecha
        }
    }
}

/**
 * Dibuja una firma (imagen).
 */
private fun DrawScope.dibujarSignature(
    signature: AnnotationElement.Signature,
    screenWidth: Float,
    screenHeight: Float,
    pdfPageWidth: Int,
    pdfPageHeight: Int,
    zoom: Float,
    offsetX: Float,
    offsetY: Float
) {
    val screenPos = CoordinateMapper.pdfToScreen(
        signature.posicion,
        screenWidth, screenHeight, pdfPageWidth, pdfPageHeight, zoom, offsetX, offsetY
    )

    val bitmap = BitmapFactory.decodeByteArray(
        signature.bitmapBytes,
        0,
        signature.bitmapBytes.size
    ) ?: return

    val scaledWidth = bitmap.width * signature.escala * zoom
    val scaledHeight = bitmap.height * signature.escala * zoom

    drawImage(
        image = bitmap.asImageBitmap(),
        topLeft = screenPos,
        alpha = 1f
    )
}