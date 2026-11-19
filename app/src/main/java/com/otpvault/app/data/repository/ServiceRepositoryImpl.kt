package com.otpvault.app.data.repository

import com.otpvault.app.data.local.DefaultServices
import com.otpvault.app.data.local.dao.ServiceDao
import com.otpvault.app.data.local.entity.ServiceEntity
import com.otpvault.app.domain.model.Service
import com.otpvault.app.domain.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ServiceRepositoryImpl(private val dao: ServiceDao) : ServiceRepository {

    override fun observeAll(): Flow<List<Service>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeCount(): Flow<Int> = dao.observeCount()

    override suspend fun seedDefaults() {
        val now = System.currentTimeMillis()
        dao.insertAll(DefaultServices.all.map { it.copy(createdAt = now) })
    }

    override suspend fun addCustom(name: String, category: String, icon: String): Service {
        val entity = ServiceEntity(
            id = "custom-" + UUID.randomUUID().toString(),
            name = name.trim(), category = category, icon = icon,
            isCustom = true, createdAt = System.currentTimeMillis()
        )
        dao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun updateCustom(service: Service) {
        val existing = dao.getById(service.id) ?: return
        require(existing.isCustom) { "Built-in services cannot be edited" }
        dao.update(existing.copy(name = service.name, category = service.category, icon = service.icon))
    }

    override suspend fun deleteCustom(id: String) {
        val count = dao.countOtpsForService(id)
        check(count == 0) { "Service has OTP records. Delete those first." }
        dao.deleteCustomById(id)
    }
}
