package com.example.nextthingb1.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.nextthingb1.data.local.dao.TaskDao
import com.example.nextthingb1.data.local.dao.LocationDao
import com.example.nextthingb1.data.local.entity.TaskEntity
import com.example.nextthingb1.data.local.entity.LocationEntity
import com.example.nextthingb1.data.local.converter.Converters

@Database(
    entities = [TaskEntity::class, LocationEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class TaskDatabase : RoomDatabase() {
    
    abstract fun taskDao(): TaskDao
    abstract fun locationDao(): LocationDao
    
    companion object {
        const val DATABASE_NAME = "next_thing_database"
        
        @Volatile
        private var INSTANCE: TaskDatabase? = null
        
        fun getDatabase(context: Context): TaskDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TaskDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
} 