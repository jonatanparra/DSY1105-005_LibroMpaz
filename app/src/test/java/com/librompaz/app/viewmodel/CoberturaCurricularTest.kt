package com.librompaz.app.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** RF-07: UTP revisa la cobertura curricular por curso (criterios de aceptación). */
class CoberturaCurricularTest {

    private val viewModel = LibroMpazViewModel()

    @Test
    fun `3 basico A Matematica con la sesion S1 tiene 1 de 3 OA trabajados`() {
        val trabajados = viewModel.oasTrabajados("C3A", "MAT")

        assertEquals(setOf("OA-MAT-1"), trabajados)
        assertEquals(3, viewModel.objetivosDe("MAT").size)
    }

    @Test
    fun `4 basico A Matematica sin sesiones tiene 0 OA trabajados`() {
        assertTrue(viewModel.oasTrabajados("C4A", "MAT").isEmpty())
    }

    @Test
    fun `sin curso o asignatura seleccionados no hay OA trabajados`() {
        assertTrue(viewModel.oasTrabajados(null, null).isEmpty())
    }
}
