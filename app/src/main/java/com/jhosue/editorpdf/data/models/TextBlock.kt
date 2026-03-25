package com.jhosue.editorpdf.data.models

import android.graphics.RectF

/**
 * Representa un bloque de texto extraído del PDF.
 * Contiene la información del texto, su posición en coordenadas PDF
 * y las propiedades de formato.
 * @property id Identificador único del bloque.
 * @property texto Contenido del texto extraído.
 * @property fuente Nombre de la fuente del texto.
 * @property tamanio Tamaño de la fuente en puntos.
 * @property negrita Indica si el texto está en negrita.
 * @property cursiva Indica si el texto está en cursiva.
 * @property color Color del texto en formato ARGB.
 * @property rect Rectángulo delimitador en coordenadas PDF (origen arriba-izquierda).
 * @property pageIndex Índice de la página donde se encuentra el bloque.
 */
data class TextBlock(
    val id: String,
    val texto: String,
    val fuente: String,
    val tamanio: Float,
    val negrita: Boolean,
    val cursiva: Boolean,
    val color: Int,
    val rect: RectF,
    val pageIndex: Int
)
