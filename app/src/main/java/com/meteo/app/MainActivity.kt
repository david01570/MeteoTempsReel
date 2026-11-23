package com.meteo.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay

private val REFRESH_INTERVAL_MS = 5 * 60_000L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize()) {
                MeteoApp()
            }
        }
    }
}

@Composable
fun MeteoApp() {
    val viewModel: WeatherViewModel = viewModel()
    val state = viewModel.state.value
    var permissionDenied by remember { mutableStateOf(false) }

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val granted = grants.values.any { it }
        permissionDenied = !granted
        if (granted) viewModel.requestLocation()
    }

    LaunchedEffect(Unit) {
        if (LocationProvider.hasPermission(viewModel.getApplication())) {
            viewModel.requestLocation()
        } else {
            permissionLauncher.launch(locationPermissions)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(REFRESH_INTERVAL_MS)
            if (LocationProvider.hasPermission(viewModel.getApplication())) {
                viewModel.requestLocation()
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1565C0), Color(0xFF0D47A1))))
            .systemBarsPadding()
    ) {
        when (val s = state) {
            is WeatherUiState.Loading -> LoadingView()
            is WeatherUiState.Error -> ErrorView(
                message = s.message,
                onRetry = {
                    if (LocationProvider.hasPermission(viewModel.getApplication())) {
                        viewModel.requestLocation()
                    } else {
                        permissionLauncher.launch(locationPermissions)
                    }
                }
            )
            is WeatherUiState.Success -> WeatherView(
                weather = s.weather,
                onRefresh = { viewModel.requestLocation() }
            )
        }

        if (permissionDenied) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "La localisation est nécessaire pour afficher la météo de votre position.",
                        color = Color.White,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Button(
                    onClick = { permissionLauncher.launch(locationPermissions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Autoriser la localisation")
                }
            }
        }
    }
}

@Composable
private fun LoadingView() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = Color.White)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Recherche de votre position…", color = Color.White)
        }
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(text = "⚠️", fontSize = 48.sp, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onRetry) {
                Text("Réessayer")
            }
        }
    }
}

@Composable
private fun WeatherView(weather: CurrentWeather, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = weather.city,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = WeatherCodes.emoji(weather.weatherCode, weather.isDay),
            fontSize = 88.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${weather.temperature.toInt()}°",
            color = Color.White,
            fontSize = 88.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = WeatherCodes.describe(weather.weatherCode),
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 20.sp
        )

        if (weather.tempMax != null && weather.tempMin != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Max ${weather.tempMax!!.toInt()}° · Min ${weather.tempMin!!.toInt()}°",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Actualisation automatique toutes les 5 minutes",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                DetailRow(label = "🌡️", title = "Ressenti", value = "${weather.feelsLike.toInt()}°")
                DetailRow(label = "💧", title = "Humidité", value = "${weather.humidity} %")
                DetailRow(label = "💨", title = "Vent", value = "${weather.wind.toInt()} km/h")
                DetailRow(label = "🌧️", title = "Précipitations", value = "${weather.precipitations} mm")
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Actualiser",
                tint = Color.White
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Actualiser maintenant", color = Color.White)
        }

        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun DetailRow(label: String, title: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, fontSize = 20.sp)
            Spacer(modifier = Modifier.size(12.dp))
            Text(text = title, color = Color.White.copy(alpha = 0.9f), fontSize = 16.sp)
        }
        Text(
            text = value,
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}
