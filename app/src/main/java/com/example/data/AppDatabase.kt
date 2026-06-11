package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom
import android.util.Base64
import com.example.utils.PrefsHelper

@Database(entities = [MemoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Get or generate a secure key for SQLCipher
                val prefs = PrefsHelper.getPrefs(context)
                var dbKeyString = prefs.getString("db_key", null)
                if (dbKeyString == null) {
                    val random = SecureRandom()
                    val key = ByteArray(32)
                    random.nextBytes(key)
                    dbKeyString = Base64.encodeToString(key, Base64.DEFAULT)
                    prefs.edit().putString("db_key", dbKeyString).apply()
                }
                val dbKey = Base64.decode(dbKeyString, Base64.DEFAULT)
                
                val factory = SupportFactory(dbKey)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "memory_os_secure_database"
                )
                .openHelperFactory(factory)
                .build()
                
                INSTANCE = instance
                instance
            }
        }
    }
}
