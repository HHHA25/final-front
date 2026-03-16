package com.property.propertymanagement.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.property.propertymanagement.R
import com.property.propertymanagement.network.ApiResult
import com.property.propertymanagement.network.ForgetPasswordRequest
import com.property.propertymanagement.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ForgetPasswordActivity : AppCompatActivity() {

    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilOldPassword: TextInputLayout
    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout

    private lateinit var etUsername: TextInputEditText
    private lateinit var etOldPassword: TextInputEditText
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText

    private lateinit var btnConfirm: MaterialButton
    private lateinit var apiService: com.property.propertymanagement.network.ApiService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forget_password)

        apiService = RetrofitClient.createApiService(this)

        initViews()
        setupListeners()
    }

    private fun initViews() {
        tilUsername = findViewById(R.id.til_username)
        tilOldPassword = findViewById(R.id.til_old_password)
        tilNewPassword = findViewById(R.id.til_new_password)
        tilConfirmPassword = findViewById(R.id.til_confirm_password)

        etUsername = findViewById(R.id.et_username)
        etOldPassword = findViewById(R.id.et_old_password)
        etNewPassword = findViewById(R.id.et_new_password)
        etConfirmPassword = findViewById(R.id.et_confirm_password)
        btnConfirm = findViewById(R.id.btn_confirm)

        // 如果是从登录页带用户名过来，自动填充且不可修改
        val username = intent.getStringExtra("username")
        if (!username.isNullOrEmpty()) {
            etUsername.setText(username)
            etUsername.isEnabled = false
        }
    }

    private fun setupListeners() {
        // 用户名输入监听
        etUsername.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilUsername.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 旧密码输入监听
        etOldPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilOldPassword.error = null
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 新密码输入监听
        etNewPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                tilNewPassword.error = null
                // 如果确认密码已输入，实时验证一致性
                if (!etConfirmPassword.text.isNullOrEmpty()) {
                    validateConfirmPassword()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 确认密码输入监听
        etConfirmPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                validateConfirmPassword()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnConfirm.setOnClickListener {
            attemptChangePassword()
        }
    }

    // 前端实时校验：两次新密码是否一致（显示红字）
    private fun validateConfirmPassword(): Boolean {
        val newPass = etNewPassword.text.toString()
        val confirmPass = etConfirmPassword.text.toString()
        return if (newPass != confirmPass) {
            tilConfirmPassword.error = "两次新密码不一致"
            false
        } else {
            tilConfirmPassword.error = null
            true
        }
    }

    private fun attemptChangePassword() {
        // 清除所有旧错误
        tilUsername.error = null
        tilOldPassword.error = null
        tilNewPassword.error = null
        tilConfirmPassword.error = null

        val username = etUsername.text.toString().trim()
        val oldPassword = etOldPassword.text.toString().trim()
        val newPassword = etNewPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // 基本非空校验（前端提示）
        if (username.isEmpty()) {
            tilUsername.error = "请输入用户名"
            etUsername.requestFocus()
            return
        }
        if (oldPassword.isEmpty()) {
            tilOldPassword.error = "请输入旧密码"
            etOldPassword.requestFocus()
            return
        }
        if (newPassword.isEmpty()) {
            tilNewPassword.error = "请输入新密码"
            etNewPassword.requestFocus()
            return
        }
        if (newPassword.length < 6) {
            tilNewPassword.error = "新密码长度不能少于6位"
            etNewPassword.requestFocus()
            return
        }
        if (!validateConfirmPassword()) {
            etConfirmPassword.requestFocus()
            return
        }

        // 调用接口
        btnConfirm.isEnabled = false
        btnConfirm.text = "提交中..."

        val request = ForgetPasswordRequest(username, oldPassword, newPassword)
        apiService.forgetPassword(request).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(call: Call<ApiResult<Void>>, response: Response<ApiResult<Void>>) {
                btnConfirm.isEnabled = true
                btnConfirm.text = "确认修改"

                if (response.isSuccessful) {
                    val result = response.body()
                    if (result?.code == 200) {
                        Toast.makeText(this@ForgetPasswordActivity, "密码修改成功，请重新登录", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        // 业务错误：根据错误信息匹配字段显示红字
                        val errorMsg = result?.msg ?: "修改失败"
                        handleBusinessError(errorMsg)
                    }
                } else {
                    // 网络请求失败（如404、500），用Toast提示
                    Toast.makeText(this@ForgetPasswordActivity, "网络请求失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                btnConfirm.isEnabled = true
                btnConfirm.text = "确认修改"
                Toast.makeText(this@ForgetPasswordActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 根据后端返回的错误信息，设置对应字段的红字提示
    private fun handleBusinessError(errorMsg: String) {
        when {
            errorMsg.contains("用户不存在") || errorMsg.contains("用户名错误") -> {
                tilUsername.error = errorMsg
                etUsername.requestFocus()
            }
            errorMsg.contains("旧密码错误") -> {
                tilOldPassword.error = errorMsg
                etOldPassword.requestFocus()
            }
            errorMsg.contains("两次密码不一致") -> {
                tilConfirmPassword.error = errorMsg
                etConfirmPassword.requestFocus()
            }
            else -> {
                // 其他未预期的业务错误，用Toast显示（也可以考虑统一显示在某个字段）
                Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
            }
        }
    }
}