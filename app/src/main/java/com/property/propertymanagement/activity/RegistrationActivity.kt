package com.property.propertymanagement.activity

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.property.propertymanagement.R
import com.property.propertymanagement.network.RegisterRequest
import com.property.propertymanagement.network.RetrofitClient
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegistrationActivity : AppCompatActivity() {
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var etName: TextInputEditText
    private lateinit var etHouseNumber: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnRegister: MaterialButton
    private lateinit var btnBackLogin: MaterialButton
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registration)

        apiService = RetrofitClient.createApiService(this)
        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        etName = findViewById(R.id.et_name)
        etHouseNumber = findViewById(R.id.et_house_number)
        etPhone = findViewById(R.id.et_phone)
        btnRegister = findViewById(R.id.btn_register)
        btnBackLogin = findViewById(R.id.btn_back_login)
    }

    private fun setupClickListeners() {
        btnRegister.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()
            val name = etName.text.toString().trim()
            val houseNumber = etHouseNumber.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (validateInput(username, password, confirmPassword, name, houseNumber)) {
                registerWithNetwork(username, password, name, houseNumber, phone.ifEmpty { null })
            }
        }

        btnBackLogin.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(
        username: String,
        password: String,
        confirmPassword: String,
        name: String,
        houseNumber: String
    ): Boolean {
        return when {
            username.isEmpty() -> {
                etUsername.error = "请输入用户名"
                etUsername.requestFocus()
                false
            }
            password.isEmpty() -> {
                etPassword.error = "请输入密码"
                etPassword.requestFocus()
                false
            }
            password.length < 6 -> {
                etPassword.error = "密码长度不能少于6位"
                etPassword.requestFocus()
                false
            }
            password != confirmPassword -> {
                etConfirmPassword.error = "两次输入的密码不一致"
                etConfirmPassword.requestFocus()
                false
            }
            name.isEmpty() -> {
                etName.error = "请输入姓名"
                etName.requestFocus()
                false
            }
            houseNumber.isEmpty() -> {
                etHouseNumber.error = "请输入房号"
                etHouseNumber.requestFocus()
                false
            }
            else -> true
        }
    }

    private fun registerWithNetwork(username: String, password: String, name: String, houseNumber: String, phone: String?) {
        btnRegister.isEnabled = false
        btnRegister.text = "提交中..."

        val registerRequest = RegisterRequest(username, password, name, houseNumber, phone)
        apiService.register(registerRequest).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                btnRegister.isEnabled = true
                btnRegister.text = "注册"

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.code == 200) {
                        Toast.makeText(this@RegistrationActivity, "注册请求已提交，请等待管理员审批", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        val errorMsg = result?.msg ?: "注册失败"
                        Toast.makeText(this@RegistrationActivity, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RegistrationActivity, "网络请求失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                btnRegister.isEnabled = true
                btnRegister.text = "注册"
                Toast.makeText(this@RegistrationActivity, "网络连接失败: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}