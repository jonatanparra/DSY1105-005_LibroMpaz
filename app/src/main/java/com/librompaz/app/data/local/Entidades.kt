package com.librompaz.app.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

// Cada @Entity es una tabla de SQLite. Solo guardo lo que necesita el RF-07 (cobertura
// curricular): quién entra, los cursos, las asignaturas, los OA y las sesiones con sus OA.

@Entity(tableName = "usuarios")
data class UsuarioEntity(
    @PrimaryKey val id: String,
    val nombre: String,
    val usuario: String,
    val clave: String,
    // El perfil lo guardo como texto (el nombre del enum) y lo convierto al leer.
    val perfil: String
)

@Entity(tableName = "cursos")
data class CursoEntity(
    @PrimaryKey val id: String,
    val nombre: String
)

@Entity(tableName = "asignaturas")
data class AsignaturaEntity(
    @PrimaryKey val id: String,
    val nombre: String
)

@Entity(
    tableName = "objetivos",
    foreignKeys = [
        ForeignKey(
            entity = AsignaturaEntity::class,
            parentColumns = ["id"],
            childColumns = ["asignaturaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("asignaturaId")]
)
data class ObjetivoEntity(
    @PrimaryKey val id: String,
    val asignaturaId: String,
    val codigo: String,
    val descripcion: String
)

@Entity(
    tableName = "sesiones",
    foreignKeys = [
        ForeignKey(
            entity = CursoEntity::class,
            parentColumns = ["id"],
            childColumns = ["cursoId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AsignaturaEntity::class,
            parentColumns = ["id"],
            childColumns = ["asignaturaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cursoId"), Index("asignaturaId")]
)
data class SesionEntity(
    @PrimaryKey val id: String,
    val cursoId: String,
    val asignaturaId: String,
    val fecha: String,
    val bloque: String,
    val contenido: String,
    val registradoPor: String,
    val fechaHoraRegistro: String
)

// Una sesión puede trabajar varios OA y un OA aparece en varias sesiones (muchos a muchos),
// por eso va en una tabla intermedia en vez de una lista dentro de la sesión.
@Entity(
    tableName = "sesion_oa",
    primaryKeys = ["sesionId", "oaId"],
    foreignKeys = [
        ForeignKey(
            entity = SesionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sesionId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ObjetivoEntity::class,
            parentColumns = ["id"],
            childColumns = ["oaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("oaId")]
)
data class SesionOaEntity(
    val sesionId: String,
    val oaId: String
)

// No es una tabla: es el resultado de pedirle a Room una sesión junto con sus OA.
data class SesionConOas(
    @Embedded val sesion: SesionEntity,
    @Relation(parentColumn = "id", entityColumn = "sesionId")
    val oas: List<SesionOaEntity>
)
