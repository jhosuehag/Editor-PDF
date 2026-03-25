package com.jhosue.editorpdf.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entidad que representa una firma digital guardada.
 * 
 * OPTIMIZACIÓN: Incluye índice en fechaCreacion para mejorar el rendimiento
 * de consultas que ordenan o filtran por fecha (como "firmas recientes").
 * 
 * @property id Identificador único auto-generado.
 * @property nombre Nombre asignado a la firma.
 * @property fechaCreacion Timestamp de creación en milisegundos.
 * @property bitmapBytes Bytes de la imagen PNG de la firma.
 */
@Entity(
    tableName = "signatures",
    indices = [
        Index(value = ["fechaCreacion"])
    ]
)
data class SignatureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val nombre: String,
    val fechaCreacion: Long,
    val bitmapBytes: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SignatureEntity
        if (id != other.id) return false
        if (nombre != other.nombre) return false
        if (fechaCreacion != other.fechaCreacion) return false
        if (!bitmapBytes.contentEquals(other.bitmapBytes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + nombre.hashCode()
        result = 31 * result + fechaCreacion.hashCode()
        result = 31 * result + bitmapBytes.contentHashCode()
        return result
    }
}