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
    initialNombreTecnico: String,
    onDismiss: () -> Unit,
    onSend: (String, String, String, String, String) -> Unit
) {
    var orden by remember { mutableStateOf(initialOrden) }
    var ot by remember { mutableStateOf(initialOt) }
    var nodo by remember { mutableStateOf(initialNodo) }
    var estado by remember { mutableStateOf(initialEstado) }
    var nombreTecnico by remember { mutableStateOf(initialNombreTecnico) }

    Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Revisar Datos", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(value = orden, onValueChange = { orden = it }, label = { Text("Orden") })
            OutlinedTextField(value = ot, onValueChange = { ot = it }, label = { Text("OT") })
            OutlinedTextField(value = nodo, onValueChange = { nodo = it }, label = { Text("Nodo") })
            OutlinedTextField(value = nombreTecnico, onValueChange = { nombreTecnico = it }, label = { Text("Nombre") })
            OutlinedTextField(value = estado, onValueChange = { estado = it }, label = { Text("Estado") })
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
                Button(onClick = { onSend(orden, ot, nodo, estado, nombreTecnico) }) { Text("Enviar a Excel y WhatsApp") }
            }
        }
    }
}
