package com.librompaz.app.ui.screen.utp

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.librompaz.app.model.Asignatura
import com.librompaz.app.model.Curso
import com.librompaz.app.ui.components.BarraSuperior
import com.librompaz.app.ui.components.SelectorDesplegable
import com.librompaz.app.util.LectorDeVoz
import com.librompaz.app.util.vibrar
import com.librompaz.app.viewmodel.LibroMpazViewModel
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/**
 * RF-07 (UTP): por curso y asignatura, qué OA ya se trabajaron, cuáles faltan y qué contenidos
 * se vieron en las clases registradas. El cálculo vive en el ViewModel; aquí solo se muestra.
 *
 * Recursos nativos: leer la cobertura en voz alta (texto a voz) y vibrar al tocar el botón.
 * Ninguno saca datos del teléfono (RN-01, RNF-07).
 */
@Composable
fun CoberturaCurricularScreen(navController: NavHostController, viewModel: LibroMpazViewModel) {
    val contexto = LocalContext.current

    // Los datos vienen de Room; collectAsState hace que la pantalla se actualice sola.
    val cursos by viewModel.cursosEstado.collectAsState()
    val asignaturas by viewModel.asignaturasEstado.collectAsState()
    val catalogoOa by viewModel.objetivosEstado.collectAsState()
    val sesiones by viewModel.sesiones.collectAsState()

    // Mientras el usuario no elige nada, uso el primer curso y la primera asignatura.
    var cursoElegido by remember { mutableStateOf<Curso?>(null) }
    var asignaturaElegida by remember { mutableStateOf<Asignatura?>(null) }
    val curso = cursoElegido ?: cursos.firstOrNull()
    val asignatura = asignaturaElegida ?: asignaturas.firstOrNull()

    val objetivos = remember(catalogoOa, asignatura) { viewModel.objetivosDe(asignatura?.id) }
    val trabajados = remember(sesiones, catalogoOa, curso, asignatura) {
        viewModel.oasTrabajados(curso?.id, asignatura?.id)
    }
    val clases = remember(sesiones, curso, asignatura) {
        viewModel.sesionesDe(curso?.id, asignatura?.id)
    }
    val avance = if (objetivos.isEmpty()) 0f else trabajados.size.toFloat() / objetivos.size

    // Animación: la barra y el porcentaje suben suave cuando cambia el curso o la asignatura.
    val avanceAnimado by animateFloatAsState(
        targetValue = avance,
        animationSpec = tween(durationMillis = 700),
        label = "avanceCobertura"
    )

    val mensajes = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    // Recurso nativo: texto a voz. El motor se crea al entrar a la pantalla y se libera al salir.
    val lector = remember { LectorDeVoz(contexto) }
    DisposableEffect(Unit) {
        onDispose { lector.cerrar() }
    }

    val escuchar: () -> Unit = {
        val leyo = lector.leer(viewModel.textoCoberturaParaVoz(curso?.id, asignatura?.id))
        vibrar(contexto)
        if (!leyo) {
            alcance.launch {
                mensajes.showSnackbar("La lectura en voz alta no está disponible en este dispositivo.")
            }
        }
    }

    Scaffold(
        topBar = { BarraSuperior("Cobertura curricular", navController) },
        snackbarHost = { SnackbarHost(mensajes) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SelectorDesplegable(
                etiqueta = "Curso",
                opciones = cursos,
                seleccion = curso,
                textoDe = { it.nombre },
                onSeleccion = { cursoElegido = it }
            )
            SelectorDesplegable(
                etiqueta = "Asignatura",
                opciones = asignaturas,
                seleccion = asignatura,
                textoDe = { it.nombre },
                onSeleccion = { asignaturaElegida = it }
            )

            if (objetivos.isEmpty()) {
                Text("Sin OA cargados", style = MaterialTheme.typography.titleMedium)
            } else {
                Text(
                    "OA trabajados: ${trabajados.size} de ${objetivos.size} (${(avanceAnimado * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.titleMedium
                )
                LinearProgressIndicator(progress = { avanceAnimado }, modifier = Modifier.fillMaxWidth())
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = curso != null && asignatura != null,
                onClick = escuchar
            ) {
                Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Escuchar cobertura")
            }

            objetivos.forEach { oa ->
                TarjetaOa(oa.codigo, oa.descripcion, hecho = oa.id in trabajados)
            }

            Text("Contenidos trabajados", style = MaterialTheme.typography.titleMedium)
            if (clases.isEmpty()) {
                Text(
                    "Aún no hay clases registradas para este curso y asignatura.",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            clases.forEach { clase ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("${clase.fecha} · ${clase.bloque}", style = MaterialTheme.typography.titleSmall)
                        Text(clase.contenido, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaOa(codigo: String, descripcion: String, hecho: Boolean) {
    // Animación: el color del estado cambia suave entre "Pendiente" y "Trabajado".
    val colorEstado by animateColorAsState(
        targetValue = if (hecho) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.tertiary,
        label = "colorEstadoOa"
    )
    Card(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(codigo, style = MaterialTheme.typography.titleSmall)
            Text(descripcion, style = MaterialTheme.typography.bodySmall)
            Text(
                if (hecho) "✔ Trabajado" else "Pendiente",
                color = colorEstado,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
