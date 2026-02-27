package ua.com.programmer.simpleremote.http.entity

data class EditLockResponse(
    val result: String = "",
    val message: String = "",
    val token: String = "",
    val data: List<LockResult?> = emptyList(),
)

data class LockResult(
    val locked: String = "",
    val error: String = "",
)

fun EditLockResponse.isSuccessful(): Boolean = data.firstOrNull()?.locked == "ok"

fun EditLockResponse.readError(): String = data.firstOrNull()?.error ?: ""
