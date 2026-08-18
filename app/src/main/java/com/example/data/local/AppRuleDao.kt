package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AppRule
import kotlinx.coroutines.flow.Flow

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM app_rules ORDER BY displayName ASC")
    fun getAllAppRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE enabled = 1")
    fun getEnabledAppRules(): Flow<List<AppRule>>

    @Query("SELECT * FROM app_rules WHERE enabled = 1")
    suspend fun getEnabledAppRulesSync(): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE id IN (:ids)")
    suspend fun getAppRulesByIds(ids: List<Long>): List<AppRule>

    @Query("SELECT * FROM app_rules WHERE id = :id LIMIT 1")
    suspend fun getAppRuleById(id: Long): AppRule?

    @Query("SELECT * FROM app_rules WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppRuleByPackageName(packageName: String): AppRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppRule(appRule: AppRule): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAppRules(appRules: List<AppRule>): List<Long>

    @Update
    suspend fun updateAppRule(appRule: AppRule)

    @Delete
    suspend fun deleteAppRule(appRule: AppRule)

    @Query("DELETE FROM app_rules WHERE id = :id")
    suspend fun deleteAppRuleById(id: Long)
}
