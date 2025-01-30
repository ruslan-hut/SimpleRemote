package ua.com.programmer.simpleremote.firebase

import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import ua.com.programmer.simpleremote.entity.UserInfo
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DeleteOldUsersWorker(cont: Context, parameters: WorkerParameters) : Worker(cont, parameters) {

    override fun doWork(): Result {
        deleteOldUserRecords()
        return Result.success()
    }

    private fun deleteOldUserRecords() {
        Log.d("RC_DeleteOldUsersWorker", "Deleting old user records...")
        val firebase = FirebaseFirestore.getInstance()
        val usersCollection = firebase.collection("users")
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -30) // delete records older than 30 days
        val thresholdDate = calendar.time

        usersCollection.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val userInfo = document.toObject(UserInfo::class.java)
                    val loginDate = dateFormat.parse(userInfo.loginDate)

                    if (loginDate != null && loginDate.before(thresholdDate)) {
                        usersCollection.document(document.id).delete()
                            .addOnSuccessListener {
                                Log.d("RC_DeleteOldUsersWorker", "Deleted: ${document.id}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("RC_DeleteOldUsersWorker", "Error deleting: ${document.id}, ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("RC_DeleteOldUsersWorker", "Error getting documents: ${e.message}")
            }
    }
}