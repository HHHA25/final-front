// ProfileFragment.kt
package com.property.propertymanagement.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.android.material.textfield.TextInputEditText
import android.content.Context
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import com.property.propertymanagement.R
import com.property.propertymanagement.activity.BuildingManagementActivity
import com.property.propertymanagement.activity.HouseManagementActivity
import com.property.propertymanagement.activity.LoginActivity
import com.property.propertymanagement.activity.UserManagementActivity
import com.property.propertymanagement.network.ApiResult
import com.property.propertymanagement.network.ApiService
import com.property.propertymanagement.network.ChangePasswordRequest
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {
    private lateinit var tvUsername: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvHouseNumber: TextView
    private lateinit var tvLoginTime: TextView
    private lateinit var tvTokenExpire: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var rvProfileItems: RecyclerView
    private lateinit var apiService: ApiService

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        apiService = RetrofitClient.createApiService(requireContext())
        initViews(view)
        loadUserInfo()
        setupClickListeners()
        setupProfileItems()
    }

    override fun onResume() {
        super.onResume()
        loadUserInfo()
    }

    private fun initViews(view: View) {
        tvUsername = view.findViewById(R.id.tv_username)
        tvRole = view.findViewById(R.id.tv_role)
        tvHouseNumber = view.findViewById(R.id.tv_house_number)
        tvLoginTime = view.findViewById(R.id.tv_login_time)
        tvTokenExpire = view.findViewById(R.id.tv_token_expire)
        btnLogout = view.findViewById(R.id.btn_logout)
        btnChangePassword = view.findViewById(R.id.btn_change_password)
        rvProfileItems = view.findViewById(R.id.rv_profile_items)
    }

    fun loadUserInfo() {
        val username = PermissionUtil.getCurrentUsername(requireContext()) ?: "未登录"
        val role = PermissionUtil.getCurrentRole(requireContext())
        val houseNumber = PermissionUtil.getCurrentHouseNumber(requireContext()) ?: "无"
        val name = PermissionUtil.getCurrentUserName(requireContext()) ?: username

        tvUsername.text = "用户名: $username"
        tvRole.text = "角色: ${if (role == "ADMIN") "管理员" else "居民"}"
        tvHouseNumber.text = "房号: $houseNumber"

        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        val loginTime = sharedPref.getLong("login_time", System.currentTimeMillis())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        tvLoginTime.text = "登录时间: ${dateFormat.format(Date(loginTime))}"

        val expireTime = loginTime + 5 * 24 * 60 * 60 * 1000
        tvTokenExpire.text = "Token过期: ${dateFormat.format(Date(expireTime))}"
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }
    }

    private fun setupProfileItems() {
        val items = mutableListOf<ProfileItem>()

        if (PermissionUtil.isAdmin(requireContext())) {
            items.add(ProfileItem("用户管理", "管理用户和权限", R.drawable.ic_user))
            items.add(ProfileItem("楼栋管理", "管理楼栋信息", R.drawable.ic_building))
            items.add(ProfileItem("房屋管理", "管理房屋信息", R.drawable.ic_house))
        }

        items.add(ProfileItem("关于我们", "关于物业管理系统", R.drawable.ic_about))

        rvProfileItems.layoutManager = LinearLayoutManager(requireContext())
        rvProfileItems.adapter = ProfileItemAdapter(items) { item ->
            onProfileItemClick(item)
        }
    }

    private fun onProfileItemClick(item: ProfileItem) {
        when (item.title) {
            "用户管理" -> {
                startActivity(Intent(requireContext(), UserManagementActivity::class.java))
            }
            "楼栋管理" -> {
                startActivity(Intent(requireContext(), BuildingManagementActivity::class.java))
            }
            "房屋管理" -> {
                startActivity(Intent(requireContext(), HouseManagementActivity::class.java))
            }
            "关于我们" -> {
                showAboutDialog()
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("退出") { _, _ ->
                logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 在 ProfileFragment 类中添加方法

    private fun showChangePasswordDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_change_password, null)

        val tilOldPassword = dialogView.findViewById<TextInputLayout>(R.id.til_old_password)
        val tilNewPassword = dialogView.findViewById<TextInputLayout>(R.id.til_new_password)
        val tilConfirmPassword = dialogView.findViewById<TextInputLayout>(R.id.til_confirm_password)
        val tvError = dialogView.findViewById<TextView>(R.id.tv_error)
        val etOldPassword = tilOldPassword.editText
        val etNewPassword = tilNewPassword.editText
        val etConfirmPassword = tilConfirmPassword.editText

        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btn_confirm)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btn_cancel)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("修改密码")
            .setView(dialogView)
            .create()

        btnConfirm.setOnClickListener {
            // 获取输入
            val oldPwd = etOldPassword?.text.toString().trim()
            val newPwd = etNewPassword?.text.toString().trim()
            val confirmPwd = etConfirmPassword?.text.toString().trim()

            // 清除所有错误
            tilOldPassword.error = null
            tilNewPassword.error = null
            tilConfirmPassword.error = null
            tvError.visibility = View.GONE
            tvError.text = ""

            var isValid = true

            // 验证旧密码
            if (oldPwd.isEmpty()) {
                tilOldPassword.error = "请输入旧密码"
                isValid = false
            }

            // 验证新密码
            if (newPwd.isEmpty()) {
                tilNewPassword.error = "请输入新密码"
                isValid = false
            } else if (newPwd.length < 6) {
                tilNewPassword.error = "新密码长度不能少于6位"
                isValid = false
            }

            // 验证确认密码
            if (confirmPwd.isEmpty()) {
                tilConfirmPassword.error = "请确认新密码"
                isValid = false
            } else if (confirmPwd != newPwd) {
                tilConfirmPassword.error = "两次输入的新密码不一致"
                isValid = false
            }

            if (isValid) {
                // 调用修改密码接口（带回调）
                changePassword(
                    oldPwd = oldPwd,
                    newPwd = newPwd,
                    onSuccess = {
                        dialog.dismiss()
                        Toast.makeText(requireContext(), "密码修改成功，请重新登录", Toast.LENGTH_LONG).show()
                        logoutAndGoToLogin()
                    },
                    onError = { errorMsg ->
                        // 根据错误信息显示在对应位置
                        if (errorMsg.contains("旧密码", ignoreCase = true)) {
                            tilOldPassword.error = errorMsg
                        } else {
                            tvError.text = errorMsg
                            tvError.visibility = View.VISIBLE
                        }
                    }
                )
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    /**
     * 修改密码网络请求（带回调）
     */
    private fun changePassword(
        oldPwd: String,
        newPwd: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val request = ChangePasswordRequest(oldPwd, newPwd)
        apiService.changePassword(request).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(call: Call<ApiResult<Void>>, response: Response<ApiResult<Void>>) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    onSuccess()
                } else {
                    val errorMsg = response.body()?.msg ?: "修改密码失败"
                    onError(errorMsg)
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                onError("网络错误: ${t.message}")
            }
        })
    }

    private fun validatePasswordInput(oldPwd: String, newPwd: String, confirmPwd: String): Boolean {
        return when {
            oldPwd.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入旧密码", Toast.LENGTH_SHORT).show()
                false
            }
            newPwd.isEmpty() -> {
                Toast.makeText(requireContext(), "请输入新密码", Toast.LENGTH_SHORT).show()
                false
            }
            newPwd.length < 6 -> {
                Toast.makeText(requireContext(), "新密码长度不能少于6位", Toast.LENGTH_SHORT).show()
                false
            }
            newPwd != confirmPwd -> {
                Toast.makeText(requireContext(), "两次输入的新密码不一致", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun changePassword(oldPwd: String, newPwd: String) {
        val request = ChangePasswordRequest(oldPwd, newPwd)


        apiService.changePassword(request).enqueue(object : Callback<ApiResult<Void>> {
            override fun onResponse(call: Call<ApiResult<Void>>, response: Response<ApiResult<Void>>) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(requireContext(), "密码修改成功，请重新登录", Toast.LENGTH_LONG).show()
                    // 清除登录状态并跳转到登录页
                    logoutAndGoToLogin()
                } else {
                    val errorMsg = response.body()?.msg ?: "修改密码失败"
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<ApiResult<Void>>, t: Throwable) {
                Toast.makeText(requireContext(), "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun logoutAndGoToLogin() {
        // 清除用户数据
        PermissionUtil.clearAllUserData(requireContext())
        val sharedPref = requireContext().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        // 跳转到登录页
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("关于物业管理系统")
            .setMessage("版本: 1.0.0\n\n" +
                    "功能说明:\n" +
                    "• 物业费用管理\n" +
                    "• 维修报修管理\n" +
                    "• 投诉建议管理\n" +
                    "• 车位管理\n" +
                    "• 用户管理\n\n" +
                    "技术支持: 物业管理系统开发团队")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun logout() {
        PermissionUtil.clearAllUserData(requireContext())

        val sharedPref = requireContext().getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()

        val intent = android.content.Intent(requireContext(), com.property.propertymanagement.activity.LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    data class ProfileItem(
        val title: String,
        val desc: String,
        val iconRes: Int
    )

    inner class ProfileItemAdapter(
        private val items: List<ProfileItem>,
        private val onItemClick: (ProfileItem) -> Unit
    ) : RecyclerView.Adapter<ProfileItemAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivIcon: android.widget.ImageView = itemView.findViewById(R.id.iv_icon)
            val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_profile, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivIcon.setImageResource(item.iconRes)
            holder.tvTitle.text = item.title
            holder.tvDesc.text = item.desc

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount() = items.size
    }
}