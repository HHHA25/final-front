package com.property.propertymanagement.network

import android.content.Context
import android.widget.Toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class NetworkManager(private val context: Context) {
    private val apiService = RetrofitClient.createApiService(context)

    // 登录
    fun login(username: String, password: String, callback: (String?) -> Unit) {
        apiService.login(LoginRequest(username, password))
            .enqueue(object : Callback<ApiResult<String>> {
                override fun onResponse(
                    call: Call<ApiResult<String>>,
                    response: Response<ApiResult<String>>
                ) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        callback(response.body()?.data)
                    } else {
                        val errorMsg = response.body()?.msg ?: "登录失败"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        callback(null)
                    }
                }

                override fun onFailure(call: Call<ApiResult<String>>, t: Throwable) {
                    Toast.makeText(context, "网络连接失败: ${t.message}", Toast.LENGTH_SHORT).show()
                    callback(null)
                }
            })
    }

    // 注册 - 修复参数
    fun register(username: String, password: String, name: String, houseNumber: String, phone: String?, callback: (Boolean) -> Unit) {
        apiService.register(RegisterRequest(username, password, name, houseNumber, phone))
            .enqueue(object : Callback<ApiResult<Void>> {
                override fun onResponse(
                    call: Call<ApiResult<Void>>,
                    response: Response<ApiResult<Void>>
                ) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        Toast.makeText(context, "注册请求已提交，请等待管理员审批", Toast.LENGTH_LONG).show()
                        callback(true)
                    } else {
                        val errorMsg = response.body()?.msg ?: "注册失败"
                        Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                        callback(false)
                    }
                }

                override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                    Toast.makeText(context, "网络连接失败: ${t.message}", Toast.LENGTH_SHORT).show()
                    callback(false)
                }
            })
    }

    // 获取待审批数量
    fun getPendingCount(callback: (Int) -> Unit) {
        apiService.getPendingCount()
            .enqueue(object : Callback<ApiResult<Int>> {
                override fun onResponse(
                    call: Call<ApiResult<Int>>,
                    response: Response<ApiResult<Int>>
                ) {
                    if (response.isSuccessful && response.body()?.code == 200) {
                        callback(response.body()?.data ?: 0)
                    } else {
                        callback(0)
                    }
                }

                override fun onFailure(call: Call<ApiResult<Int>>, t: Throwable) {
                    callback(0)
                }
            })
    }

    // 可以继续添加其他网络请求方法...
}