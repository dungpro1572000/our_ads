package com.dungz.our_ads.remotedata

import android.annotation.SuppressLint
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.dungz.our_ads.model.DataKey
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "remote_config_data")

@SuppressLint("StaticFieldLeak")
object RemoteConfigData {

    private lateinit var appContext: Context
    const val ENABLE_ADS = "enable_all_ads"
    private val enable_all_ads = DataKey(ENABLE_ADS, true)

    // Danh sách các key cần sync
    private val localSyncRemoteConfigListKey: List<DataKey<*>> = listOf(enable_all_ads)

    // Gọi 1 lần trong Application.onCreate()
    fun init(context: Context) {
        appContext = context.applicationContext
    }
    // Lấy giá trị từ DataStore, fallback về default
    suspend fun get(key: String): Any {
        val dataKey = localSyncRemoteConfigListKey.find { it.key == key }
        val prefs = appContext.dataStore.data.first()
        return try {
            when (dataKey?.value) {
                is String -> prefs[stringPreferencesKey(dataKey.key)] ?: dataKey.value
                is Boolean -> prefs[booleanPreferencesKey(dataKey.key)] ?: dataKey.value
                is Int -> prefs[intPreferencesKey(dataKey.key)] ?: dataKey.value
                is Long -> prefs[longPreferencesKey(dataKey.key)] ?: dataKey.value
                else -> Unit
            }
        } catch (e: ClassCastException) {
            dataKey?.value ?: Unit
        }
    }

    // Sync từ Remote Config -> DataStore
    // Key có trên Remote Config -> dùng giá trị remote
    // Key không có trên Remote Config -> dùng default value
    suspend fun syncData() {
        val remoteConfig = FirebaseRemoteConfig.getInstance()
        // Phải fetch + activate trước khi đọc, không thì remoteConfig.all trống
        remoteConfig.fetchAndActivate().await()
        val remoteKeys = remoteConfig.all.keys
        appContext.dataStore.edit { prefs ->
            localSyncRemoteConfigListKey.forEach { dataKey ->
                val hasRemoteValue = remoteKeys.contains(dataKey.key)
                when (dataKey.value) {
                    is String -> {
                        val value = if (hasRemoteValue) remoteConfig.getString(dataKey.key) else dataKey.value as String
                        prefs[stringPreferencesKey(dataKey.key)] = value
                    }
                    is Boolean -> {
                        val value = if (hasRemoteValue) remoteConfig.getBoolean(dataKey.key) else dataKey.value as Boolean
                        prefs[booleanPreferencesKey(dataKey.key)] = value
                    }
                    is Int -> {
                        val value = if (hasRemoteValue) remoteConfig.getLong(dataKey.key).toInt() else dataKey.value as Int
                        prefs[intPreferencesKey(dataKey.key)] = value
                    }
                    is Long -> {
                        val value = if (hasRemoteValue) remoteConfig.getLong(dataKey.key) else dataKey.value as Long
                        prefs[longPreferencesKey(dataKey.key)] = value
                    }

                    else -> {}
                }
            }
        }
    }
}
