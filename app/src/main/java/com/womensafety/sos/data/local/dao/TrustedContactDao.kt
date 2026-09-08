package com.womensafety.sos.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.womensafety.sos.data.entity.TrustedContact
import kotlinx.coroutines.flow.Flow

@Dao
interface TrustedContactDao {
    @Query("SELECT * FROM trusted_contacts ORDER BY priorityRank ASC")
    fun getAllContacts(): Flow<List<TrustedContact>>

    @Query("SELECT * FROM trusted_contacts ORDER BY priorityRank ASC")
    suspend fun getAllContactsList(): List<TrustedContact>

    @Query("SELECT * FROM trusted_contacts WHERE id = :id LIMIT 1")
    suspend fun getContactById(id: Long): TrustedContact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: TrustedContact): Long

    @Update
    suspend fun updateContact(contact: TrustedContact)

    @Delete
    suspend fun deleteContact(contact: TrustedContact)

    @Query("DELETE FROM trusted_contacts WHERE id = :id")
    suspend fun deleteById(id: Long)
}
