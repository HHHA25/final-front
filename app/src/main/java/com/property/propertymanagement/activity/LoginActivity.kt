package com.property.propertymanagement.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.property.propertymanagement.R
import com.property.propertymanagement.network.LoginRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.property.propertymanagement.MainActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnRegister: MaterialButton
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        apiService = RetrofitClient.createApiService(this)
        initViews()
        setupClickListeners()

        // 检查是否已登录
        checkLoginStatus()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)
        btnRegister = findViewById(R.id.btn_register)
    }

    private fun setupClickListeners() {
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "用户名或密码不能为空", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loginWithNetwork(username, password)
        }

        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegistrationActivity::class.java))
        }
    }

    private fun loginWithNetwork(username: String, password: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "登录中..."

        val loginRequest = LoginRequest(username, password)
        apiService.login(loginRequest).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<String>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<String>>,
                response: Response<com.property.propertymanagement.network.ApiResult<String>>
            ) {
                btnLogin.isEnabled = true
                btnLogin.text = "登录"

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.code == 200 && result.data != null) {
                        val token = result.data
                        saveLoginInfo(username, token)
                        Toast.makeText(this@LoginActivity, "登录成功", Toast.LENGTH_SHORT).show()

                        // 获取用户信息并跳转
                        getUserInfoAndNavigate(username, token)
                    } else {
                        val errorMsg = result?.msg ?: "登录失败"
                        Toast.makeText(this@LoginActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "网络请求失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<String>>, t: Throwable) {
                btnLogin.isEnabled = true
                btnLogin.text = "登录"
                Toast.makeText(this@LoginActivity, "网络连接失败: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getUserInfoAndNavigate(username: String, token: String) {
        // 这里简化处理，实际应该调用获取用户信息的接口
        // 暂时使用默认值，实际项目中应该从后端获取完整的用户信息
        val role = if (username == "admin") "ADMIN" else "RESIDENT"
        val houseNumber = if (username == "admin") null else "A101" // 示例房号
        val userId = if (username == "admin") 1L else 2L
        val name = if (username == "admin") "管理员" else "居民用户"

        PermissionUtil.saveUser(this, username, role, houseNumber, userId, name)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun saveLoginInfo(username: String, token: String) {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("username", username)
            putString("token", token)
            putLong("login_time", System.currentTimeMillis()) // 保存登录时间
            putBoolean("is_logged_in", true)
            apply()
        }
        Log.d("LoginActivity", "保存登录信息，登录时间: ${System.currentTimeMillis()}")
    }

    private fun checkLoginStatus() {
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("is_logged_in", false)
        val token = sharedPref.getString("token", null)

        // 只有token存在且标记为已登录时才自动跳转
        if (isLoggedIn && !token.isNullOrEmpty()) {
            // 验证token是否有效（这里可以添加token有效性检查）
            val username = sharedPref.getString("username", null)
            if (!username.isNullOrEmpty()) {
                // 获取保存的用户信息并跳转
                val role = sharedPref.getString("role", "RESIDENT") ?: "RESIDENT"
                val houseNumber = sharedPref.getString("house_number", null)
                val userId = sharedPref.getLong("user_id", 0L)
                val name = sharedPref.getString("name", null)

                PermissionUtil.saveUser(this, username, role, houseNumber, userId, name ?: username)

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
        // 否则停留在登录页面
    }
}