package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.JobStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    fun getUser(userId: String): Flow<UserEntity?>

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Query("SELECT COUNT(*) FROM users")
    fun getUserCount(): Flow<Int>
}

@Dao
interface ProviderConnectionDao {
    @Query("SELECT * FROM provider_connections WHERE userId = :userId")
    fun getConnectionsForUser(userId: String): Flow<List<ProviderConnectionEntity>>

    @Query("SELECT * FROM provider_connections WHERE userId = :userId AND provider = :provider LIMIT 1")
    suspend fun getConnection(userId: String, provider: String): ProviderConnectionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(connection: ProviderConnectionEntity)

    @Query("DELETE FROM provider_connections WHERE userId = :userId AND provider = :provider")
    suspend fun deleteConnection(userId: String, provider: String)
}

@Dao
interface TransferJobDao {
    @Query("SELECT * FROM transfer_jobs WHERE userId = :userId ORDER BY createdAt DESC")
    fun getJobsForUser(userId: String): Flow<List<TransferJobEntity>>

    @Query("SELECT * FROM transfer_jobs WHERE id = :jobId LIMIT 1")
    fun getJobById(jobId: String): Flow<TransferJobEntity?>

    @Query("SELECT * FROM transfer_jobs WHERE id = :jobId LIMIT 1")
    suspend fun getJobByIdOnce(jobId: String): TransferJobEntity?

    @Query("SELECT * FROM transfer_jobs WHERE status IN ('QUEUED', 'PREPARING', 'TRANSFERRING', 'VERIFYING') ORDER BY createdAt ASC")
    fun getActiveJobs(): Flow<List<TransferJobEntity>>

    @Query("SELECT * FROM transfer_jobs ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getRecentJobsOnce(limit: Int = 100): List<TransferJobEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: TransferJobEntity)

    @Update
    suspend fun updateJob(job: TransferJobEntity)

    @Query("UPDATE transfer_jobs SET status = :status WHERE id = :jobId")
    suspend fun updateJobStatus(jobId: String, status: JobStatus)

    @Query("DELETE FROM transfer_jobs WHERE id = :jobId")
    suspend fun deleteJob(jobId: String)

    @Query("SELECT COUNT(*) FROM transfer_jobs")
    fun getTotalJobsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transfer_jobs WHERE status = 'COMPLETED'")
    fun getCompletedJobsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transfer_jobs WHERE status = 'FAILED'")
    fun getFailedJobsCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM transfer_jobs WHERE status IN ('QUEUED', 'PREPARING', 'TRANSFERRING', 'VERIFYING')")
    fun getActiveJobsCount(): Flow<Int>
}

@Dao
interface TransferItemDao {
    @Query("SELECT * FROM transfer_items WHERE jobId = :jobId ORDER BY startedAt ASC")
    fun getItemsForJob(jobId: String): Flow<List<TransferItemEntity>>

    @Query("SELECT * FROM transfer_items WHERE jobId = :jobId")
    suspend fun getItemsForJobOnce(jobId: String): List<TransferItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<TransferItemEntity>)

    @Update
    suspend fun updateItem(item: TransferItemEntity)

    @Query("DELETE FROM transfer_items WHERE jobId = :jobId")
    suspend fun deleteItemsForJob(jobId: String)
}

@Dao
interface TransferEventDao {
    @Query("SELECT * FROM transfer_events WHERE jobId = :jobId ORDER BY timestamp DESC LIMIT 100")
    fun getEventsForJob(jobId: String): Flow<List<TransferEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: TransferEventEntity)
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE userId = :userId LIMIT 1")
    fun getSettings(userId: String): Flow<UserSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: UserSettingsEntity)
}

@Dao
interface AuditLogDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT 200")
    fun getRecentLogs(): Flow<List<AuditLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: AuditLogEntity)
}
