package com.baohao.esimkeeper.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface ESimCardDao {
    @Query("SELECT * FROM esim_cards ORDER BY expiryDate ASC, updatedAt DESC")
    fun observeCards(): Flow<List<ESimCard>>

    @Upsert
    suspend fun upsert(card: ESimCard)

    @Insert
    suspend fun insertAll(cards: List<ESimCard>)

    @Delete
    suspend fun delete(card: ESimCard)
}
