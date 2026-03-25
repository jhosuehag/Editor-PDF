package com.jhosue.editorpdf.data.models

import android.graphics.PointF
import android.graphics.RectF

/**
 * Clase sellada que representa ediciones reales al contenido del PDF.
 * A diferencia de las anotaciones que son superpuestas, estas
 * modificaciones alteran el contenido del documento.
 */
sealed class ContentEdit {
    abstract val id: String
    abstract val pageIndex: Int

    /**
     * Edición de texto existente en el PDF.
     * @property textoOriginal Texto que se va a reemplazar.
     * @property textoNuevo Nuevo texto que sustituye al original.
     * @property posicionRect Rectángulo que delimita la posición del texto.
     * @property fuente Nombre de la fuente a usar.
     * @property tamanio Tamaño de fuente.
     * @property color Color ARGB del texto.
     * @property negrita Si el texto es negrita.
     * @property cursiva Si el texto es cursiva.
     * @property alineacion Alineación del texto: "LEFT", "CENTER", "RIGHT".
     */
    data class TextEdit(
        override val id: String,
        override val pageIndex: Int,
        val textoOriginal: String,
        val textoNuevo: String,
        val posicionRect: RectF,
        val fuente: String,
        val tamanio: Float,
        val color: Int,
        val negrita: Boolean,
        val cursiva: Boolean,
        val alineacion: String
    ) : ContentEdit()

    /**
     * Reemplazo de una imagen existente en el PDF.
     * @property posicionRect Rectángulo que delimita la imagen a reemplazar.
     * @property nuevaImagenBytes Bytes de la nueva imagen PNG/JPEG.
     */
    data class ImageReplace(
        override val id: String,
        override val pageIndex: Int,
        val posicionRect: RectF,
        val nuevaImagenBytes: ByteArray
    ) : ContentEdit() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ImageReplace
            if (id != other.id) return false
            if (pageIndex != other.pageIndex) return false
            if (!nuevaImagenBytes.contentEquals(other.nuevaImagenBytes)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + pageIndex
            result = 31 * result + nuevaImagenBytes.contentHashCode()
            return result
        }
    }

    /**
     * Inserción de una nueva imagen en el PDF.
     * @property posicion Punto donde se coloca la imagen.
     * @property imagenBytes Bytes de la imagen.
     * @property ancho Ancho de la imagen.
     * @property alto Alto de la imagen.
     */
    data class ImageInsert(
        override val id: String,
        override val pageIndex: Int,
        val posicion: PointF,
        val imagenBytes: ByteArray,
        val ancho: Float,
        val alto: Float
    ) : ContentEdit() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as ImageInsert
            if (id != other.id) return false
            if (pageIndex != other.pageIndex) return false
            if (!imagenBytes.contentEquals(other.imagenBytes)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + pageIndex
            result = 31 * result + imagenBytes.contentHashCode()
            return result
        }
    }
}