package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "website_rules")
data class WebsiteRule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val domain: String,
    val includeSubdomains: Boolean = true,
    val enabled: Boolean = true
)
