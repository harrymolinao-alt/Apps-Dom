package com.example.data

import kotlinx.coroutines.flow.Flow

class Repository(private val database: AppDatabase) {
    private val solicitudDao = database.solicitudDao()
    private val notificacionDao = database.notificacionDao()

    val allSolicitudes: Flow<List<Solicitud>> = solicitudDao.getAllSolicitudes()
    val allNotificaciones: Flow<List<Notificacion>> = notificacionDao.getAllNotificaciones()
    val unreadNotificationsCount: Flow<Int> = notificacionDao.getUnreadCount()

    fun getSolicitudesByFuncionario(username: String): Flow<List<Solicitud>> {
        return solicitudDao.getSolicitudesByFuncionario(username)
    }

    suspend fun createSolicitud(solicitud: Solicitud): Long {
        val id = solicitudDao.insertSolicitud(solicitud)
        // Add a notification about creation
        val notificacion = Notificacion(
            solicitudId = id.toInt(),
            titulo = "Nueva Solicitud Creada",
            mensaje = "Se ha registrado la solicitud #${id} de ${solicitud.tipoItem}: '${solicitud.identificadorItem}'"
        )
        notificacionDao.insertNotificacion(notificacion)
        return id
    }

    suspend fun updateSolicitudStatus(solicitudId: Int, nuevoEstado: String, nota: String = "") {
        val solicitud = solicitudDao.getSolicitudById(solicitudId) ?: return
        val anteriorEstado = solicitud.estado
        if (anteriorEstado == nuevoEstado && solicitud.notaEstado == nota) return

        val actualizada = solicitud.copy(
            estado = nuevoEstado,
            notaEstado = nota,
            fechaActualizacion = System.currentTimeMillis()
        )
        solicitudDao.updateSolicitud(actualizada)

        // Generate status-change notification
        val title = when (nuevoEstado) {
            "APROBADO" -> "Solicitud #${solicitudId} Aprobada"
            "RECHAZADO" -> "Solicitud #${solicitudId} Rechazada"
            "DESPACHADO" -> "Solicitud #${solicitudId} en Camino (Despachada)"
            "ENTREGADO" -> "Solicitud #${solicitudId} Entregada"
            "DEVUELTO" -> "Solicitud #${solicitudId} Devuelta y Archivada"
            else -> "Actualización de Solicitud #${solicitudId}"
        }

        val details = when (nuevoEstado) {
            "APROBADO" -> "El encargado de archivo ha aprobado su solicitud. Se iniciará la búsqueda en la bodega."
            "RECHAZADO" -> "La solicitud ha sido rechazada. Observación: '$nota'."
            "DESPACHADO" -> "El documento/carpeta ha salido de la bodega y va de camino."
            "ENTREGADO" -> "Se ha confirmado la entrega del documento a ${solicitud.funcionarioNombre}."
            "DEVUELTO" -> "El documento se regresó a la bodega y ha sido archivado correctamente en su ubicación original."
            else -> "La solicitud cambió de estado a $nuevoEstado."
        }

        val fNote = if (nota.isNotEmpty() && nuevoEstado != "RECHAZADO") " ($nota)" else ""

        notificacionDao.insertNotificacion(
            Notificacion(
                solicitudId = solicitudId,
                titulo = title,
                mensaje = "$details$fNote"
            )
        )
    }

    suspend fun markNotificationAsRead(id: Int) {
        notificacionDao.markAsRead(id)
    }

    suspend fun markAllNotificationsAsRead() {
        notificacionDao.markAllAsRead()
    }

    suspend fun clearAllNotifications() {
        notificacionDao.clearAll()
    }

    suspend fun seedSampleDataIfEmpty() {
        val count = solicitudDao.getSolicitudesCount()
        if (count > 0) return

        val ahora = System.currentTimeMillis()
        val unaHora = 3600000L
        val unDia = 86400000L

        val solicitudesIniciales = listOf(
            Solicitud(
                funcionarioNombre = "Rodrigo Muñoz",
                departamento = "Finanzas",
                tipoItem = "Carpeta",
                identificadorItem = "Carpeta Contratos Concesiones 2024",
                descripcion = "Se requiere de forma urgente para subsanar observaciones de la contraloría interna.",
                urgencia = "Alta",
                fechaSolicitud = ahora - 2 * unaHora,
                estado = "PENDIENTE",
                notaEstado = "",
                fechaActualizacion = ahora - 2 * unaHora
            ),
            Solicitud(
                funcionarioNombre = "Elena Salazar",
                departamento = "Recursos Humanos",
                tipoItem = "Documento",
                identificadorItem = "Expediente de Personal N° 402 - E. Gómez",
                descripcion = "Copia autorizada para tramitación de pensión de jubilación del funcionario.",
                urgencia = "Media",
                fechaSolicitud = ahora - unDia,
                estado = "APROBADO",
                notaEstado = "Búsqueda asignada a estante sector B-4.",
                fechaActualizacion = ahora - 4 * unaHora
            ),
            Solicitud(
                funcionarioNombre = "Patricio Aravena",
                departamento = "Jurídica",
                tipoItem = "Documento",
                identificadorItem = "Decreto Alcaldicio N° 1025 (Exento)",
                descripcion = "Se solicita para responder oficio judicial en causa Rol C-540-2025.",
                urgencia = "Alta",
                fechaSolicitud = ahora - 5 * unaHora,
                estado = "DESPACHADO",
                notaEstado = "Despachado en valija de documentos con mensajero.",
                fechaActualizacion = ahora - unaHora
            ),
            Solicitud(
                funcionarioNombre = "Claudio Soto",
                departamento = "Urbanismo",
                tipoItem = "Carpeta",
                identificadorItem = "Planos de Alcantarillado Villa Los Andes",
                descripcion = "Revisión técnica de empalme subterráneo solicitado por constructora.",
                urgencia = "Baja",
                fechaSolicitud = ahora - 3 * unDia,
                estado = "ENTREGADO",
                notaEstado = "Entregado conforme en oficina técnica.",
                fechaActualizacion = ahora - 2 * unDia
            ),
            Solicitud(
                funcionarioNombre = "Ana María Pérez",
                departamento = "Secretaría Municipal",
                tipoItem = "Documento",
                identificadorItem = "Acta de Concejo Municipal N° 45 (Mayo 2023)",
                descripcion = "Para verificación de acuerdo municipal de adjudicación de aseo urbano.",
                urgencia = "Media",
                fechaSolicitud = ahora - 6 * unDia,
                estado = "DEVUELTO",
                notaEstado = "Devuelto por la funcionaria y reintegrado al estante central de actas.",
                fechaActualizacion = ahora - 5 * unDia
            )
        )

        for (sol in solicitudesIniciales) {
            val id = solicitudDao.insertSolicitud(sol)
            // Generate notification for the seed data as if they were historically saved
            val notificationTitle = when (sol.estado) {
                "PENDIENTE" -> "Nueva Solicitud Creada"
                "APROBADO" -> "Solicitud #${id} Aprobada"
                "DESPACHADO" -> "Solicitud #${id} Despachada"
                "ENTREGADO" -> "Solicitud #${id} Entregada"
                "DEVUELTO" -> "Solicitud #${id} Devuelta y Archivada"
                else -> "Actualización de Solicitud #${id}"
            }
            val notificationMsg = when (sol.estado) {
                "PENDIENTE" -> "Se ha registrado la solicitud #${id} de ${sol.tipoItem}: '${sol.identificadorItem}'"
                "APROBADO" -> "La solicitud #${id} de ${sol.tipoItem} ha sido aprobada por el encargado."
                "DESPACHADO" -> "El documento/carpeta #${id} va de camino con el mensajero."
                "ENTREGADO" -> "Se ha confirmado la entrega del documento #${id} a ${sol.funcionarioNombre}."
                "DEVUELTO" -> "El documento #${id} ha sido regresado a la bodega y archivado en su estante."
                else -> "La solicitud #${id} cambió de estado."
            }
            notificacionDao.insertNotificacion(
                Notificacion(
                    solicitudId = id.toInt(),
                    titulo = notificationTitle,
                    mensaje = notificationMsg,
                    fecha = sol.fechaActualizacion,
                    leida = sol.estado == "DEVUELTO" || sol.estado == "ENTREGADO"
                )
            )
        }
    }
}
