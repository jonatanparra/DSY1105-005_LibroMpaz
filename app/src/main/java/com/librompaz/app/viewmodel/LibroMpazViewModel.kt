package com.librompaz.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.librompaz.app.data.LibroMpazRepository
import com.librompaz.app.data.MockData
import com.librompaz.app.data.local.AppDatabase
import com.librompaz.app.model.Anotacion
import com.librompaz.app.model.Asignatura
import com.librompaz.app.model.Calificacion
import com.librompaz.app.model.CargaDocente
import com.librompaz.app.model.Curso
import com.librompaz.app.model.EstadoAsistencia
import com.librompaz.app.model.Estudiante
import com.librompaz.app.model.Evaluacion
import com.librompaz.app.model.ObjetivoAprendizaje
import com.librompaz.app.model.Perfil
import com.librompaz.app.model.RegistroAuditoria
import com.librompaz.app.model.ResumenCurso
import com.librompaz.app.model.SesionClase
import com.librompaz.app.model.TipoAnotacion
import com.librompaz.app.model.Usuario
import com.librompaz.app.util.calcularOasTrabajados
import com.librompaz.app.util.textoCobertura
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel único, compartido por todas las pantallas (se crea una sola vez en AppNavigation).
 *
 * Alcance de esta entrega: RF-07 (UTP ve la cobertura curricular por curso).
 *  - Lo que usa el RF-07 viene de Room a través del Repository: usuarios (login), cursos,
 *    asignaturas, OA y sesiones.
 *  - Los demás módulos (asistencia, notas, hoja de vida, Inspectoría, Dirección) quedan
 *    simulados con MockData y siguen marcados con TODO(equipo): no son parte de este avance.
 */
class LibroMpazViewModel(private val repositorio: LibroMpazRepository) : ViewModel() {

    // =====================================================================
    // Módulo "Autenticación y alcance por rol"
    // =====================================================================

    private val _usuarioActivo = MutableStateFlow<Usuario?>(null)
    val usuarioActivo: StateFlow<Usuario?> = _usuarioActivo.asStateFlow()

    private val _errorLogin = MutableStateFlow<String?>(null)
    val errorLogin: StateFlow<String?> = _errorLogin.asStateFlow()

    // Un error por campo (se muestra junto al campo) y uno general para credenciales malas.
    private val _errorUsuario = MutableStateFlow<String?>(null)
    val errorUsuario: StateFlow<String?> = _errorUsuario.asStateFlow()

    private val _errorClave = MutableStateFlow<String?>(null)
    val errorClave: StateFlow<String?> = _errorClave.asStateFlow()

    // Las validaciones están aquí y no en la pantalla: devuelven el mensaje de error o null.
    fun validarUsuario(texto: String): String? = when {
        texto.isBlank() -> "Ingresa tu correo."
        !FORMATO_CORREO.matches(texto.trim()) -> "El correo no tiene un formato válido."
        else -> null
    }

    fun validarClave(texto: String): String? =
        if (texto.isEmpty()) "Ingresa tu clave." else null

    /**
     * Primero valido los campos; si están bien, le pregunto al Repository (que consulta Room)
     * si el correo y la clave existen. Si es así dejo al usuario activo y aviso a la pantalla
     * con onIngreso, que lo lleva a su panel según el perfil.
     */
    fun login(usuario: String, clave: String, onIngreso: (Perfil) -> Unit) {
        _errorUsuario.value = validarUsuario(usuario)
        _errorClave.value = validarClave(clave)
        _errorLogin.value = null
        if (_errorUsuario.value != null || _errorClave.value != null) return

        viewModelScope.launch {
            val encontrado = repositorio.autenticar(usuario.trim(), clave)
            if (encontrado == null) {
                _errorLogin.value = "Correo o clave incorrectos."
            } else {
                _usuarioActivo.value = encontrado
                onIngreso(encontrado.perfil)
            }
        }
    }

    // Cuando el usuario vuelve a escribir, saco los errores anteriores.
    fun limpiarErroresLogin() {
        _errorUsuario.value = null
        _errorClave.value = null
        _errorLogin.value = null
    }

    fun logout() {
        _usuarioActivo.value = null
        limpiarErroresLogin()
        _cursoSeleccionadoId.value = null
        _asignaturaSeleccionadaId.value = null
    }

    companion object {
        private val FORMATO_CORREO = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        // El ViewModel necesita su Repository, así que ya no se puede crear vacío: esta
        // fábrica arma la cadena Room -> DAO -> Repository -> ViewModel.
        fun fabrica(contexto: Context): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val dao = AppDatabase.obtener(contexto).coberturaDao()
                LibroMpazViewModel(LibroMpazRepository(dao))
            }
        }
    }

    // =====================================================================
    // Datos de referencia y selección (no es lógica de negocio)
    // =====================================================================

    // Estos datos vienen de Room. stateIn convierte el Flow del Repository en un StateFlow, así
    // las pantallas que lo observan se actualizan solas cuando cambia la base de datos.
    val cursosEstado: StateFlow<List<Curso>> =
        repositorio.cursos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val asignaturasEstado: StateFlow<List<Asignatura>> =
        repositorio.asignaturas.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val objetivosEstado: StateFlow<List<ObjetivoAprendizaje>> =
        repositorio.objetivos.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        // La primera vez la base está vacía: se llena con los datos ficticios.
        viewModelScope.launch { repositorio.sembrarSiVacio() }
    }

    // Atajos para las pantallas de los otros módulos, que leen la lista actual.
    val cursos: List<Curso> get() = cursosEstado.value
    val asignaturas: List<Asignatura> get() = asignaturasEstado.value
    private val objetivos: List<ObjetivoAprendizaje> get() = objetivosEstado.value
    private val estudiantes: List<Estudiante> = MockData.estudiantes

    private val _cursoSeleccionadoId = MutableStateFlow<String?>(null)
    val cursoSeleccionadoId: StateFlow<String?> = _cursoSeleccionadoId.asStateFlow()

    private val _asignaturaSeleccionadaId = MutableStateFlow<String?>(null)
    val asignaturaSeleccionadaId: StateFlow<String?> = _asignaturaSeleccionadaId.asStateFlow()

    fun seleccionarCurso(cursoId: String?, asignaturaId: String? = null) {
        _cursoSeleccionadoId.value = cursoId
        _asignaturaSeleccionadaId.value = asignaturaId
    }

    fun cursoPorId(id: String?): Curso? = cursos.find { it.id == id }
    fun asignaturaPorId(id: String?): Asignatura? = asignaturas.find { it.id == id }
    fun estudiantePorId(id: String?): Estudiante? = estudiantes.find { it.id == id }
    fun estudiantesDelCurso(cursoId: String?): List<Estudiante> = estudiantes.filter { it.cursoId == cursoId }
    fun objetivosDe(asignaturaId: String?): List<ObjetivoAprendizaje> = objetivos.filter { it.asignaturaId == asignaturaId }

    /**
     * TODO(equipo): devolver SOLO las cargas del docente autenticado.
     * RF: "mostrar únicamente los cursos/asignaturas bajo su asignación formal" (RBAC).
     * Hoy devuelve todas las cargas, incluso las de otros docentes.
     */
    fun misCargas(usuario: Usuario?): List<CargaDocente> = MockData.cargas

    // =====================================================================
    // Módulo "Asistencia"
    // =====================================================================

    private val _asistencias = MutableStateFlow(MockData.asistencias)

    /**
     * TODO(equipo): devolver la asistencia ya guardada del curso en esa fecha
     * (idEstudiante -> estado), para mostrarla y poder corregirla. Hoy devuelve un mapa vacío.
     * Pendiente con el CITT: si se puede corregir la asistencia de días anteriores.
     */
    fun asistenciaGuardada(cursoId: String, fecha: String): Map<String, EstadoAsistencia> = emptyMap()

    /**
     * TODO(equipo): guardar (o sobrescribir) la asistencia del curso en esa fecha, con
     * registradoPor = usuario activo y la fecha y hora actuales, y dejar un RegistroAuditoria.
     * RF: "registrar la asistencia diaria con estados presente, ausente, atrasado, justificado".
     * Hoy no hace nada.
     */
    fun guardarAsistencia(cursoId: String, fecha: String, estados: Map<String, EstadoAsistencia>) {
        // Sin implementar todavía.
    }

    /**
     * TODO(equipo): porcentaje de asistencia del curso (0 a 100) para Inspectoría y Dirección.
     * Hoy devuelve null (sin calcular).
     */
    fun porcentajeAsistencia(cursoId: String): Int? = null

    /**
     * TODO(equipo): estudiantes con inasistencias reiteradas (por ejemplo, 3 o más ausencias),
     * con su número de ausencias. RF de Inspectoría: "identificar inasistencias reiteradas".
     * Hoy devuelve una lista vacía.
     */
    fun inasistenciasReiteradas(): List<Pair<Estudiante, Int>> = emptyList()

    // =====================================================================
    // Módulo "Cobertura curricular"
    // =====================================================================

    val sesiones: StateFlow<List<SesionClase>> =
        repositorio.sesiones.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun sesionesDe(cursoId: String?, asignaturaId: String?): List<SesionClase> =
        sesiones.value.filter { it.cursoId == cursoId && it.asignaturaId == asignaturaId }

    /**
     * TODO(equipo): guardar la sesión con los OA y el contenido trabajados, con auditoría.
     * RF: "asociar OA y contenidos didácticos a cada sesión/bloque lectivo". Hoy no hace nada.
     */
    fun guardarSesion(cursoId: String, asignaturaId: String, fecha: String, bloque: String, oaIds: List<String>, contenido: String) {
        // Sin implementar todavía.
    }

    /**
     * RF-07: ids de los OA ya trabajados en el curso y asignatura (solo lectura, UTP).
     * El cálculo está en util/Cobertura.kt para poder probarlo con un test simple.
     */
    fun oasTrabajados(cursoId: String?, asignaturaId: String?): Set<String> =
        calcularOasTrabajados(sesiones.value, objetivos, cursoId, asignaturaId)

    /** Frase con la cobertura, para leerla en voz alta desde la pantalla (texto a voz). */
    fun textoCoberturaParaVoz(cursoId: String?, asignaturaId: String?): String =
        textoCobertura(
            nombreCurso = cursoPorId(cursoId)?.nombre.orEmpty(),
            nombreAsignatura = asignaturaPorId(asignaturaId)?.nombre.orEmpty(),
            objetivos = objetivosDe(asignaturaId),
            trabajados = oasTrabajados(cursoId, asignaturaId)
        )

    // =====================================================================
    // Módulo "Calificaciones"
    // =====================================================================

    private val _evaluaciones = MutableStateFlow(MockData.evaluaciones)
    val evaluaciones: StateFlow<List<Evaluacion>> = _evaluaciones.asStateFlow()

    private val _calificaciones = MutableStateFlow(MockData.calificaciones)

    fun evaluacionesDe(cursoId: String?, asignaturaId: String?): List<Evaluacion> =
        _evaluaciones.value.filter { it.cursoId == cursoId && it.asignaturaId == asignaturaId }

    fun calificacionesDe(evaluacionId: String?): List<Calificacion> =
        _calificaciones.value.filter { it.evaluacionId == evaluacionId }

    /**
     * TODO(equipo): crear la evaluación (instrumento + fecha) para el curso y la asignatura, con
     * auditoría. Validar nombre y fecha obligatorios. Hoy no hace nada.
     */
    fun crearEvaluacion(cursoId: String, asignaturaId: String, nombreInstrumento: String, fecha: String) {
        // Sin implementar todavía.
    }

    /**
     * TODO(equipo): validar una nota escrita por el docente y devolver el mensaje de error o
     * null si es válida (número con coma o punto; rango 1,0 a 7,0 si el CITT lo confirma).
     * Hoy siempre devuelve null (sin validar).
     */
    fun validarNota(texto: String): String? = null

    /**
     * TODO(equipo): guardar las notas de la evaluación (idEstudiante -> nota escrita), con
     * auditoría. RF: "registrar calificaciones asociadas a un instrumento y fecha". Hoy no hace nada.
     */
    fun guardarCalificaciones(evaluacionId: String, notas: Map<String, String>) {
        // Sin implementar todavía.
    }

    /**
     * TODO(equipo): detectar desfases u omisiones de registro de notas, por ejemplo
     * evaluaciones con estudiantes sin nota, o cursos/asignaturas sin evaluaciones.
     * RF de UTP: "identificar desfases u omisiones de registro". Hoy devuelve una lista vacía.
     */
    fun desfasesCalificaciones(): List<String> = emptyList()

    // =====================================================================
    // Módulo "Hoja de vida"
    // =====================================================================

    private val _anotaciones = MutableStateFlow(MockData.anotaciones)
    val anotaciones: StateFlow<List<Anotacion>> = _anotaciones.asStateFlow()

    fun anotacionesDe(estudianteId: String?): List<Anotacion> =
        _anotaciones.value.filter { it.estudianteId == estudianteId }

    /**
     * TODO(equipo): registrar la anotación con marca temporal y "firma digital" del docente
     * (usuario activo + fecha y hora), y dejar auditoría. Hoy no hace nada.
     */
    fun registrarAnotacion(estudianteId: String, tipo: TipoAnotacion, descripcion: String) {
        // Sin implementar todavía.
    }

    /**
     * TODO(equipo): anotaciones que requieren intervención de Inspectoría (por ejemplo,
     * negativas reiteradas de un mismo estudiante). Hoy devuelve una lista vacía.
     */
    fun anotacionesQueRequierenIntervencion(): List<Anotacion> = emptyList()

    // =====================================================================
    // Módulo "Panel administrativo" (Dirección) y auditoría
    // =====================================================================

    private val _auditoria = MutableStateFlow(MockData.auditoria)
    val auditoria: StateFlow<List<RegistroAuditoria>> = _auditoria.asStateFlow()

    /**
     * TODO(equipo): calcular las métricas consolidadas de cada curso (asistencia, sesiones,
     * notas registradas, anotaciones negativas). RF de Dirección: "estado global administrativo
     * del colegio". Hoy devuelve los cursos sin métricas (valores null).
     */
    fun resumenPorCurso(): List<ResumenCurso> = cursos.map { ResumenCurso(cursoId = it.id) }
}
