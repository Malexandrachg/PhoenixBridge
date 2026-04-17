package com.hansa.phoenixbridge.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OcrResultDialog(
    initialOrden: String,
    initialOt: String,
    initialNodo: String,
    initialEstado: String,
    initialNombreTecnico: String, // ¡NUEVO!
    onDismiss: () -> Unit,
    onSend: (orden: String, ot: String, nodo: String, estado: String, nombre: String) -> Unit
) {
    // Variables de estado editables por el usuario
    var orden by remember { mutableStateOf(initialOrden) }
    var ot by remember { mutableStateOf(initialOt) }
    var nodo by remember { mutableStateOf(initialNodo) }
    var estado by remember { mutableStateOf(initialEstado) }
    var nombreTecnico by remember { mutableStateOf(initialNombreTecnico) } // !NUEVO!

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Revisar Datos Capturados", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            Text("Puedes modificar los campos antes de enviarlos a Excel:", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = orden,
                onValueChange = { orden = it },
                label = { Text("N° Orden Phoenix") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = ot,
                onValueChange = { ot = it },
                label = { Text("OT (Número)") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = nodo,
                onValueChange = { nodo = it },
                label = { Text("Nodo / Código") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = nombreTecnico,
                onValueChange = { nombreTecnico = it },
                label = { Text("Nombre del Técnico") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = estado,
                onValueChange = { estado = it },
                label = { Text("Estado") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { 
                    onSend(orden, ot, nodo, estado, nombreTecnico) 
                }) {
                    Text("Enviar a Excel")
                }
            }
        }
    }
}
