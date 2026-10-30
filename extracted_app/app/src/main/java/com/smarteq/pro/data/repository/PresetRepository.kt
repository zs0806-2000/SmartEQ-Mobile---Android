package com.smarteq.pro.data.repository

import com.smarteq.pro.data.local.PresetDao
import com.smarteq.pro.data.local.toDomain
import com.smarteq.pro.data.local.toEntity
import com.smarteq.pro.data.model.BuiltInPresets
import com.smarteq.pro.data.model.EqPreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PresetRepository @Inject constructor(
    private val dao: PresetDao,
) {
    /** Built-in presets first (matching original DEFAULT_PRESETS ordering), then user presets. */
    fun observePresets(): Flow<List<EqPreset>> = dao.observeAll().map { saved ->
        BuiltInPresets.ALL + saved.map { it.toDomain() }
    }

    suspend fun save(preset: EqPreset) {
        require(!BuiltInPresets.ALL.any { it.id == preset.id }) { "Cannot overwrite a built-in preset" }
        dao.upsert(preset.toEntity())
    }

    suspend fun delete(preset: EqPreset) {
        if (preset.isBuiltIn) return // built-ins are not deletable, mirrors deletePreset() guard in App.tsx
        dao.deleteById(preset.id)
    }

    suspend fun linkToDevice(preset: EqPreset, deviceAddress: String?) {
        if (preset.isBuiltIn) return
        dao.upsert(preset.copy(linkedOutputDeviceAddress = deviceAddress).toEntity())
    }

    suspend fun findLinkedToDevice(deviceAddress: String): EqPreset? =
        dao.findLinkedToDevice(deviceAddress)?.toDomain()
}
