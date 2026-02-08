package com.healthguidelab.keto365.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_email")
data class UserEmailEntity(
    @PrimaryKey val id: Int = 1,
    val email: String,
    val createdAtMillis: Long = System.currentTimeMillis()
)
