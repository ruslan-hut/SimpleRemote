package ua.com.programmer.simpleremote.firebase

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ua.com.programmer.simpleremote.entity.UserInfo
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class DeleteOldUsersWorker(cont: Context, parameters: WorkerParameters) : CoroutineWorker(cont, parameters) {

    override suspend fun doWork(): Result {
        return try {
            deleteOldUserRecords()
            Result.success()
        } catch (e: Exception) {
            Log.e("RC_DeleteOldUsersWorker", "Error during cleanup: ${e.message}")
            Result.failure()
        }
    }

    private suspend fun deleteOldUserRecords() {
        Log.d("RC_DeleteOldUsersWorker", "Deleting old user records...")
        val firebase = FirebaseFirestore.getInstance()
        val usersCollection = firebase.collection("users")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val thresholdDate = LocalDateTime.now().minusDays(30)

        val documents = usersCollection.get().await()
        for (document in documents) {
            val userInfo = document.toObject(UserInfo::class.java)
            if (userInfo.loginDate.isEmpty()) continue
            val loginDate = try {
                LocalDateTime.parse(userInfo.loginDate, formatter)
            } catch (e: Exception) {
                Log.e("RC_DeleteOldUsersWorker", "Error parsing date: ${userInfo.loginDate}; ${e.message}")
                null
            }

            if (loginDate != null && loginDate.isBefore(thresholdDate)) {
                try {
                    usersCollection.document(document.id).delete().await()
                    Log.d("RC_DeleteOldUsersWorker", "Deleted: ${document.id}")
                } catch (e: Exception) {
                    Log.e("RC_DeleteOldUsersWorker", "Error deleting: ${document.id}, ${e.message}")
                }
            }
        }
    }
}
