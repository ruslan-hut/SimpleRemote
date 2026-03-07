package ua.com.programmer.simpleremote.firebase

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import ua.com.programmer.simpleremote.entity.UserInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

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
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30) // delete records older than 30 days
        val thresholdDate = calendar.time

        val documents = usersCollection.get().await()
        for (document in documents) {
            val userInfo = document.toObject(UserInfo::class.java)
            if (userInfo.loginDate.isEmpty()) continue
            val loginDate = try {
                dateFormat.parse(userInfo.loginDate)
            } catch (e: Exception) {
                Log.e("RC_DeleteOldUsersWorker", "Error parsing date: ${userInfo.loginDate}; ${e.message}")
                null
            }

            if (loginDate != null && loginDate.before(thresholdDate)) {
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
