package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SolicitudDao {
    @Query("SELECT COUNT(*) FROM solicitudes")
    suspend fun getSolicitudesCount(): Int

    @Query("SELECT * FROM solicitudes ORDER BY fechaActualizacion DESC")
    fun getAllSolicitudes(): Flow<List<Solicitud>>

    @Query("SELECT * FROM solicitudes WHERE id = :id")
    suspend fun getSolicitudById(id: Int): Solicitud?

    @Query("SELECT * FROM solicitudes WHERE funcionarioNombre = :funcionario ORDER BY fechaActualizacion DESC")
    fun getSolicitudesByFuncionario(funcionario: String): Flow<List<Solicitud>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSolicitud(solicitud: Solicitud): Long

    @Update
    suspend fun updateSolicitud(solicitud: Solicitud)

    @Delete
    suspend fun deleteSolicitud(solicitud: Solicitud)
}

@Dao
interface NotificacionDao {
    @Query("SELECT * FROM notificaciones ORDER BY fecha DESC")
    fun getAllNotificaciones(): Flow<List<Notificacion>>

    @Query("SELECT COUNT(*) FROM notificaciones WHERE leida = 0")
    fun getUnreadCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotificacion(notificacion: Notificacion): Long

    @Query("UPDATE notificaciones SET leida = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("UPDATE notificaciones SET leida = 1")
    suspend fun markAllAsRead()

    @Query("DELETE FROM notificaciones")
    suspend fun clearAll()
}
