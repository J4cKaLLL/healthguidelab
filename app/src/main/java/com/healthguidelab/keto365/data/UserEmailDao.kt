package com.healthguidelab.keto365.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UserEmailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEmail(userEmailEntity: UserEmailEntity)

    @Query("SELECT * FROM user_email WHERE id = 1")
    suspend fun getEmail(): UserEmailEntity?
}
