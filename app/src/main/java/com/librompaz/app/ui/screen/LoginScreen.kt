package com.librompaz.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.librompaz.app.R
import com.librompaz.app.model.Perfil
import com.librompaz.app.util.vibrar
import com.librompaz.app.viewmodel.LibroMpazViewModel

/**
 * Inicio de sesión. La pantalla solo muestra los campos y los errores; las validaciones y la
 * consulta a Room las hace el ViewModel. Según el perfil, AppNavigation lleva a cada panel.
 * Regla del caso: no existe acceso para estudiantes ni apoderados.
 *
 * Usuarios de prueba (ficticios): ver README.md.
 */
@Composable
fun LoginScreen(
    viewModel: LibroMpazViewModel,
    onIngresar: (Perfil) -> Unit
) {
    val contexto = LocalContext.current
    var usuario by remember { mutableStateOf("") }
    var clave by remember { mutableStateOf("") }
    var claveVisible by remember { mutableStateOf(false) }
    val errorLogin by viewModel.errorLogin.collectAsState()
    val errorUsuario by viewModel.errorUsuario.collectAsState()
    val errorClave by viewModel.errorClave.collectAsState()

    // Recurso nativo: el teléfono vibra cuando aparece un error de login.
    val hayError = errorLogin != null || errorUsuario != null || errorClave != null
    LaunchedEffect(hayError) {
        if (hayError) vibrar(contexto)
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_app),
                contentDescription = "Logo",
                modifier = Modifier.size(110.dp)
            )
            Text("Libro MPAZ", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Libro digital de clases",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedTextField(
                value = usuario,
                onValueChange = {
                    usuario = it
                    viewModel.limpiarErroresLogin()
                },
                label = { Text("Correo") },
                leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
                isError = errorUsuario != null,
                supportingText = if (errorUsuario != null) {
                    { MensajeError(errorUsuario.orEmpty()) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = clave,
                onValueChange = {
                    clave = it
                    viewModel.limpiarErroresLogin()
                },
                label = { Text("Clave") },
                leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                isError = errorClave != null,
                supportingText = if (errorClave != null) {
                    { MensajeError(errorClave.orEmpty()) }
                } else {
                    null
                },
                singleLine = true,
                visualTransformation = if (claveVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { claveVisible = !claveVisible }) {
                        Icon(
                            imageVector = if (claveVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (claveVisible) "Ocultar clave" else "Mostrar clave"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            // Animación: el mensaje de credenciales incorrectas aparece y desaparece suave.
            AnimatedVisibility(visible = errorLogin != null) {
                MensajeError(errorLogin.orEmpty())
            }

            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { viewModel.login(usuario.trim(), clave, onIngresar) }
            ) { Text("Ingresar") }

            // RNF-11: la app es una herramienta interna, sin validez oficial.
            Text(
                "Prototipo académico de uso interno",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Mensaje de error con ícono, para que el error se note también sin depender solo del color.
@Composable
private fun MensajeError(texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.Error,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(texto, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}
