package com.property.propertymanagement.util

import android.content.Context
import android.content.SharedPreferences

object PermissionUtil {
    private const val PREF_NAME = "user_prefs"
    private const val KEY_USERNAME = "username"
    private const val KEY_ROLE = "role"
    private const val KEY_HOUSE_NUMBER = "house_number"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NAME = "name"
    private const val KEY_TOKEN = "token"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_LOGIN_TIME = "login_time"

    // 保存登录用户信息
    fun saveUser(context: Context, username: String, role: String, houseNumber: String?, userId: Long, name: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_USERNAME, username)
            .putString(KEY_ROLE, role)
            .putString(KEY_HOUSE_NUMBER, houseNumber)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_NAME, name)
            .putLong(KEY_LOGIN_TIME, System.currentTimeMillis()) // 保存登录时间
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }
    // 检查Token是否过期（7天）
    fun isTokenExpired(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (loginTime == 0L) return true

        val currentTime = System.currentTimeMillis()
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L

        return (currentTime - loginTime) > sevenDaysInMillis
    }

    // 获取剩余时间（毫秒）
    fun getRemainingTime(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val loginTime = prefs.getLong(KEY_LOGIN_TIME, 0L)
        if (loginTime == 0L) return 0L

        val currentTime = System.currentTimeMillis()
        val sevenDaysInMillis = 7 * 24 * 60 * 60 * 1000L

        return (loginTime + sevenDaysInMillis) - currentTime
    }

    // 保存token
    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .apply()
    }

    // 获取当前登录用户角色
    fun getCurrentRole(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ROLE, "RESIDENT") ?: "RESIDENT"
    }

    // 判断是否为管理员
    fun isAdmin(context: Context): Boolean {
        return getCurrentRole(context) == "ADMIN"
    }

    // 获取当前用户房号（居民）
    fun getCurrentHouseNumber(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_HOUSE_NUMBER, null)
    }

    // 获取当前用户ID
    fun getCurrentUserId(context: Context): Long {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_USER_ID, 0)
    }

    // 获取当前用户姓名
    fun getCurrentUserName(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_NAME, null)
    }

    // 获取当前用户名
    fun getCurrentUsername(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_USERNAME, null)
    }

    // 获取token
    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_TOKEN, null)
    }

    // 检查是否已登录
    fun isLoggedIn(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getToken(context) != null
    }

    // 退出登录
    fun logout(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(KEY_TOKEN)
            .remove(KEY_IS_LOGGED_IN)
            .apply()
    }

    // 完全清除所有用户数据（用于彻底退出）
    fun clearAllUserData(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
    }
}