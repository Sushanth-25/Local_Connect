package com.localconnect.domain.repository

interface StorageRepository {
    /**
     * Upload an image to Firebase Storage.
     * @param imageBytes The compressed image bytes to upload
     * @param path Optional destination path; if null, repository will generate a path
     * @param onProgress Optional progress callback in range [0f..1f]
     * @return Result with the download URL on success
     */
    suspend fun uploadImage(
        imageBytes: ByteArray,
        path: String? = null,
        onProgress: ((Float) -> Unit)? = null
    ): Result<String>
}
