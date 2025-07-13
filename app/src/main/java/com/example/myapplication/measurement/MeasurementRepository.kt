package com.example.myapplication.measurement

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MeasurementRepository @Inject constructor(
    private val dao: MeasurementResultDao
) {
    suspend fun insert(result: MeasurementResultEntity) = dao.insert(result)
    fun getAll(): Flow<List<MeasurementResultEntity>> = dao.getAll()
    suspend fun deleteAll() = dao.deleteAll()
    suspend fun delete(result: MeasurementResultEntity) = dao.delete(result)
} 