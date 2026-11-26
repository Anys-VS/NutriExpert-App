package cl.duoc.nutriexpert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.nutriexpert.data.RegistroTerreno
import cl.duoc.nutriexpert.domain.EdicionRegistro
import cl.duoc.nutriexpert.domain.ErroresEdicion
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistorialScreen(
    registros: List<RegistroTerreno>,
    onBorrar: (Long) -> Unit,
    onVolver: () -> Unit,
    onActualizar: (id: Long, lote: String, meta: Double, eficiencia: Double) -> Unit = { _, _, _, _ -> }
) {
    // Registro que se está editando; null = diálogo cerrado.
    var enEdicion by remember { mutableStateOf<RegistroTerreno?>(null) }

    enEdicion?.let { registro ->
        DialogoEditar(
            registro = registro,
            onGuardar = { lote, meta, eficiencia ->
                onActualizar(registro.id, lote, meta, eficiencia)
                enEdicion = null
            },
            onCancelar = { enEdicion = null }
        )
    }

    Scaffold(
        containerColor = Color.White,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            EncabezadoMarca(onVolver = onVolver)

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
                        Text(
                            "Historial",
                            color = ColoresMarca.Titulo,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Registros guardados en este teléfono",
                            color = ColoresMarca.Subtitulo,
                            fontSize = 15.sp
                        )
                    }
                }
                if (registros.isEmpty()) {
                    item {
                        Text(
                            "Todavía no hay registros",
                            color = ColoresMarca.Subtitulo,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
                items(registros, key = { it.id }) { registro ->
                    TarjetaHistorial(
                        registro = registro,
                        onBorrar = { onBorrar(registro.id) },
                        onEditar = { enEdicion = registro },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TarjetaHistorial(
    registro: RegistroTerreno,
    onBorrar: () -> Unit,
    onEditar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ColoresMarca.ChipFondo)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${registro.cultivo} · Lote ${registro.lote}",
                    color = ColoresMarca.Titulo,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    if (registro.sincronizado) "Sincronizado" else "Pendiente",
                    color = if (registro.sincronizado) ColoresMarca.Verde else ColoresMarca.Gota,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(formatoFecha(registro.creadoEn), color = ColoresMarca.Subtitulo, fontSize = 13.sp)
            Text(
                "Meta ${formatoNumero(registro.metaRendimiento)} t/há · Riego ${formatoNumero(registro.eficienciaRiego)} %",
                color = ColoresMarca.Subtitulo,
                fontSize = 13.sp
            )
            Text(
                "N ${formatoNumero(registro.n)} · P₂O₅ ${formatoNumero(registro.p2o5)} · " +
                    "K₂O ${formatoNumero(registro.k2o)} kg/há",
                color = ColoresMarca.Verde,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                if (registro.fotoUri.isBlank()) "Sin foto foliar" else "Con foto foliar",
                color = ColoresMarca.Subtitulo,
                fontSize = 13.sp
            )
            Row(modifier = Modifier.align(Alignment.End)) {
                TextButton(onClick = onEditar) {
                    Text("Editar", color = ColoresMarca.Verde, fontWeight = FontWeight.SemiBold)
                }
                TextButton(onClick = onBorrar) {
                    Text("Borrar", color = ColoresMarca.Verde)
                }
            }
        }
    }
}

// Formulario para corregir lote, meta y eficiencia. El cultivo no se cambia:
// para otro cultivo se hace un registro nuevo. Los nutrientes se recalculan al guardar.
@Composable
private fun DialogoEditar(
    registro: RegistroTerreno,
    onGuardar: (lote: String, meta: Double, eficiencia: Double) -> Unit,
    onCancelar: () -> Unit
) {
    var lote by remember { mutableStateOf(registro.lote) }
    var meta by remember { mutableStateOf(formatoNumero(registro.metaRendimiento)) }
    var eficiencia by remember { mutableStateOf(formatoNumero(registro.eficienciaRiego)) }
    var errores by remember { mutableStateOf(ErroresEdicion()) }

    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = Color.White,
        title = { Text("Editar ${registro.cultivo}", color = ColoresMarca.Titulo, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                CampoTexto(
                    valor = lote,
                    onCambio = { lote = it; errores = errores.copy(lote = null) },
                    etiqueta = "Lote o cuartel",
                    error = errores.lote
                )
                CampoTexto(
                    valor = meta,
                    onCambio = { meta = it; errores = errores.copy(metaRendimiento = null) },
                    etiqueta = "Meta de rendimiento (t/há)",
                    error = errores.metaRendimiento,
                    tipoTeclado = KeyboardType.Decimal
                )
                CampoTexto(
                    valor = eficiencia,
                    onCambio = { eficiencia = it; errores = errores.copy(eficienciaRiego = null) },
                    etiqueta = "Eficiencia de riego (%)",
                    error = errores.eficienciaRiego,
                    tipoTeclado = KeyboardType.Decimal
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                errores = EdicionRegistro.validar(lote, meta, eficiencia)
                if (!errores.hayErrores) {
                    onGuardar(lote, EdicionRegistro.aNumero(meta)!!, EdicionRegistro.aNumero(eficiencia)!!)
                }
            }) {
                Text("Guardar", color = ColoresMarca.Verde, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = ColoresMarca.Subtitulo)
            }
        }
    )
}

fun formatoFecha(millis: Long): String {
    val formato = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return formato.format(Date(millis))
}

// Coma decimal y un decimal, como se escribe en Chile.
private fun formatoNumero(valor: Double): String =
    String.format(Locale.forLanguageTag("es-CL"), "%.1f", valor)
