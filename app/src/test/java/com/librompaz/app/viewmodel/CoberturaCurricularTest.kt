package com.librompaz.app.viewmodel

import com.librompaz.app.data.MockData
import com.librompaz.app.util.calcularOasTrabajados
import com.librompaz.app.util.textoCobertura
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-07: UTP revisa la cobertura curricular por curso (criterios de aceptación). */
class CoberturaCurricularTest {

    private fun trabajados(cursoId: String?, asignaturaId: String?) =
        calcularOasTrabajados(MockData.sesiones, MockData.objetivos, cursoId, asignaturaId)

    @Test
    fun `3 basico A Matematica con la sesion S1 tiene 1 de 3 OA trabajados`() {
        assertEquals(setOf("OA-MAT-1"), trabajados("C3A", "MAT"))
        assertEquals(3, MockData.objetivos.count { it.asignaturaId == "MAT" })
    }

    @Test
    fun `4 basico A Matematica sin sesiones tiene 0 OA trabajados`() {
        assertTrue(trabajados("C4A", "MAT").isEmpty())
    }

    @Test
    fun `sin curso o asignatura seleccionados no hay OA trabajados`() {
        assertTrue(trabajados(null, null).isEmpty())
    }

    @Test
    fun `la frase para leer en voz alta resume la cobertura de 3 basico A Matematica`() {
        val frase = textoCobertura(
            nombreCurso = "3° Básico A",
            nombreAsignatura = "Matemática",
            objetivos = MockData.objetivos.filter { it.asignaturaId == "MAT" },
            trabajados = trabajados("C3A", "MAT")
        )

        assertEquals(
            "3° Básico A, Matemática: 1 de 3 OA trabajados. Trabajados: OA 1 (ejemplo). " +
                "Pendientes: OA 2 (ejemplo), OA 3 (ejemplo).",
            frase
        )
    }
}
