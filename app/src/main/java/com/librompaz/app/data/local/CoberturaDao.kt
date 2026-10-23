package com.librompaz.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

// El DAO es donde escribo las consultas. Las que devuelven Flow avisan solas a la pantalla
// cuando cambia la tabla; las suspend se llaman desde una corrutina.
@Dao
abstract class CoberturaDao {

    @Query("SELECT * FROM usuarios WHERE LOWER(usuario) = LOWER(:usuario) LIMIT 1")
    abstract suspend fun buscarUsuario(usuario: String): UsuarioEntity?

    @Query("SELECT * FROM cursos ORDER BY nombre")
    abstract fun cursos(): Flow<List<CursoEntity>>

    @Query("SELECT * FROM asignaturas ORDER BY nombre")
    abstract fun asignaturas(): Flow<List<AsignaturaEntity>>

    @Query("SELECT * FROM objetivos ORDER BY id")
    abstract fun objetivos(): Flow<List<ObjetivoEntity>>

    // @Transaction porque Room hace dos consultas (sesiones y sus OA) y deben ser coherentes.
    @Transaction
    @Query("SELECT * FROM sesiones ORDER BY fecha, bloque")
    abstract fun sesiones(): Flow<List<SesionConOas>>

    @Query("SELECT COUNT(*) FROM cursos")
    abstract suspend fun contarCursos(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarUsuarios(usuarios: List<UsuarioEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarCursos(cursos: List<CursoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarAsignaturas(asignaturas: List<AsignaturaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarObjetivos(objetivos: List<ObjetivoEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarSesiones(sesiones: List<SesionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertarSesionOas(relaciones: List<SesionOaEntity>)

    // Cargo todo en una sola transacción y en este orden porque las tablas de abajo dependen
    // de las de arriba (llaves foráneas). Si algo falla, no queda nada a medias.
    @Transaction
    open suspend fun sembrar(
        usuarios: List<UsuarioEntity>,
        cursos: List<CursoEntity>,
        asignaturas: List<AsignaturaEntity>,
        objetivos: List<ObjetivoEntity>,
        sesiones: List<SesionEntity>,
        sesionOas: List<SesionOaEntity>
    ) {
        insertarCursos(cursos)
        insertarAsignaturas(asignaturas)
        insertarObjetivos(objetivos)
        insertarUsuarios(usuarios)
        insertarSesiones(sesiones)
        insertarSesionOas(sesionOas)
    }
}
