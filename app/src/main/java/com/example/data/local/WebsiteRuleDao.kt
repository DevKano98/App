package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.WebsiteRule
import kotlinx.coroutines.flow.Flow

@Dao
interface WebsiteRuleDao {
    @Query("SELECT * FROM website_rules ORDER BY domain ASC")
    fun getAllWebsiteRules(): Flow<List<WebsiteRule>>

    @Query("SELECT * FROM website_rules WHERE enabled = 1")
    fun getEnabledWebsiteRules(): Flow<List<WebsiteRule>>

    @Query("SELECT * FROM website_rules WHERE enabled = 1")
    suspend fun getEnabledWebsiteRulesSync(): List<WebsiteRule>

    @Query("SELECT * FROM website_rules WHERE id IN (:ids)")
    suspend fun getWebsiteRulesByIds(ids: List<Long>): List<WebsiteRule>

    @Query("SELECT * FROM website_rules WHERE id = :id LIMIT 1")
    suspend fun getWebsiteRuleById(id: Long): WebsiteRule?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWebsiteRule(websiteRule: WebsiteRule): Long

    @Update
    suspend fun updateWebsiteRule(websiteRule: WebsiteRule)

    @Delete
    suspend fun deleteWebsiteRule(websiteRule: WebsiteRule)

    @Query("DELETE FROM website_rules WHERE id = :id")
    suspend fun deleteWebsiteRuleById(id: Long)
}
