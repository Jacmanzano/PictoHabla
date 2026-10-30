package com.cmanzano.pictohabla.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.cmanzano.pictohabla.core.database.dao.BoardDao
import com.cmanzano.pictohabla.core.database.dao.FavoriteDao
import com.cmanzano.pictohabla.core.database.dao.HistoryDao
import com.cmanzano.pictohabla.core.database.dao.PictogramDao
import com.cmanzano.pictohabla.core.database.entity.BoardEntity
import com.cmanzano.pictohabla.core.database.entity.BoardPictogramCrossRef
import com.cmanzano.pictohabla.core.database.entity.FavoriteEntity
import com.cmanzano.pictohabla.core.database.entity.PhraseHistoryEntity
import com.cmanzano.pictohabla.core.database.entity.PictogramEntity

/**
 * Base de datos principal de PictoHabla (Room SQLite).
 * Versión 2: añade soporte para múltiples tableros personalizados y pictogramas propios con fotos locales.
 */
@Database(
    entities = [
        PictogramEntity::class,
        FavoriteEntity::class,
        PhraseHistoryEntity::class,
        BoardEntity::class,
        BoardPictogramCrossRef::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pictogramDao(): PictogramDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun historyDao(): HistoryDao
    abstract fun boardDao(): BoardDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migración segura de versión 1 a versión 2 (No destructiva).
         * Preserva al 100% los favoritos, el historial y los pictogramas existentes.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Nuevas columnas en pictograms para pictogramas personalizados
                db.execSQL("ALTER TABLE pictograms ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE pictograms ADD COLUMN speechText TEXT")
                db.execSQL("ALTER TABLE pictograms ADD COLUMN localImagePath TEXT")

                // Tabla de tableros
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS boards (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL DEFAULT '',
                        columnsCount INTEGER NOT NULL DEFAULT 3,
                        isDefault INTEGER NOT NULL DEFAULT 0,
                        createdAt INTEGER NOT NULL
                    )
                    """.trimIndent()
                )

                // Tabla de relación entre tableros y pictogramas con posiciones ordenadas
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS board_pictograms (
                        boardId INTEGER NOT NULL,
                        pictoId INTEGER NOT NULL,
                        position INTEGER NOT NULL,
                        PRIMARY KEY(boardId, pictoId),
                        FOREIGN KEY(boardId) REFERENCES boards(id) ON DELETE CASCADE,
                        FOREIGN KEY(pictoId) REFERENCES pictograms(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )

                // Índice para optimizar consultas de pictogramas ordenados en tableros
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_board_pictograms_boardId_position ON board_pictograms(boardId, position)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_board_pictograms_pictoId ON board_pictograms(pictoId)"
                )
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pictohabla_database.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
