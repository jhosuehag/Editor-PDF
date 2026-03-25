package com.jhosue.editorpdf.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad de Room para persistir los datos de una capa de página.
 * Almacena las anotaciones y ediciones como JSON serializado.
 * 
 * OPTIMIZACIÓN: Incluye índice único en pageIndex para mejorar el rendimiento
 * de las consultas por página. Esto acelera significativamente la búsqueda
 * de capas cuando se navega entre páginas o se guardan cambios.
 * 
 * @property id Identificador único auto-generado.
 * @property pageIndex Índice de la página (0-based).
 * @property anotacionesJson JSON serializado de las anotaciones.
 * @property edicionesJson JSON serializado de las ediciones de contenido.
 */
@Entity(
    tableName = "layers",
    indices = [
        Index(value = ["pageIndex"], unique = true)
    ]
)
data class LayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val pageIndex: Int,
    val anotacionesJson: String,
    val edicionesJson: String
)