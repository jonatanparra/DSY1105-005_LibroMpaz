package com.librompaz.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        UsuarioEntity::class,
        CursoEntity::class,
        AsignaturaEntity::class,
        ObjetivoEntity::class,
        SesionEntity::class,
        SesionOaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun coberturaDao(): CoberturaDao

    companion object {
        // Una sola instancia de la base para toda la app (abrirla varias veces es caro).
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(contexto: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    contexto.applicationContext,
                    AppDatabase::class.java,
                    "libro_mpaz.db"
                ).build().also { instancia = it }
            }
    }
}
