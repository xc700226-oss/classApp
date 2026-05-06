package com.classapp.schedule.data.fetcher

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.nio.charset.Charset
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

sealed class FetchResult {
    data class Success(val html: String) : FetchResult()
    data class Error(val message: String, val cause: Exception? = null) : FetchResult()
}

class ScheduleFetcher {

    companion object {
        private const val LOGIN_PATH = "/Logon.do?method=logon"
        private const val SCHEDULE_PATH = "/xskb_list.do"
    }

    private val cookieStore = CookieJarImpl()

    private val client: OkHttpClient by lazy { buildClient() }

    private fun buildClient(): OkHttpClient {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, trustAllCerts, SecureRandom())
        }

        return OkHttpClient.Builder()
            .cookieJar(cookieStore)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true }
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun fetchSchedule(baseUrl: String, username: String, password: String): FetchResult {
        return try {
            val loginResult = login(baseUrl, username, password)
            if (loginResult is FetchResult.Error) return loginResult
            fetchSchedulePage(baseUrl)
        } catch (e: Exception) {
            FetchResult.Error("网络请求失败: ${e.localizedMessage}", e)
        }
    }

    private fun login(baseUrl: String, username: String, password: String): FetchResult {
        val loginUrl = "${baseUrl.trimEnd('/')}$LOGIN_PATH"

        val formBody = FormBody.Builder()
            .add("USERNAME", username)
            .add("PASSWORD", password)
            .build()

        val request = Request.Builder()
            .url(loginUrl)
            .post(formBody)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        val contentType = response.body?.contentType()
        val charset = contentType?.charset() ?: Charset.forName("GBK")
        val body = response.body?.source()?.use { it.readString(charset) } ?: ""

        if (body.contains("密码错误") || body.contains("用户名或密码") ||
            body.contains("验证码") || body.contains("账号不存在")) {
            return FetchResult.Error("登录失败：用户名或密码错误")
        }

        val hasSession = cookieStore.hasSessionCookie(baseUrl)
        if (!hasSession && body.length < 1000) {
            return FetchResult.Error("登录失败：未获取到会话，请检查网址和凭据")
        }

        return FetchResult.Success(body)
    }

    private fun fetchSchedulePage(baseUrl: String): FetchResult {
        val scheduleUrl = "${baseUrl.trimEnd('/')}$SCHEDULE_PATH"

        val request = Request.Builder()
            .url(scheduleUrl)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36")
            .build()

        val response = client.newCall(request).execute()
        val contentType = response.body?.contentType()
        val charset = contentType?.charset() ?: Charset.forName("GBK")
        val html = response.body?.source()?.use { it.readString(charset) } ?: ""

        if (html.length < 500) {
            return FetchResult.Error("课表页面内容异常（可能需要重新登录）")
        }

        if (!html.contains("kbtable") && !html.contains("kb_table") &&
            !html.contains("kbcontent")) {
            if (html.contains("Logon.do") || html.contains("login")) {
                return FetchResult.Error("会话已过期，请检查凭据后重试")
            }
            return FetchResult.Error("页面中未找到课表数据")
        }

        return FetchResult.Success(html)
    }

    fun clearSession() {
        cookieStore.clear()
    }

    private class CookieJarImpl : CookieJar {
        private val store = ConcurrentHashMap<String, List<Cookie>>()

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            store[url.host] = cookies
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return store[url.host] ?: emptyList()
        }

        fun hasSessionCookie(baseUrl: String): Boolean {
            val host = baseUrl.toHttpUrlOrNull()?.host ?: return false
            return store[host]?.any {
                it.name.equals("JSESSIONID", ignoreCase = true) ||
                    it.name.contains("session", ignoreCase = true)
            } ?: false
        }

        fun clear() {
            store.clear()
        }
    }
}
