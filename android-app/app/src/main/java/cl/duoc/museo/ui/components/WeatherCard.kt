package cl.duoc.museo.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.duoc.museo.data.repository.DayForecast
import cl.duoc.museo.data.repository.WeatherInfo
import cl.duoc.museo.ui.theme.TealLight
import cl.duoc.museo.ui.viewmodel.WeatherUiState

/**
 * Tarjeta con el clima de la ciudad del museo. Los datos vienen de la API externa Open-Meteo
 * (consumida con Retrofit en WeatherRepository) y no se mezclan con los datos propios.
 */
@Composable
fun WeatherCard(state: WeatherUiState, city: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = TealLight)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Clima ahora en $city", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(targetState = state, label = "weather") { s ->
                when (s) {
                    is WeatherUiState.Loading -> Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Consultando Open-Meteo…", style = MaterialTheme.typography.bodySmall)
                    }
                    is WeatherUiState.Error -> Column {
                        Text(s.message, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onRetry) { Text("Reintentar") }
                    }
                    is WeatherUiState.Success -> WeatherContent(s.info)
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Fuente: Open-Meteo (API externa)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeatherContent(info: WeatherInfo) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(info.emoji, fontSize = 40.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text("${info.temperature}°C", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(info.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "Sensación ${info.apparentTemperature}°C · Humedad ${info.humidity}% · Viento ${info.windSpeed} km/h",
            style = MaterialTheme.typography.bodySmall
        )
        if (info.days.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                info.days.forEach { DayItem(it) }
            }
        }
    }
}

@Composable
private fun DayItem(day: DayForecast) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(day.label, style = MaterialTheme.typography.labelMedium)
        Text(day.emoji, fontSize = 22.sp)
        Text("${day.max}° / ${day.min}°", style = MaterialTheme.typography.bodySmall)
    }
}
