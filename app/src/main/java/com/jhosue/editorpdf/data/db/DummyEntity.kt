package com.jhosue.editorpdf.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad placeholder para la base de datos Room.
 * Las entidades reales se agregarán en subfases posteriores.
 */
@Entity(tableName = "dummy")
data class DummyEntity(
    @PrimaryKey
    val id: Int = 0
)