package com.property.propertymanagement.network

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import com.property.propertymanagement.activity.LoginActivity
import com.property.propertymanagement.util.PermissionUtil
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "http://192.168.1.148:8080/" // 真机访问
    // private const val BASE_URL = "http://10.0.2.2:8080/" // 模拟器访问本地

    private const val TAG = "RetrofitClient"

    fun createApiService(context: Context): ApiService {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 添加 Token 和错误处理拦截器
        val authInterceptor = Interceptor { chain ->
            val originalRequest = chain.request()

            // 从 SharedPreferences 获取 Token
            val token = getToken(context)
            Log.d(TAG, "发送请求，当前 Token: ${token?.take(10)}...")

            val requestBuilder = originalRequest.newBuilder()
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")

            if (token != null) {
                requestBuilder.header("Authorization", token)
            }

            val request = requestBuilder.build()
            val response = chain.proceed(request)

            // 检查响应头中是否有新的 Token
            val newToken = response.header("X-Renew-Token")
            if (!newToken.isNullOrEmpty()) {
                Log.d(TAG, "收到新的 Token，自动更新: ${newToken.take(10)}...")
                saveToken(context, newToken)
            }

            // 检查响应状态码
            when (response.code) {
                401 -> {
                    // Token过期，清除登录状态并跳转到登录页面
                    Log.w(TAG, "Token 过期，跳转登录页面")
                    clearLoginStatus(context)
                    context.startActivity(
                        Intent(
                            context,
                            LoginActivity::class.java
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            putExtra("expired", true)
                        })
                    Toast.makeText(context, "登录已过期，请重新登录", Toast.LENGTH_SHORT).show()
                }
                403 -> {
                    Log.w(TAG, "无权限访问")
                    Toast.makeText(context, "无权限访问", Toast.LENGTH_SHORT).show()
                }
            }

            response
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .addInterceptor(authInterceptor)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }

    private fun getToken(context: Context): String? {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        return sharedPref.getString("token", null)
    }

    private fun saveToken(context: Context, token: String) {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("token", token)
            apply()
        }
    }

    private fun clearLoginStatus(context: Context) {
        val sharedPref = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("token")
            putBoolean("is_logged_in", false)
            apply()
        }
        PermissionUtil.clearAllUserData(context)
    }
}