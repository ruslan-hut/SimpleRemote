package ua.com.programmer.simpleremote.dao.impl

import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ua.com.programmer.simpleremote.BuildConfig
import ua.com.programmer.simpleremote.dao.database.AppDatabase
import ua.com.programmer.simpleremote.dao.database.ConnectionSettingsDao
import ua.com.programmer.simpleremote.dao.entity.ConnectionSettings
import ua.com.programmer.simpleremote.dao.entity.isDemo
import ua.com.programmer.simpleremote.entity.UserInfo
import ua.com.programmer.simpleremote.repository.ConnectionSettingsRepository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionSettingsImpl @Inject constructor(
   private val connectionSettingsDao: ConnectionSettingsDao,
   private val database: AppDatabase,
) : ConnectionSettingsRepository {

    override val currentConnection = connectionSettingsDao.getCurrent()

    override fun getAll(): Flow<List<ConnectionSettings>> {
        return connectionSettingsDao.getAll()
    }

    override fun getByGuid(guid: String): Flow<ConnectionSettings> {
        return connectionSettingsDao.getByGuid(guid).map {
            it ?: ConnectionSettings(guid = guid)
        }
    }

    override suspend fun save(connection: ConnectionSettings): Long {
        val id = connectionSettingsDao.insert(connection)
        val current = connectionSettingsDao.checkCurrent()
        if (current == null) {
            connectionSettingsDao.setIsCurrent(connection.guid)
        }
        return id
    }

    override suspend fun delete(guid: String): Int {
        return connectionSettingsDao.delete(guid)
    }

    override suspend fun setCurrent(guid: String) {
        if (guid.isEmpty()) return
        database.withTransaction {
            connectionSettingsDao.resetIsCurrent()
            connectionSettingsDao.setIsCurrent(guid)
        }
    }

    override suspend fun checkAvailableConnection() {
        val current = connectionSettingsDao.checkCurrent()
        if (current == null) {
            val first = connectionSettingsDao.getFirst()
            if (first != null) {
                connectionSettingsDao.setIsCurrent(first.guid)
                return
            }
            val demo = ConnectionSettings.Builder.buildDemo()
            connectionSettingsDao.insert(demo)
            connectionSettingsDao.setIsCurrent(demo.guid)
        }
    }

    private fun getCurrentDate(): String {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
    }

    private fun writeUserInfo(connection: ConnectionSettings) {
        val userInfo = UserInfo.build(connection).copy(
            version = BuildConfig.VERSION_NAME,
            loginDate = getCurrentDate()
        )

        val firebase = FirebaseFirestore.getInstance()
        firebase.collection("users")
            .document(connection.guid)
            .set(userInfo)
    }

    override suspend fun updateUserData(connection: ConnectionSettings) {
        if (connection.isDemo()) return

        val logger = FirebaseCrashlytics.getInstance()
        logger.setUserId(connection.guid)
        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            auth.signInWithEmailAndPassword(BuildConfig.FIREBASE_EMAIL, BuildConfig.FIREBASE_PASSWORD)
                .addOnSuccessListener { writeUserInfo(connection) }
                .addOnFailureListener { e -> logger.recordException(e) }
        } else {
            writeUserInfo(connection)
        }
    }

}