package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "solicitudes")
data class Solicitud(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val funcionarioNombre: String,
    val departamento: String,
    val tipoItem: String, // "Documento" or "Carpeta"
    val identificadorItem: String, // e.g., "Facturas 2025", "Folleto N° 12"
    val descripcion: String, // dynamic details
    val urgencia: String, // "Baja", "Media", "Alta"
    val fechaSolicitud: Long = System.currentTimeMillis(),
    val estado: String = "PENDIENTE", // "PENDIENTE", "APROBADO", "DESPACHADO", "ENTREGADO", "DEVUELTO", "RECHAZADO"
    val notaEstado: String = "", // Observation or reason from archive manager
    val fechaActualizacion: Long = System.currentTimeMillis()
)

@Entity(tableName = "notificaciones")
data class Notificacion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val solicitudId: Int,
    val titulo: String,
    val mensaje: String,
    val fecha: Long = System.currentTimeMillis(),
    val leida: Boolean = false
)
