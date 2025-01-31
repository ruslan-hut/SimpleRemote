package ua.com.programmer.simpleremote.utility;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.firestore.FirebaseFirestore;
import ua.com.programmer.simpleremote.BuildConfig;
import ua.com.programmer.simpleremote.specialItems.DataBaseItem;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserDataUpdater {

    private String getCurrentDate() {
        Date currentDate = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return dateFormat.format(currentDate);
    }

    private void writeUserInfo(UserInfo userInfo) {
        userInfo.version = BuildConfig.VERSION_NAME;
        userInfo.loginDate = getCurrentDate();

        FirebaseFirestore firebase = FirebaseFirestore.getInstance();
        firebase.collection("users")
                .document(userInfo.guid)
                .set(userInfo);
    }

    public void updateUserData(DataBaseItem connection) {
        UserInfo userInfo = new UserInfo();
        userInfo.guid = connection.getString("guid");
        userInfo.version = BuildConfig.VERSION_NAME;
        userInfo.loginDate = getCurrentDate();
        userInfo.serverAddress = connection.getString("serverAddress");
        userInfo.databaseName = connection.getString("databaseName");
        userInfo.user = connection.getString("user");
        userInfo.password = connection.getString("password");
        userInfo.mode = connection.getString("mode");

        if (userInfo.guid.isEmpty()) return;

        FirebaseCrashlytics logger = FirebaseCrashlytics.getInstance();
        logger.setUserId(userInfo.guid);
        FirebaseAuth auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            auth.signInWithEmailAndPassword(BuildConfig.FIREBASE_EMAIL, BuildConfig.FIREBASE_PASSWORD)
                    .addOnSuccessListener(authResult -> writeUserInfo(userInfo))
                    .addOnFailureListener(logger::recordException);
        } else {
            writeUserInfo(userInfo);
        }
    }
}

