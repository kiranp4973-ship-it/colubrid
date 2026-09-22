package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.data.model.ConflictStrategy
import com.example.data.model.ConnectionStatus
import com.example.data.model.ItemStatus
import com.example.data.model.JobStatus
import com.example.data.model.LogLevel
import com.example.data.model.ProviderType
import com.example.data.model.UserRole
import com.example.data.model.VerificationStatus

class CloudBridgeConverters {
    @TypeConverter
    fun fromProviderType(value: ProviderType): String = value.name
    @TypeConverter
    fun toProviderType(value: String): ProviderType = runCatching { ProviderType.valueOf(value) }.getOrDefault(ProviderType.GOOGLE_DRIVE)

    @TypeConverter
    fun fromConnectionStatus(value: ConnectionStatus): String = value.name
    @TypeConverter
    fun toConnectionStatus(value: String): ConnectionStatus = runCatching { ConnectionStatus.valueOf(value) }.getOrDefault(ConnectionStatus.NOT_CONNECTED)

    @TypeConverter
    fun fromJobStatus(value: JobStatus): String = value.name
    @TypeConverter
    fun toJobStatus(value: String): JobStatus = runCatching { JobStatus.valueOf(value) }.getOrDefault(JobStatus.QUEUED)

    @TypeConverter
    fun fromItemStatus(value: ItemStatus): String = value.name
    @TypeConverter
    fun toItemStatus(value: String): ItemStatus = runCatching { ItemStatus.valueOf(value) }.getOrDefault(ItemStatus.QUEUED)

    @TypeConverter
    fun fromVerificationStatus(value: VerificationStatus): String = value.name
    @TypeConverter
    fun toVerificationStatus(value: String): VerificationStatus = runCatching { VerificationStatus.valueOf(value) }.getOrDefault(VerificationStatus.PENDING)

    @TypeConverter
    fun fromConflictStrategy(value: ConflictStrategy): String = value.name
    @TypeConverter
    fun toConflictStrategy(value: String): ConflictStrategy = runCatching { ConflictStrategy.valueOf(value) }.getOrDefault(ConflictStrategy.SKIP_DUPLICATE)

    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name
    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.USER)

    @TypeConverter
    fun fromLogLevel(value: LogLevel): String = value.name
    @TypeConverter
    fun toLogLevel(value: String): LogLevel = runCatching { LogLevel.valueOf(value) }.getOrDefault(LogLevel.INFO)
}

@Database(
    entities = [
        UserEntity::class,
        ProviderConnectionEntity::class,
        TransferJobEntity::class,
        TransferItemEntity::class,
        TransferEventEntity::class,
        UserSettingsEntity::class,
        AuditLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(CloudBridgeConverters::class)
abstract class CloudBridgeDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun providerConnectionDao(): ProviderConnectionDao
    abstract fun transferJobDao(): TransferJobDao
    abstract fun transferItemDao(): TransferItemDao
    abstract fun transferEventDao(): TransferEventDao
    abstract fun userSettingsDao(): UserSettingsDao
    abstract fun auditLogDao(): AuditLogDao

    companion object {
        @Volatile
        private var INSTANCE: CloudBridgeDatabase? = null

        fun getDatabase(context: Context): CloudBridgeDatabase {
            return INSTANCE ?: synchronized(this) {
                val targetContext = runCatching { context.applicationContext }.getOrNull() ?: context
                val instance = Room.databaseBuilder(
                    targetContext,
                    CloudBridgeDatabase::class.java,
                    "cloudbridge_database"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        fun getInMemoryDatabase(context: Context): CloudBridgeDatabase {
            val targetContext = runCatching { context.applicationContext }.getOrNull() ?: context
            return Room.inMemoryDatabaseBuilder(
                targetContext,
                CloudBridgeDatabase::class.java
            )
                .allowMainThreadQueries()
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
        }
    }
}
