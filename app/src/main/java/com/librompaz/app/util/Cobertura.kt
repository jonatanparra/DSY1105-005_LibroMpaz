package com.librompaz.app.util

import com.librompaz.app.model.ObjetivoAprendizaje
import com.librompaz.app.model.SesionClase

/**
 * RF-07: ids de los OA ya trabajados en el curso y la asignatura, sacados de las sesiones
 * registradas y sin repetidos. Solo cuento los OA que existen en el catálogo de la
 * asignatura, para que el avance nunca supere el total.
 *
 * Es una función pura (no usa Android ni Room), por eso se puede probar con un test simple.
 */
fun calcularOasTrabajados(
    sesiones: List<SesionClase>,
    objetivos: List<ObjetivoAprendizaje>,
    cursoId: String?,
    asignaturaId: String?
): Set<String> {
    val catalogo = objetivos.filter { it.asignaturaId == asignaturaId }.map { it.id }.toSet()
    return sesiones
        .filter { it.cursoId == cursoId && it.asignaturaId == asignaturaId }
        .flatMap { it.oaIds }
        .filter { it in catalogo }
        .toSet()
}

/**
 * RF-07: frase que se lee en voz alta con la cobertura del curso y la asignatura elegidos.
 * Por ejemplo: "3° Básico A, Matemática: 1 de 3 OA trabajados. Trabajados: OA 1 (ejemplo).
 * Pendientes: OA 2 (ejemplo), OA 3 (ejemplo)."
 */
fun textoCobertura(
    nombreCurso: String,
    nombreAsignatura: String,
    objetivos: List<ObjetivoAprendizaje>,
    trabajados: Set<String>
): String {
    if (objetivos.isEmpty()) return "$nombreCurso, $nombreAsignatura: sin OA cargados."
    val (hechos, pendientes) = objetivos.partition { it.id in trabajados }
    val partes = mutableListOf("$nombreCurso, $nombreAsignatura: ${hechos.size} de ${objetivos.size} OA trabajados.")
    if (hechos.isNotEmpty()) partes += "Trabajados: ${hechos.joinToString { it.codigo }}."
    if (pendientes.isNotEmpty()) partes += "Pendientes: ${pendientes.joinToString { it.codigo }}."
    return partes.joinToString(" ")
}
