package com.jhosue.editorpdf.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos Room de PDFix.
 * Contiene las entidades y DAOs para persistencia de datos.
 */
@Database(
    entities = [DummyEntity::class, LayerEntity::class, SignatureEntity::class, RecentPdfEntity::class],
    version = 3,
    exportSchema = false
)
abstract class PdfFixDatabase : RoomDatabase() {

    /**
     * DAO para acceder a las capas de páginas.
     */
    abstract fun layerDao(): LayerDao

    /**
     * DAO para acceder a las firmas digitales.
     */
    abstract fun signatureDao(): SignatureDao

    /**
     * DAO para acceder a los PDFs recientes.
     */
    abstract fun recentPdfDao(): RecentPdfDao

    companion object {
        private const val DATABASE_NAME = "pdfix_database"

        @Volatile
        private var INSTANCE: PdfFixDatabase? = null

        /**
         * Obtiene la instancia singleton de la base de datos.
         * @param context Contexto de la aplicación.
         * @return Instancia de PdfFixDatabase.
         */
        fun getInstance(context: Context): PdfFixDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PdfFixDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}