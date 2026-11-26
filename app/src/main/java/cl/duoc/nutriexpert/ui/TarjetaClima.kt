package cl.duoc.nutriexpert.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.nutriexpert.model.ClimaUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Tarjeta del Home con el clima de Open-Meteo (consumo de API externa con Retrofit).
@Composable
fun TarjetaClima(
    state: ClimaUiState,
    onReintentar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ColoresMarca.HojaRelleno)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Hoy en Quillota",
                color = ColoresMarca.Verde,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            val clima = state.clima
            when {
                state.isLoading && clima == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = ColoresMarca.Verde
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Cargando clima…", color = ColoresMarca.Subtitulo, fontSize = 14.sp)
                }

                clima == null -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        state.error ?: "Sin datos del clima.",
                        color = ColoresMarca.Subtitulo,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onReintentar) {
                        Text("Reintentar", color = ColoresMarca.Verde, fontWeight = FontWeight.SemiBold)
                    }
                }

                else -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // La temperatura va grande: es lo primero que se mira.
                        Text(
                            "${decimal(clima.current.temperatura)}°",
                            color = ColoresMarca.Titulo,
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 20.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            DatoClima("Humedad", "${clima.current.humedad} %")
                            DatoClima("Lluvia hoy", "${decimal(clima.daily.lluviaTotal.firstOrNull() ?: 0.0)} mm")
                            // ET₀: agua que pierden el suelo y la planta en el día.
                            DatoClima(
                                "Demanda de riego",
                                "${decimal(clima.daily.evapotranspiracion.firstOrNull() ?: 0.0)} mm"
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    // Siempre se muestra la hora del dato para no confundirlo con el clima actual.
                    val hora = state.actualizadoEn?.let { horaCorta(it) } ?: ""
                    Text(
                        text = if (state.desdeCache) "Datos guardados de las $hora" else "Actualizado a las $hora",
                        color = if (state.desdeCache) ColoresMarca.Ambar else ColoresMarca.Subtitulo,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun DatoClima(titulo: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(titulo, color = ColoresMarca.Subtitulo, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Text(valor, color = ColoresMarca.Titulo, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

// Formato chileno: un decimal y coma (18,4).
private fun decimal(valor: Double): String = String.format(Locale("es", "CL"), "%.1f", valor)

private fun horaCorta(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
