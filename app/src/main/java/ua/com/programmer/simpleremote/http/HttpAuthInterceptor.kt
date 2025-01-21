package ua.com.programmer.simpleremote.http

import okhttp3.Interceptor
import okhttp3.Response
import kotlin.text.isNotBlank

class HttpAuthInterceptor: Interceptor {

    private var credentials = ""

    fun setCredentials(user: String, pass: String) {
        credentials = if (user.isNotBlank()) {
            okhttp3.Credentials.basic(user, pass, Charsets.UTF_8)
        } else{
            ""
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        if (credentials.isNotBlank()) {
            request = request.newBuilder().header(
                "Authorization", credentials
            ).build()
        }
        return chain.proceed(request)
    }
}