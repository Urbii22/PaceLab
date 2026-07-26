package com.urbii.pacelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.room.Room
import com.urbii.pacelab.data.healthconnect.HealthConnectManager
import com.urbii.pacelab.data.healthconnect.HealthConnectDataSource
import com.urbii.pacelab.data.local.PaceLabDatabase
import com.urbii.pacelab.data.repository.RoomWorkoutRepository
import com.urbii.pacelab.data.repository.SyncStatus
import com.urbii.pacelab.domain.model.ExerciseType
import com.urbii.pacelab.domain.model.TimeSeriesSample
import com.urbii.pacelab.domain.model.Workout
import com.urbii.pacelab.domain.usecase.aggregateWeekly
import com.urbii.pacelab.domain.usecase.paceFromWorkout
import com.urbii.pacelab.ui.theme.PaceLabTheme
import androidx.health.connect.client.PermissionController
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = Room.databaseBuilder(applicationContext, PaceLabDatabase::class.java, "pacelab.db").build()
        val repository = RoomWorkoutRepository(database, HealthConnectDataSource(applicationContext))
        setContent { PaceLabTheme { PaceLabApp(repository) } }
    }
}

private data class Destination(val route: String, val label: String, val icon: @Composable () -> Unit)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun PaceLabApp(repository: RoomWorkoutRepository) {
    val navController = rememberNavController()
    val workouts by repository.workouts.collectAsStateWithLifecycle(emptyList())
    val syncStatus by repository.lastSync.collectAsStateWithLifecycle()
    val destinations = listOf(
        Destination("dashboard", "Resumen") { Icon(Icons.Default.Home, contentDescription = null) },
        Destination("activities", "Actividades") { Icon(Icons.Default.List, contentDescription = null) },
        Destination("progress", "Progreso") { Icon(Icons.Default.Insights, contentDescription = null) },
        Destination("settings", "Ajustes") { Icon(Icons.Default.Settings, contentDescription = null) },
    )
    LaunchedEffect(Unit) { repository.seedIfEmpty() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("PaceLab", fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors()) },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    NavigationBarItem(
                        selected = navController.currentBackStackEntry?.destination?.route == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = destination.icon,
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = "dashboard", modifier = Modifier.padding(padding)) {
            composable("dashboard") { DashboardScreen(workouts, syncStatus, repository::sync) { id -> navController.navigate("detail/$id") } }
            composable("activities") { ActivitiesScreen(workouts) { id -> navController.navigate("detail/$id") } }
            composable("progress") { ProgressScreen(workouts) }
            composable("settings") { SettingsScreen(syncStatus, repository::sync) { navController.navigate("diagnostics") } }
            composable("diagnostics") { DiagnosticsScreen() }
            composable("detail/{id}") { entry ->
                val id = entry.arguments?.getString("id")
                workouts.firstOrNull { it.id == id }?.let { DetailScreen(it) }
            }
        }
    }
}

@Composable
private fun DashboardScreen(workouts: List<Workout>, sync: SyncStatus, onSync: suspend () -> Unit, onOpen: (String) -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val runs = workouts.filter { it.exerciseType == ExerciseType.RUNNING }
    val weekDistance = runs.filter { it.startTime.isAfter(java.time.Instant.now().minusSeconds(7 * 86400)) }.sumOf { it.distanceMeters ?: 0.0 }
    val weekTime = runs.filter { it.startTime.isAfter(java.time.Instant.now().minusSeconds(7 * 86400)) }.sumOf { it.activeDurationSeconds ?: it.elapsedDurationSeconds ?: 0.0 }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Tu entrenamiento, en contexto", style = MaterialTheme.typography.headlineSmall)
            Text("Datos locales de Health Connect · running primero", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Km esta semana", formatKm(weekDistance), Modifier.weight(1f))
                MetricCard("Tiempo", formatDuration(weekTime), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Carreras", runs.size.toString(), Modifier.weight(1f))
                MetricCard("Último VO₂max", runs.firstOrNull { it.vo2Max != null }?.vo2Max?.let { "%.1f".format(Locale.US, it) } ?: "No disponible", Modifier.weight(1f))
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if (sync.inProgress) "Sincronización en curso" else sync.message ?: "Health Connect listo", fontWeight = FontWeight.SemiBold)
                        Text("La sincronización es manual y no modifica Samsung Health", style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { scope.launch { onSync() } }) { Icon(Icons.Default.Refresh, contentDescription = "Sincronizar") }
                }
            }
        }
        item { Text("Última actividad", style = MaterialTheme.typography.titleLarge) }
        workouts.firstOrNull()?.let { workout -> item { WorkoutRow(workout, onOpen) } }
        item { Text("Volumen reciente", style = MaterialTheme.typography.titleLarge) }
        item { WeeklyMiniChart(workouts) }
    }
}

@Composable
private fun ActivitiesScreen(workouts: List<Workout>, onOpen: (String) -> Unit) {
    var selected by rememberSaveable { mutableStateOf("Todos") }
    val filtered = workouts.filter { selected == "Todos" || it.exerciseType.label == selected }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text("Actividades", style = MaterialTheme.typography.headlineSmall) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Todos", "Running", "Walking", "Cycling", "Hiking").forEach { label ->
                    FilterChip(selected = selected == label, onClick = { selected = label }, label = { Text(label) })
                }
            }
        }
        items(filtered, key = { it.id }) { workout -> WorkoutRow(workout, onOpen) }
        if (filtered.isEmpty()) item { EmptyState("No hay actividades para este filtro") }
    }
}

@Composable
private fun ProgressScreen(workouts: List<Workout>) {
    val summaries = aggregateWeekly(workouts, ZoneOffset.UTC)
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Progreso", style = MaterialTheme.typography.headlineSmall) }
        item { Text("Agregados semanales calculados desde la copia local", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        item { WeeklyChart(summaries.map { it.distanceMeters / 1000.0 }) }
        items(summaries.reversed()) { summary ->
            Card {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(summary.weekStart.toString())
                    Text("%.1f km · %s · %d sesiones".format(Locale.US, summary.distanceMeters / 1000.0, formatDuration(summary.durationSeconds), summary.workoutCount))
                }
            }
        }
    }
}

@Composable
private fun SettingsScreen(sync: SyncStatus, onSync: suspend () -> Unit, onDiagnostics: () -> Unit) {
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) }
        item { SettingCard("Health Connect", if (sync.error == null) "Fuente local preparada" else sync.error!!) }
        item { SettingCard("Sincronización", sync.message ?: "Al abrir y manual") }
        item {
            Button(onClick = { scope.launch { onSync() } }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Sincronizar ahora")
            }
        }
        item { Button(onClick = onDiagnostics, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Default.Analytics, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Abrir diagnósticos") } }
        item { SettingCard("Privacidad", "Sin backend, sin analítica y sin permisos de escritura") }
        item { SettingCard("Backup", "Formato .pacelab versionado: preparado para el siguiente milestone") }
        item { Text("Versión 0.1.0 · diagnóstico y fundación", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DiagnosticsScreen() {
    val context = androidx.compose.ui.platform.LocalContext.current
    val manager = remember { HealthConnectManager(context) }
    var permissions by remember { mutableStateOf<Set<String>>(emptySet()) }
    LaunchedEffect(Unit) { permissions = manager.grantedPermissions() }
    val contractClient = manager.client
    val permissionLauncher = if (contractClient != null) {
        androidx.activity.compose.rememberLauncherForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
        ) { granted: Set<String> -> permissions = granted }
    } else {
        null
    }
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Diagnóstico Health Connect", style = MaterialTheme.typography.headlineSmall) }
        item { SettingCard("Disponibilidad", manager.availability().name) }
        item { SettingCard("Permisos de lectura", "${permissions.intersect(HealthConnectManager.requiredReadPermissions).size}/${HealthConnectManager.requiredReadPermissions.size}") }
        item { Text("La app leerá sesiones finalizadas y dejará constancia del origen y timestamp de cada registro.") }
        if (permissionLauncher != null) {
            item {
                Button(onClick = { permissionLauncher.launch(HealthConnectManager.requiredReadPermissions) }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Favorite, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Solicitar permisos")
                }
            }
        }
        item { SettingCard("Salida", "Importación real pendiente de probar en un teléfono con Samsung Health sincronizado") }
    }
}

@Composable
private fun DetailScreen(workout: Workout) {
    val pace = paceFromWorkout(workout)
    LazyColumn(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsRun, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                Spacer(Modifier.width(10.dp))
                Column { Text(workout.exerciseType.label, style = MaterialTheme.typography.headlineSmall); Text(formatDate(workout.startTime)) }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Distancia", formatKm(workout.distanceMeters), Modifier.weight(1f))
                MetricCard("Duración", formatDuration(workout.activeDurationSeconds ?: workout.elapsedDurationSeconds ?: 0.0), Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Ritmo", pace?.let(::formatPace) ?: "No disponible", Modifier.weight(1f))
                MetricCard("FC", workout.averageHeartRateBpm?.let { "%.0f ppm".format(Locale.US, it) } ?: "No disponible", Modifier.weight(1f))
            }
        }
        item { Text("Serie temporal", style = MaterialTheme.typography.titleLarge) }
        item { TimelineChart(workout.heartRateSamples, MaterialTheme.colorScheme.primary) }
        item { Text("Métricas derivadas solo cuando la resolución lo permite", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun WorkoutRow(workout: Workout, onOpen: (String) -> Unit) {
    Card(onClick = { onOpen(workout.id) }) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (workout.exerciseType == ExerciseType.RUNNING) Icons.Default.DirectionsRun else Icons.Default.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(workout.exerciseType.label, fontWeight = FontWeight.SemiBold)
                Text(formatDate(workout.startTime), style = MaterialTheme.typography.bodySmall)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatKm(workout.distanceMeters))
                Text(paceFromWorkout(workout)?.let(::formatPace) ?: formatDuration(workout.elapsedDurationSeconds ?: 0.0), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) { Column(Modifier.padding(14.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Spacer(Modifier.height(4.dp)); Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
}

@Composable
private fun SettingCard(label: String, value: String) { Card { Column(Modifier.padding(16.dp)) { Text(label, fontWeight = FontWeight.SemiBold); Text(value, color = MaterialTheme.colorScheme.onSurfaceVariant) } } }

@Composable
private fun EmptyState(text: String) { Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) { Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun TimelineChart(samples: List<TimeSeriesSample>, color: Color) {
    Card { Canvas(Modifier.fillMaxWidth().height(180.dp).padding(16.dp)) {
        if (samples.size < 2) return@Canvas
        val max = samples.maxOf { it.value }; val min = samples.minOf { it.value }; val span = (max - min).takeIf { it > 0 } ?: 1.0
        val path = Path()
        samples.forEachIndexed { index, sample ->
            val x = size.width * index / (samples.lastIndex.coerceAtLeast(1)).toFloat()
            val y = size.height * (1f - ((sample.value - min) / span).toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
        drawCircle(color, radius = 5f, center = Offset(0f, size.height * (1f - ((samples.first().value - min) / span).toFloat())))
    } }
}

@Composable
private fun WeeklyMiniChart(workouts: List<Workout>) { WeeklyChart(aggregateWeekly(workouts).map { it.distanceMeters / 1000.0 }) }

@Composable
private fun WeeklyChart(values: List<Double>) {
    val lineColor = MaterialTheme.colorScheme.tertiary
    Card { Canvas(Modifier.fillMaxWidth().height(150.dp).padding(16.dp)) {
        if (values.size < 2) return@Canvas
        val max = values.maxOrNull()?.takeIf { it > 0 } ?: 1.0
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = size.width * index / (values.lastIndex.coerceAtLeast(1)).toFloat()
            val y = size.height * (1f - (value / max).toFloat())
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
    } }
}

private val ExerciseType.label: String get() = when (this) { ExerciseType.RUNNING -> "Running"; ExerciseType.WALKING -> "Walking"; ExerciseType.HIKING -> "Hiking"; ExerciseType.CYCLING -> "Cycling"; ExerciseType.OTHER -> "Otro" }

private fun formatKm(meters: Double?): String = meters?.let { "%.2f km".format(Locale.US, it / 1000.0) } ?: "No disponible"
private fun formatDuration(seconds: Double): String = "${(seconds / 60).toInt()} min"
private fun formatPace(secondsPerKm: Double): String = "${(secondsPerKm / 60).toInt()}:${(secondsPerKm % 60).toInt().toString().padStart(2, '0')} /km"
private fun formatDate(instant: java.time.Instant): String = DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.getDefault()).withZone(ZoneOffset.UTC).format(instant)
