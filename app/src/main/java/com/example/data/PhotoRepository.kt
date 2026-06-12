package com.example.data

import kotlinx.coroutines.flow.Flow

class PhotoRepository(private val dao: CapturedPhotoDao) {
    val allPhotos: Flow<List<CapturedPhoto>> = dao.getAllPhotos()

    suspend fun getPhotoById(id: Int): CapturedPhoto? {
        return dao.getPhotoById(id)
    }

    suspend fun savePhoto(photo: CapturedPhoto): Long {
        return dao.insertPhoto(photo)
    }

    suspend fun updatePhoto(photo: CapturedPhoto) {
        dao.updatePhoto(photo)
    }

    suspend fun deletePhoto(photo: CapturedPhoto) {
        dao.deletePhoto(photo)
    }
}
