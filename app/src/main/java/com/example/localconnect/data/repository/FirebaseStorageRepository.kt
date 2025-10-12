package com.example.localconnect.data.repository

import com.example.localconnect.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirebaseStorageRepository : StorageRepository {
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override suspend fun uploadImage(
        imageBytes: ByteArray,
        path: String?,
        onProgress: ((Float) -> Unit)?
    ): Result<String> {
        return try {
            val uid = auth.currentUser?.uid ?: "anonymous"
            val finalPath = path ?: "posts/$uid/${System.currentTimeMillis()}-${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child(finalPath)
            val metadata = StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .build()

            // Upload with progress callback wrapped for coroutines
            val downloadUrl = suspendCancellableCoroutine<String> { cont ->
                val uploadTask = ref.putBytes(imageBytes, metadata)
                uploadTask.addOnProgressListener { snapshot ->
                    val prog = snapshot.bytesTransferred.toFloat() / (snapshot.totalByteCount.takeIf { it > 0 } ?: 1)
                    onProgress?.invoke(prog.coerceIn(0f, 1f))
                }.addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { uri ->
                        cont.resume(uri.toString())
                    }.addOnFailureListener { e ->
                        cont.resumeWithException(e)
                    }
                }.addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
                cont.invokeOnCancellation {
                    uploadTask.cancel()
                }
            }

            Result.success(downloadUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

