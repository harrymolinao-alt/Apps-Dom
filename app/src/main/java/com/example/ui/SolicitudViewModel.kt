package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Repository
import com.example.data.Solicitud
import com.example.data.Notificacion
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SolicitudViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: Repository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = Repository(database)
        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
        }
    }

    // Role state: "FUNCIONARIO" (civil servant) or "ENCARGADO" (archive manager)
    private val _userRole = MutableStateFlow("FUNCIONARIO")
    val userRole: StateFlow<String> = _userRole.asStateFlow()

    // Current civil servant state (to simulate different employees easily)
    private val _currentUser = MutableStateFlow("Rodrigo Muñoz")
    val currentUser: StateFlow<String> = _currentUser.asStateFlow()

    private val _currentDept = MutableStateFlow("Finanzas")
    val currentDept: StateFlow<String> = _currentDept.asStateFlow()

    // Form inputs
    val formTipoItem = MutableStateFlow("Documento") // "Documento" or "Carpeta"
    val formIdentificador = MutableStateFlow("")
    val formDescripcion = MutableStateFlow("")
    val formUrgencia = MutableStateFlow("Media") // "Baja", "Media", "Alta"

    // Filter states
    val statusFilter = MutableStateFlow("TODOS")
    val urgenciaFilter = MutableStateFlow("TODAS")
    val tipoFilter = MutableStateFlow("TODOS")

    // Selected solicitud for details/actions drawer
    private val _selectedSolicitud = MutableStateFlow<Solicitud?>(null)
    val selectedSolicitud: StateFlow<Solicitud?> = _selectedSolicitud.asStateFlow()

    // All solicitudes observed from Room
    val solicitudes: StateFlow<List<Solicitud>> = repository.allSolicitudes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Nested data helper to bypass Kotlin combine limits
    data class FiltrosBusqueda(
        val estado: String,
        val urgencia: String,
        val tipo: String
    )

    private val filtrosCombinados: Flow<FiltrosBusqueda> = combine(
        statusFilter,
        urgenciaFilter,
        tipoFilter
    ) { status, urgencia, tipo ->
        FiltrosBusqueda(status, urgencia, tipo)
    }

    // Filtered solicitudes based on role, filters, and current simulated user
    val filteredSolicitudes: StateFlow<List<Solicitud>> = combine(
        solicitudes,
        _userRole,
        _currentUser,
        filtrosCombinados
    ) { list, role, user, filtros ->
        var result = list
        
        // If in Funcionarios mode, only see requests made by CURRENT simulated user
        if (role == "FUNCIONARIO") {
            result = result.filter { it.funcionarioNombre.equals(user, ignoreCase = true) }
        }

        // Apply filters
        if (filtros.estado != "TODOS") {
            result = result.filter { it.estado == filtros.estado }
        }
        if (filtros.urgencia != "TODAS") {
            result = result.filter { it.urgencia == filtros.urgencia }
        }
        if (filtros.tipo != "TODOS") {
            result = result.filter { it.tipoItem == filtros.tipo }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications list
    val notifications: StateFlow<List<Notificacion>> = repository.allNotificaciones
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Unread notification counts
    val unreadNotificationsCount: StateFlow<Int> = repository.unreadNotificationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setRole(role: String) {
        _userRole.value = role
    }

    fun selectUser(name: String, dept: String) {
        _currentUser.value = name
        _currentDept.value = dept
    }

    fun createRequest(onSuccess: () -> Unit) {
        viewModelScope.launch {
            if (formIdentificador.value.isBlank()) return@launch

            val nueva = Solicitud(
                funcionarioNombre = _currentUser.value,
                departamento = _currentDept.value,
                tipoItem = formTipoItem.value,
                identificadorItem = formIdentificador.value,
                descripcion = formDescripcion.value,
                urgencia = formUrgencia.value,
                estado = "PENDIENTE",
                notaEstado = ""
            )
            repository.createSolicitud(nueva)
            
            // Reset dynamic fields
            formIdentificador.value = ""
            formDescripcion.value = ""
            formUrgencia.value = "Media"
            
            onSuccess()
        }
    }

    fun updateStatus(solicitudId: Int, nuevoEstado: String, nota: String) {
        viewModelScope.launch {
            repository.updateSolicitudStatus(solicitudId, nuevoEstado, nota)
            
            // Sync current details card if viewing
            _selectedSolicitud.value?.let { current ->
                if (current.id == solicitudId) {
                    _selectedSolicitud.value = current.copy(
                        estado = nuevoEstado,
                        notaEstado = nota,
                        fechaActualizacion = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    fun selectSolicitud(sol: Solicitud?) {
        _selectedSolicitud.value = sol
    }

    fun markNotificationRead(id: Int) {
        viewModelScope.launch {
            repository.markNotificationAsRead(id)
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch {
            repository.markAllNotificationsAsRead()
        }
    }

    fun clearAllNotifications() {
        viewModelScope.launch {
            repository.clearAllNotifications()
        }
    }
}
