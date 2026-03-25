package com.jhosue.editorpdf.utils

import android.graphics.PointF
import androidx.compose.ui.geometry.Offset

/**
 * Utilidad para convertir coordenadas entre el espacio de pantalla
 * y el espacio del PDF, considerando zoom y scroll.
 */
object CoordinateMapper {

    /**
     * Convierte un punto de pantalla a coordenadas del PDF.
     * @param screenPoint Punto en coordenadas de pantalla.
     * @param screenWidth Ancho de la vista de pantalla.
     * @param screenHeight Alto de la vista de pantalla.
     * @param pdfPageWidth Ancho original de la página PDF.
     * @param pdfPageHeight Alto original de la página PDF.
     * @param zoom Nivel de zoom actual.
     * @param offsetX Offset horizontal por scroll.
     * @param offsetY Offset vertical por scroll.
     * @return Punto en coordenadas del PDF como PointF.
     */
    fun screenToPdf(
        screenPoint: Offset,
        screenWidth: Float,
        screenHeight: Float,
        pdfPageWidth: Int,
        pdfPageHeight: Int,
        zoom: Float,
        offsetX: Float,
        offsetY: Float
    ): PointF {
        val pdfX = ((screenPoint.x - offsetX) / zoom) * pdfPageWidth / screenWidth
        val pdfY = ((screenPoint.y - offsetY) / zoom) * pdfPageHeight / screenHeight
        return PointF(pdfX, pdfY)
    }

    /**
     * Convierte un punto del PDF a coordenadas de pantalla.
     * @param pdfPoint Punto en coordenadas del PDF.
     * @param screenWidth Ancho de la vista de pantalla.
     * @param screenHeight Alto de la vista de pantalla.
     * @param pdfPageWidth Ancho original de la página PDF.
     * @param pdfPageHeight Alto original de la página PDF.
     * @param zoom Nivel de zoom actual.
     * @param offsetX Offset horizontal por scroll.
     * @param offsetY Offset vertical por scroll.
     * @return Punto en coordenadas de pantalla como Offset.
     */
    fun pdfToScreen(
        pdfPoint: PointF,
        screenWidth: Float,
        screenHeight: Float,
        pdfPageWidth: Int,
        pdfPageHeight: Int,
        zoom: Float,
        offsetX: Float,
        offsetY: Float
    ): Offset {
        val screenX = (pdfPoint.x / pdfPageWidth) * screenWidth * zoom + offsetX
        val screenY = (pdfPoint.y / pdfPageHeight) * screenHeight * zoom + offsetY
        return Offset(screenX, screenY)
    }
}