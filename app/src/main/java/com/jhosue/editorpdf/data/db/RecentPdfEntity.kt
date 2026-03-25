package com.jhosue.editorpdf.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa un PDF recientemente abierto.
 * Se usa para mostrar el historial de PDFs abiertos en HomeScreen.
 * 
 * @property id Identificador único auto-generado.
 * @property nombre Nombre del archivo PDF.
 * @property uriString URI del archivo en formato string (para Scoped Storage).
 * @property fechaApertura Timestamp de la última apertura en milisegundos.
 * @property tamanoBytes Tamaño del archivo en bytes (opcional).
 */
@Entity(
    tableName = "recent_pdfs",
    indices = [
        Index(value = ["uriString"], unique = true),
        Index(value = ["fechaApertura"])
    ]
)
data class RecentPdfEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val uriString: String,
    val fechaApertura: Long,
    val tamanoBytes: Long = 0
)