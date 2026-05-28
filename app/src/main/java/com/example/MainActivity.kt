package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.Notificacion
import com.example.data.Solicitud
import com.example.ui.SolicitudViewModel
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val viewModel: SolicitudViewModel = viewModel()
                AppMainScreen(viewModel = viewModel)
            }
        }
    }
}

// Global color helper for status badges
fun getStatusColor(estado: String): Color {
    return when (estado) {
        "PENDIENTE" -> Color(0xFFE6A23C) // Warm Amber
        "APROBADO" -> Color(0xFF409EFF) // Blue
        "DESPACHADO" -> Color(0xFF909399) // Gray-blue
        "ENTREGADO" -> Color(0xFF34C759) // Forest Green
        "DEVUELTO" -> Color(0xFF00C7BE) // Teal
        "RECHAZADO" -> Color(0xFFFF3B30) // Coral Red
        else -> Color.DarkGray
    }
}

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainScreen(viewModel: SolicitudViewModel) {
    val role by viewModel.userRole.collectAsState()
    val solicitudes by viewModel.filteredSolicitudes.collectAsState()
    val allSolicitudesForStats by viewModel.solicitudes.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val unreadCount by viewModel.unreadNotificationsCount.collectAsState()
    val selectedSolicitud by viewModel.selectedSolicitud.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentDept by viewModel.currentDept.collectAsState()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showUserPicker by remember { mutableStateOf(false) }

    val simulatedUsers = listOf(
        Pair("Rodrigo Muñoz", "Finanzas"),
        Pair("Elena Salazar", "Recursos Humanos"),
        Pair("Patricio Aravena", "Jurídica"),
        Pair("Claudio Soto", "Urbanismo"),
        Pair("Ana María Pérez", "Secretaría Municipal")
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                // Main Header
                LargeTopAppBar(
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                    title = {
                        Column {
                            Text(
                                text = "Archivo Central",
                                fontWeight = FontWeight.Medium,
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Sistema de Gestión de Documentos",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        // Notification Bell Icon with Badge
                        IconButton(
                            onClick = { showNotificationsSheet = !showNotificationsSheet },
                            modifier = Modifier.testTag("notification_bell_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (unreadCount > 0) {
                                        Badge(
                                            containerColor = MaterialTheme.colorScheme.error
                                        ) {
                                            Text(text = unreadCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = "Notificaciones",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                )

                // Simulated Workspace Header (Aesthetic and Role selector)
                Surface(
                    tonalElevation = 1.dp,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "SIMULADOR:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            // Sleek Capsule Segmented control matching HTML pills
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(9999.dp))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                val isFuncionario = role == "FUNCIONARIO"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(if (isFuncionario) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                        .clickable { viewModel.setRole("FUNCIONARIO") }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("role_funcionario_chip"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Person,
                                            contentDescription = null,
                                            tint = if (isFuncionario) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Funcionario",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isFuncionario) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                val isEncargado = role == "ENCARGADO"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(9999.dp))
                                        .background(if (isEncargado) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
                                        .clickable { viewModel.setRole("ENCARGADO") }
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .testTag("role_encargado_chip"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Settings,
                                            contentDescription = null,
                                            tint = if (isEncargado) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Encargado",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (isEncargado) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Bottom line with details based on selected role
                        AnimatedVisibility(
                            visible = role == "FUNCIONARIO",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showUserPicker = true }
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .background(
                                                MaterialTheme.colorScheme.primary,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = currentUser.take(1).uppercase(),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Sesión: $currentUser",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onBackground
                                        )
                                        Text(
                                            text = "Departamento: $currentDept",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Cambiar Usuario",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = role == "ENCARGADO",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.secondary,
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Supervisor de Archivo Central",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Text(
                                        text = "Bodega Central de Expedientes Municipales",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main workspace view
            Column(modifier = Modifier.fillMaxSize()) {
                if (role == "FUNCIONARIO") {
                    FuncionarioView(viewModel = viewModel, solicitudes = solicitudes)
                } else {
                    EncargadoView(
                        viewModel = viewModel,
                        allSolicitudes = allSolicitudesForStats,
                        filteredSolicitudes = solicitudes
                    )
                }
            }

            // Notification Overlay / Sheet
            if (showNotificationsSheet) {
                NotificationsOverlay(
                    notifications = notifications,
                    onClose = { showNotificationsSheet = false },
                    onMarkRead = { viewModel.markNotificationRead(it) },
                    onMarkAllRead = { viewModel.markAllNotificationsRead() },
                    onClearAll = { viewModel.clearAllNotifications() }
                )
            }

            // User picker dropdown sheet
            if (showUserPicker) {
                AlertDialog(
                    onDismissRequest = { showUserPicker = false },
                    title = { Text("Simular Sesión de Funcionario") },
                    text = {
                        Column {
                            Text(
                                text = "Elige un funcionario para simular solicitudes y ver el flujo completo:",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            simulatedUsers.forEach { (name, dept) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.selectUser(name, dept)
                                            showUserPicker = false
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                if (currentUser == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = name.take(1).uppercase(),
                                            color = if (currentUser == name) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            text = name,
                                            fontWeight = if (currentUser == name) FontWeight.Bold else FontWeight.Normal,
                                            color = if (currentUser == name) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = dept,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Divider()
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showUserPicker = false }) {
                            Text("Cancelar")
                        }
                    }
                )
            }

            // Request Action/Details Drawer/Overlay
            selectedSolicitud?.let { sol ->
                SolicitudDetailsOverlay(
                    solicitud = sol,
                    role = role,
                    onClose = { viewModel.selectSolicitud(null) },
                    onAction = { status, note ->
                        viewModel.updateStatus(sol.id, status, note)
                    }
                )
            }
        }
    }
}

// FUNCIONARIO VIEW: Create form & View own requests
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuncionarioView(viewModel: SolicitudViewModel, solicitudes: List<Solicitud>) {
    var expandedForm by remember { mutableStateOf(false) }
    val formTipo by viewModel.formTipoItem.collectAsState()
    val formIdentificador by viewModel.formIdentificador.collectAsState()
    val formDescripcion by viewModel.formDescripcion.collectAsState()
    val formUrgencia by viewModel.formUrgencia.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // "Hacer una solicitud" collapsible drawer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedForm = !expandedForm },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Nueva Solicitud de Archivo",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (expandedForm) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Expandir Formulario"
                    )
                }

                AnimatedVisibility(
                    visible = expandedForm,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        // Document vs Folder selector
                        Text(
                            text = "Tipo de Elemento:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ElevatedCard(
                                onClick = { viewModel.formTipoItem.value = "Documento" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("type_document_button"),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (formTipo == "Documento") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Documento")
                                }
                            }

                            ElevatedCard(
                                onClick = { viewModel.formTipoItem.value = "Carpeta" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("type_folder_button"),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = if (formTipo == "Carpeta") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Carpeta completa")
                                }
                            }
                        }

                        // Identifier input field
                        OutlinedTextField(
                            value = formIdentificador,
                            onValueChange = { viewModel.formIdentificador.value = it },
                            label = { Text("Identificación o Nombre del Item") },
                            placeholder = { Text("Ej: Decreto N° 325, Carpeta Contratos Urbanos") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("identificador_input"),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null
                                )
                            }
                        )

                        // Description details
                        OutlinedTextField(
                            value = formDescripcion,
                            onValueChange = { viewModel.formDescripcion.value = it },
                            label = { Text("Motivo / Detalles de la Solicitud") },
                            placeholder = { Text("Ej: Para responder reclamo contraloría o revisión anual") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .testTag("descripcion_input"),
                            minLines = 2,
                            maxLines = 4
                        )

                        // Urgency level chips
                        Text(
                            text = "Nivel de Urgencia:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            listOf("Baja", "Media", "Alta").forEach { u ->
                                FilterChip(
                                    selected = formUrgencia == u,
                                    onClick = { viewModel.formUrgencia.value = u },
                                    label = { Text(u) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = when (u) {
                                            "Baja" -> Color(0xFFE8F5E9)
                                            "Media" -> Color(0xFFFFF3E0)
                                            else -> Color(0xFFFFEBEE)
                                        },
                                        selectedLabelColor = when (u) {
                                            "Baja" -> Color(0xFF2E7D32)
                                            "Media" -> Color(0xFFEF6C00)
                                            else -> Color(0xFFC62828)
                                        }
                                    )
                                )
                            }
                        }

                        // Submit Button
                        Button(
                            onClick = {
                                viewModel.createRequest {
                                    expandedForm = false
                                }
                            },
                            enabled = formIdentificador.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("submit_request_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Send, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar Pedido de Bodega")
                        }
                    }
                }
            }
        }

        // List of Requests Label and Counters
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Historial & Seguimiento de Mis Pedidos",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            SuggestionChip(
                onClick = { /* No-op refresh handled automatically by Room Flow */ },
                label = { Text("${solicitudes.size} solicitudes") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // History list representation
        if (solicitudes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(52.dp),
                        tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Aún no has solicitado documentos.",
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = "Usa el formulario superior para crear un pedido.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(solicitudes, key = { it.id }) { sol ->
                    SolicitudHistoryCard(solicitud = sol, onClick = {
                        viewModel.selectSolicitud(sol)
                    })
                }
            }
        }
    }
}

// ENCARGADO VIEW: Archive manager statistics + All requests with controls
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EncargadoView(
    viewModel: SolicitudViewModel,
    allSolicitudes: List<Solicitud>,
    filteredSolicitudes: List<Solicitud>
) {
    val statusFilter by viewModel.statusFilter.collectAsState()
    val urgenciaFilter by viewModel.urgenciaFilter.collectAsState()
    val tipoFilter by viewModel.tipoFilter.collectAsState()

    val pendingCount = allSolicitudes.count { it.estado == "PENDIENTE" }
    val approvedCount = allSolicitudes.count { it.estado == "APROBADO" }
    val dispatchedCount = allSolicitudes.count { it.estado == "DESPACHADO" }
    val deliveredCount = allSolicitudes.count { it.estado == "ENTREGADO" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Warehouse Stats Cards
        item {
            Column {
                Text(
                    text = "Panel de Operación y Control",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    maxItemsInEachRow = 2
                ) {
                    StatCard(
                        title = "Pendientes",
                        count = pendingCount,
                        color = Color(0xFFE6A23C),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Evaluados/Busca",
                        count = approvedCount,
                        color = Color(0xFF409EFF),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "En Ruta",
                        count = dispatchedCount,
                        color = Color(0xFF909399),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Entregados",
                        count = deliveredCount,
                        color = Color(0xFF34C759),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Filters UI
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Filtros de Búsqueda:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Status filter chips scrollable row
                    Text(
                        text = "Estado:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("TODOS", "PENDIENTE", "APROBADO", "DESPACHADO", "ENTREGADO", "DEVUELTO", "RECHAZADO").forEach { filter ->
                            FilterChip(
                                selected = statusFilter == filter,
                                onClick = { viewModel.statusFilter.value = filter },
                                label = { Text(filter) }
                            )
                        }
                    }

                    // Dual filter (Urgency & Type)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Urgency filter
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Urgencia:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("TODAS", "Alta", "Media").forEach { urg ->
                                    FilterChip(
                                        selected = urgenciaFilter == urg,
                                        onClick = { viewModel.urgenciaFilter.value = urg },
                                        label = { Text(urg) }
                                    )
                                }
                            }
                        }

                        // Type filter
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tipo:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("TODOS", "Documento", "Carpeta").forEach { tip ->
                                    FilterChip(
                                        selected = tipoFilter == tip,
                                        onClick = { viewModel.tipoFilter.value = tip },
                                        label = { Text(tip) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Header for results list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Solicitudes Recibidas (${filteredSolicitudes.size})",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                if (statusFilter != "TODOS" || urgenciaFilter != "TODAS" || tipoFilter != "TODOS") {
                    TextButton(onClick = {
                        viewModel.statusFilter.value = "TODOS"
                        viewModel.urgenciaFilter.value = "TODAS"
                        viewModel.tipoFilter.value = "TODOS"
                    }) {
                        Text("Limpiar Filtros", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Results
        if (filteredSolicitudes.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No se encontraron solicitudes.",
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        } else {
            items(filteredSolicitudes, key = { it.id }) { sol ->
                SolicitudManagerCard(solicitud = sol, onClick = {
                    viewModel.selectSolicitud(sol)
                })
            }
        }
    }
}

// Stats Card Composable matching HTML specs
@Composable
fun StatCard(title: String, count: Int, color: Color, modifier: Modifier = Modifier) {
    val isPending = title == "Pendientes"
    Card(
        modifier = modifier
            .height(100.dp)
            .testTag("stat_card_${title.lowercase()}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPending) Color(0xFFEADDFF) else Color(0xFFF3EDF7)
        ),
        border = if (isPending) null else BorderStroke(1.dp, Color(0xFFCAC4D0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.Start
        ) {
            Icon(
                imageVector = when (title) {
                    "Pendientes" -> Icons.Default.Info
                    "Evaluados/Busca" -> Icons.Default.Search
                    "En Ruta" -> Icons.Default.Send
                    else -> Icons.Default.Check
                },
                contentDescription = null,
                tint = if (isPending) Color(0xFF21005D) else Color(0xFF49454F),
                modifier = Modifier.size(20.dp)
            )
            
            Column {
                Text(
                    text = count.toString().padStart(2, '0'),
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPending) Color(0xFF21005D) else Color(0xFF1D1B20)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isPending) Color(0xFF21005D).copy(alpha = 0.8f) else Color(0xFF49454F)
                )
            }
        }
    }
}

data class BadgeColors(val container: Color, val content: Color)

fun getStatusBadgeColors(estado: String): BadgeColors {
    return when (estado) {
        "PENDIENTE" -> BadgeColors(Color(0xFFEADDFF), Color(0xFF21005D))
        "APROBADO" -> BadgeColors(Color(0xFFD3E3FD), Color(0xFF041E49))
        "DESPACHADO" -> BadgeColors(Color(0xFFFFD8E4), Color(0xFF31111D))
        "ENTREGADO" -> BadgeColors(Color(0xFFC4EED0), Color(0xFF072711))
        "DEVUELTO" -> BadgeColors(Color(0xFFC2E7FF), Color(0xFF001D35))
        "RECHAZADO" -> BadgeColors(Color(0xFFF2B8B5), Color(0xFF601410))
        else -> BadgeColors(Color(0xFFF3EDF7), Color(0xFF49454F))
    }
}

// Card details for FUNCIONARIOS
@Composable
fun SolicitudHistoryCard(solicitud: Solicitud, onClick: () -> Unit) {
    val badgeColors = getStatusBadgeColors(solicitud.estado)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("request_item_${solicitud.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle Profile / Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (solicitud.tipoItem == "Carpeta") Icons.Default.Home else Icons.Default.List,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = solicitud.identificadorItem,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Solicitud #${solicitud.id} • ${solicitud.tipoItem}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(9999.dp),
                    color = badgeColors.container
                ) {
                    Text(
                        text = solicitud.estado,
                        color = badgeColors.content,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatTimestamp(solicitud.fechaActualizacion),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// Card details for ARCHIVE MANAGERS (ENCARGADO)
@Composable
fun SolicitudManagerCard(solicitud: Solicitud, onClick: () -> Unit) {
    val badgeColors = getStatusBadgeColors(solicitud.estado)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("manager_card_${solicitud.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circle Profile with initials or icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            if (solicitud.urgencia == "Alta") Color(0xFFFFD8E4) else MaterialTheme.colorScheme.secondaryContainer,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (solicitud.tipoItem == "Carpeta") Icons.Default.Home else Icons.Default.List,
                        contentDescription = null,
                        tint = if (solicitud.urgencia == "Alta") Color(0xFF31111D) else MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = solicitud.identificadorItem,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Por: ${solicitud.funcionarioNombre} (${solicitud.departamento})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (solicitud.urgencia == "Alta") {
                        Surface(
                            shape = RoundedCornerShape(9999.dp),
                            color = Color(0xFFF2B8B5)
                        ) {
                            Text(
                                "ALTA",
                                color = Color(0xFF601410),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(9999.dp),
                        color = badgeColors.container
                    ) {
                        Text(
                            text = solicitud.estado,
                            color = badgeColors.content,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatTimestamp(solicitud.fechaSolicitud),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

// NOTIFICATION LIST (OVERLAY)
@Composable
fun NotificationsOverlay(
    notifications: List<Notificacion>,
    onClose: () -> Unit,
    onMarkRead: (id: Int) -> Unit,
    onMarkAllRead: () -> Unit,
    onClearAll: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClose() }
            .background(Color.Black.copy(alpha = 0.5f)),
        contentAlignment = Alignment.TopEnd
    ) {
        Card(
            modifier = Modifier
                .fillMaxHeight(0.85f)
                .fillMaxWidth(0.85f)
                .padding(top = 70.dp, end = 16.dp)
                .clickable(enabled = false) {}, // avoid clicks passing through
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header of notification center
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Centro de Avisos",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Divider()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onMarkAllRead, enabled = notifications.any { !it.leida }) {
                        Text("Leídas todo", fontSize = 12.sp)
                    }

                    TextButton(onClick = onClearAll, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Text("Limpiar todo", fontSize = 12.sp)
                    }
                }

                // Scroll list
                if (notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No tienes avisos.",
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(notifications, key = { it.id }) { notif ->
                            NotificationItemCard(notif = notif, onRead = { onMarkRead(notif.id) })
                        }
                    }
                }
            }
        }
    }
}

// Small Card helper inside center list
@Composable
fun NotificationItemCard(notif: Notificacion, onRead: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !notif.leida) { onRead() },
        colors = CardDefaults.cardColors(
            containerColor = if (notif.leida) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = notif.titulo,
                    fontWeight = if (notif.leida) FontWeight.Normal else FontWeight.Bold,
                    fontSize = 13.sp,
                    color = if (notif.leida) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )

                if (!notif.leida) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = notif.mensaje,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(notif.fecha),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.outline
                )

                if (!notif.leida) {
                    Text(
                        text = "Entendido ×",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// REQUEST DETAILS AND CORRECTION PANEL OVERLAY (DRAWER SHEET)
@Composable
fun SolicitudDetailsOverlay(
    solicitud: Solicitud,
    role: String,
    onClose: () -> Unit,
    onAction: (String, String) -> Unit
) {
    var observationNote by remember { mutableStateOf("") }
    var inActionProcess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onClose() }
            .background(Color.Black.copy(alpha = 0.62f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .clickable(enabled = false) {}, // avoid clicks dismiss
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Seguimiento #S-${solicitud.id}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Ingresado el ${formatTimestamp(solicitud.fechaSolicitud)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }

                    FilledIconButton(
                        onClick = onClose,
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Core item specifications
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DetailRow(label = "Elemento:", value = "${solicitud.tipoItem} [${solicitud.identificadorItem}]", isBold = true)
                        DetailRow(label = "Solicitante:", value = "${solicitud.funcionarioNombre} (${solicitud.departamento})")
                        DetailRow(label = "Urgencia:", value = solicitud.urgencia, highlightColor = when (solicitud.urgencia) {
                            "Alta" -> Color(0xFFC62828)
                            "Media" -> Color(0xFFEF6C00)
                            else -> Color(0xFF2E7D32)
                        })
                        Divider()
                        DetailRow(label = "Motivo detallado:", value = if (solicitud.descripcion.isBlank()) "Sin observaciones descriptivas." else solicitud.descripcion)

                        if (solicitud.notaEstado.isNotEmpty()) {
                            Divider()
                            Column {
                                Text(
                                    text = "Última Observación del Archivo:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = solicitud.notaEstado,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // METRIC STATE TRACKING TIMELINE
                Text(
                    text = "Línea de Tiempo del Pedido:",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(12.dp))

                val states = listOf("PENDIENTE", "APROBADO", "DESPACHADO", "ENTREGADO", "DEVUELTO")
                val activeIndex = states.indexOf(solicitud.estado)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    states.forEachIndexed { idx, stateTitle ->
                        val isFinished = idx <= activeIndex && solicitud.estado != "RECHAZADO"
                        val isCurrent = idx == activeIndex && solicitud.estado != "RECHAZADO"
                        val isRejected = solicitud.estado == "RECHAZADO" && idx == 1

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // Point circle representation
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(
                                        when {
                                            isRejected -> Color(0xFFFF3B30)
                                            isCurrent -> getStatusColor(stateTitle)
                                            isFinished -> getStatusColor(stateTitle).copy(alpha = 0.5f)
                                            else -> Color.LightGray
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isFinished) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                } else if (isRejected) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = if (isRejected) "RECHAZADO" else stateTitle,
                                    fontWeight = if (isCurrent || isRejected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = when {
                                        isRejected -> Color(0xFFFF3B30)
                                        isCurrent -> getStatusColor(stateTitle)
                                        isFinished -> MaterialTheme.colorScheme.onSurface
                                        else -> Color.Gray
                                    }
                                )
                                Text(
                                    text = when (stateTitle) {
                                        "PENDIENTE" -> "Se emitió con éxito en la plataforma."
                                        "APROBADO" -> if (isRejected) "Rechazado temporalmente para corrección." else "Búsqueda aprobada en anaqueles."
                                        "DESPACHADO" -> "Envío asignado al mensajero."
                                        "ENTREGADO" -> "Documentación firmada y en poder del funcionario."
                                        "DEVUELTO" -> "Exitosamente devuelto al estante original de bodega."
                                        else -> ""
                                    },
                                    fontSize = 11.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // OPERATION CONTROL FOR ENCARGADOS
                if (role == "ENCARGADO") {
                    Divider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Cambiar Estado de Solicitud (Encargado)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // TextField for notes / observations
                    OutlinedTextField(
                        value = observationNote,
                        onValueChange = { observationNote = it },
                        label = { Text("Instrucción o Nota de Estado") },
                        placeholder = { Text("Ej: Ubicado en estante B-4, o motivo de rechazo") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .testTag("observacion_estado_input"),
                        singleLine = true
                    )

                    // Navigation or action control triggers
                    if (solicitud.estado == "PENDIENTE") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAction("APROBADO", observationNote.ifBlank { "Revisado y en proceso de búsqueda." })
                                    onClose()
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("approve_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Aprobar")
                            }

                            Button(
                                onClick = {
                                    onAction("RECHAZADO", observationNote.ifBlank { "No se proporcionaron detalles válidos o identificación." })
                                    onClose()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("reject_btn")
                            ) {
                                Icon(imageVector = Icons.Default.Close, contentDescription = null)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Rechazar")
                            }
                        }
                    } else if (solicitud.estado == "APROBADO") {
                        Button(
                            onClick = {
                                onAction("DESPACHADO", observationNote.ifBlank { "Enviado con el mensajero de turno." })
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dispatch_btn")
                        ) {
                            Text("Despachar (Enviar)")
                        }
                    } else if (solicitud.estado == "DESPACHADO") {
                        Button(
                            onClick = {
                                onAction("ENTREGADO", observationNote.ifBlank { "Completado y entregado en mano del funcionario." })
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("deliver_btn")
                        ) {
                            Text("Confirmar Entrega")
                        }
                    } else if (solicitud.estado == "ENTREGADO") {
                        Button(
                            onClick = {
                                onAction("DEVUELTO", observationNote.ifBlank { "Devuelto conforme y reubicado en archivo." })
                                onClose()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("return_btn")
                        ) {
                            Text("Registrar Devolución a Bodega")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String, isBold: Boolean = false, highlightColor: Color? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold || highlightColor != null) FontWeight.Bold else FontWeight.Normal,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}
