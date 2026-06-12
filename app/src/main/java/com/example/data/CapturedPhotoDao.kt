package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CapturedPhotoDao {
    @Query("SELECT * FROM captured_photos ORDER BY timestamp DESC")
    fun getAllPhotos(): Flow<List<CapturedPhoto>>

    @Query("SELECT * FROM captured_photos WHERE id = :id LIMIT 1")
    suspend fun getPhotoById(id: Int): CapturedPhoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: CapturedPhoto): Long

    @Update
    suspend fun updatePhoto(photo: CapturedPhoto)

    @Delete
    suspend fun deletePhoto(photo: CapturedPhoto)
}
