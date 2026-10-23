package com.librompaz.app.data

import com.librompaz.app.data.local.AsignaturaEntity
import com.librompaz.app.data.local.CoberturaDao
import com.librompaz.app.data.local.CursoEntity
import com.librompaz.app.data.local.ObjetivoEntity
import com.librompaz.app.data.local.SesionEntity
import com.librompaz.app.data.local.SesionOaEntity
import com.librompaz.app.data.local.UsuarioEntity
import com.librompaz.app.model.Asignatura
import com.librompaz.app.model.Curso
import com.librompaz.app.model.ObjetivoAprendizaje
import com.librompaz.app.model.Perfil
import com.librompaz.app.model.SesionClase
import com.librompaz.app.model.Usuario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// El Repository es el único que habla con el DAO. El ViewModel le pide datos a él y recibe
// los modelos de siempre (Curso, SesionClase...), así no conoce nada de Room.
class LibroMpazRepository(private val dao: CoberturaDao) {

    val cursos: Flow<List<Curso>> = dao.cursos().map { lista ->
        lista.map { Curso(it.id, it.nombre) }
    }

    val asignaturas: Flow<List<Asignatura>> = dao.asignaturas().map { lista ->
        lista.map { Asignatura(it.id, it.nombre) }
    }

    val objetivos: Flow<List<ObjetivoAprendizaje>> = dao.objetivos().map { lista ->
        lista.map { ObjetivoAprendizaje(it.id, it.asignaturaId, it.codigo, it.descripcion) }
    }

    val sesiones: Flow<List<SesionClase>> = dao.sesiones().map { lista ->
        lista.map {
            SesionClase(
                id = it.sesion.id,
                cursoId = it.sesion.cursoId,
                asignaturaId = it.sesion.asignaturaId,
                fecha = it.sesion.fecha,
                bloque = it.sesion.bloque,
                oaIds = it.oas.map { relacion -> relacion.oaId },
                contenido = it.sesion.contenido,
                registradoPor = it.sesion.registradoPor,
                fechaHoraRegistro = it.sesion.fechaHoraRegistro
            )
        }
    }

    // Devuelve el usuario si el correo y la clave coinciden con uno guardado, o null si no.
    suspend fun autenticar(usuario: String, clave: String): Usuario? {
        val encontrado = dao.buscarUsuario(usuario) ?: return null
        if (encontrado.clave != clave) return null
        return Usuario(
            id = encontrado.id,
            nombre = encontrado.nombre,
            usuario = encontrado.usuario,
            clave = encontrado.clave,
            perfil = Perfil.valueOf(encontrado.perfil)
        )
    }

    // La primera vez que se abre la app la base está vacía: la lleno con los datos ficticios.
    // Las veces siguientes ya hay datos y no hago nada (así sobreviven al cerrar la app).
    suspend fun sembrarSiVacio() {
        if (dao.contarCursos() > 0) return
        dao.sembrar(
            usuarios = MockData.usuarios.map {
                UsuarioEntity(it.id, it.nombre, it.usuario, it.clave, it.perfil.name)
            },
            cursos = MockData.cursos.map { CursoEntity(it.id, it.nombre) },
            asignaturas = MockData.asignaturas.map { AsignaturaEntity(it.id, it.nombre) },
            objetivos = MockData.objetivos.map {
                ObjetivoEntity(it.id, it.asignaturaId, it.codigo, it.descripcion)
            },
            sesiones = MockData.sesiones.map {
                SesionEntity(
                    it.id, it.cursoId, it.asignaturaId, it.fecha, it.bloque,
                    it.contenido, it.registradoPor, it.fechaHoraRegistro
                )
            },
            sesionOas = MockData.sesiones.flatMap { sesion ->
                sesion.oaIds.map { oaId -> SesionOaEntity(sesion.id, oaId) }
            }
        )
    }
}
