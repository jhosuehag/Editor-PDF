package com.jhosue.editorpdf.data.models

/**
 * Datos de capa para una página específica.
 * Agrupa todas las anotaciones y ediciones aplicadas a una página.
 * @property pageIndex Índice de la página (0-based).
 * @property anotaciones Lista de anotaciones superpuestas.
 * @property ediciones Lista de ediciones de contenido.
 */
data class LayerData(
    val pageIndex: Int,
    val anotaciones: List<AnnotationElement> = emptyList(),
    val ediciones: List<ContentEdit> = emptyList()
)