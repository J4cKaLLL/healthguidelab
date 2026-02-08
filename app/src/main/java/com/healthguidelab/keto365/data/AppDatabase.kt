package com.healthguidelab.keto365.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [UserEmailEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userEmailDao(): UserEmailDao
}
