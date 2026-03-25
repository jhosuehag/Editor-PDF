package com.jhosue.editorpdf.data.models

import android.graphics.PointF
import android.graphics.RectF

/**
 * Tipos de formas geométricas disponibles para dibujar.
 */
enum class ShapeType {
    RECTANGULO,
    CIRCULO,
    FLECHA,
    LINEA
}

/**
 * Clase sellada que representa todos los tipos de anotaciones
 * que se pueden superponer sobre una página PDF.
 * Cada anotación tiene un ID único y está ligada a una página específica.
 */
sealed class AnnotationElement {
    abstract val id: String
    abstract val pageIndex: Int

    /**
     * Resaltado de texto: uno o más rectángulos semitransparentes.
     * @property rects Lista de rectángulos a resaltar.
     * @property color Color ARGB del resaltado.
     */
    data class Highlight(
        override val id: String,
        override val pageIndex: Int,
        val rects: List<RectF>,
        val color: Int
    ) : AnnotationElement()

    /**
     * Subrayado: igual estructura que el resaltado.
     */
    data class Underline(
        override val id: String,
        override val pageIndex: Int,
        val rects: List<RectF>,
        val color: Int
    ) : AnnotationElement()

    /**
     * Tachado: igual estructura que el resaltado.
     */
    data class Strikethrough(
        override val id: String,
        override val pageIndex: Int,
        val rects: List<RectF>,
        val color: Int
    ) : AnnotationElement()

    /**
     * Dibujo libre: una serie de puntos conectados.
     * @property puntos Lista de coordenadas que forman el trazo.
     * @property color Color ARGB del trazo.
     * @property grosor Grosor del trazo en píxeles.
     */
    data class FreeDrawing(
        override val id: String,
        override val pageIndex: Int,
        val puntos: List<PointF>,
        val color: Int,
        val grosor: Float
    ) : AnnotationElement()

    /**
     * Texto libre: texto posicionado en un punto específico.
     * @property texto Contenido del texto.
     * @property posicion Punto donde se coloca el texto.
     * @property color Color ARGB del texto.
     * @property tamanio Tamaño de fuente.
     */
    data class FreeText(
        override val id: String,
        override val pageIndex: Int,
        val texto: String,
        val posicion: PointF,
        val color: Int,
        val tamanio: Float
    ) : AnnotationElement()

    /**
     * Nota adhesiva: icono colapsable con texto.
     * @property texto Contenido de la nota.
     * @property posicion Punto donde se coloca la nota.
     * @property estaColapsada Si la nota está visible o solo el icono.
     */
    data class StickyNote(
        override val id: String,
        override val pageIndex: Int,
        val texto: String,
        val posicion: PointF,
        val estaColapsada: Boolean
    ) : AnnotationElement()

    /**
     * Forma geométrica: rectángulo, círculo, flecha o línea.
     * @property tipo Tipo de forma a dibujar.
     * @property puntoInicio Punto de inicio de la forma.
     * @property puntoFin Punto de fin de la forma.
     * @property color Color ARGB del trazo.
     * @property grosor Grosor del trazo.
     */
    data class Shape(
        override val id: String,
        override val pageIndex: Int,
        val tipo: ShapeType,
        val puntoInicio: PointF,
        val puntoFin: PointF,
        val color: Int,
        val grosor: Float
    ) : AnnotationElement()

    /**
     * Firma: imagen de firma posicionada y escalada.
     * @property bitmapBytes Bytes de la imagen PNG de la firma.
     * @property posicion Punto donde se coloca la firma.
     * @property escala Factor de escala de la firma.
     */
    data class Signature(
        override val id: String,
        override val pageIndex: Int,
        val bitmapBytes: ByteArray,
        val posicion: PointF,
        val escala: Float
    ) : AnnotationElement() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Signature
            if (id != other.id) return false
            if (pageIndex != other.pageIndex) return false
            if (!bitmapBytes.contentEquals(other.bitmapBytes)) return false
            if (posicion != other.posicion) return false
            if (escala != other.escala) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + pageIndex
            result = 31 * result + bitmapBytes.contentHashCode()
            result = 31 * result + posicion.hashCode()
            result = 31 * result + escala.hashCode()
            return result
        }
    }
}