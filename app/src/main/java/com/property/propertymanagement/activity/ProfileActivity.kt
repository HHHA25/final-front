package com.property.propertymanagement.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.util.PermissionUtil
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvRole: TextView
    private lateinit var tvHouseNumber: TextView
    private lateinit var tvLoginTime: TextView
    private lateinit var tvTokenExpire: TextView
    private lateinit var btnLogout: MaterialButton
    private lateinit var btnChangePassword: MaterialButton
    private lateinit var rvProfileItems: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        initViews()
        loadUserInfo()
        setupClickListeners()
        setupProfileItems()
    }

    private fun initViews() {
        tvUsername = findViewById(R.id.tv_username)
        tvRole = findViewById(R.id.tv_role)
        tvHouseNumber = findViewById(R.id.tv_house_number)
        tvLoginTime = findViewById(R.id.tv_login_time)
        tvTokenExpire = findViewById(R.id.tv_token_expire)
        btnLogout = findViewById(R.id.btn_logout)
        btnChangePassword = findViewById(R.id.btn_change_password)
        rvProfileItems = findViewById(R.id.rv_profile_items)

        // 设置返回按钮
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "个人中心"
    }

    private fun loadUserInfo() {
        val username = PermissionUtil.getCurrentUsername(this) ?: "未登录"
        val role = PermissionUtil.getCurrentRole(this)
        val houseNumber = PermissionUtil.getCurrentHouseNumber(this) ?: "无"
        val name = PermissionUtil.getCurrentUserName(this) ?: username

        tvUsername.text = "用户名: $username"
        tvRole.text = "角色: ${if (role == "ADMIN") "管理员" else "居民"}"
        tvHouseNumber.text = "房号: $houseNumber"

        // 获取登录时间（这里简化处理，实际应从SharedPreferences读取）
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val loginTime = sharedPref.getLong("login_time", System.currentTimeMillis())
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        tvLoginTime.text = "登录时间: ${dateFormat.format(Date(loginTime))}"

        // 显示Token过期时间（7天后）
        val expireTime = loginTime + 7 * 24 * 60 * 60 * 1000
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

        // 基本信息
        items.add(ProfileItem("个人信息", "查看和修改个人信息", R.drawable.ic_user))

        // 如果是管理员，添加管理功能
        if (PermissionUtil.isAdmin(this)) {
            items.add(ProfileItem("用户管理", "管理用户和权限", R.drawable.ic_user))
            items.add(ProfileItem("楼栋管理", "管理楼栋信息", R.drawable.ic_building))
            items.add(ProfileItem("房屋管理", "管理房屋信息", R.drawable.ic_house))
            items.add(ProfileItem("车位管理", "管理车位分配", R.drawable.ic_parking))
        } else {
            items.add(ProfileItem("我的物业费", "查看和缴纳物业费", R.drawable.ic_fee))
            items.add(ProfileItem("我的报修", "查看报修记录", R.drawable.ic_repair))
            items.add(ProfileItem("我的投诉", "查看投诉记录", R.drawable.ic_complaint))
        }

        // 系统设置
        items.add(ProfileItem("系统设置", "应用设置和关于", R.drawable.ic_settings))
        items.add(ProfileItem("关于我们", "关于物业管理系统", R.drawable.ic_about))

        rvProfileItems.layoutManager = LinearLayoutManager(this)
        rvProfileItems.adapter = ProfileItemAdapter(items) { item ->
            onProfileItemClick(item)
        }
    }

    private fun onProfileItemClick(item: ProfileItem) {
        when (item.title) {
            "个人信息" -> {
                // 跳转到个人信息编辑页面
                Toast.makeText(this, "打开个人信息", Toast.LENGTH_SHORT).show()
            }
            "用户管理" -> {
                startActivity(Intent(this, UserManagementActivity::class.java))
            }
            "楼栋管理" -> {
                startActivity(Intent(this, BuildingManagementActivity::class.java))
            }
            "房屋管理" -> {
                startActivity(Intent(this, HouseManagementActivity::class.java))
            }
            "车位管理" -> {
                startActivity(Intent(this, ParkingActivity::class.java))
            }
            "我的物业费" -> {
                startActivity(Intent(this, FeeManagementActivity::class.java))
            }
            "我的报修" -> {
                startActivity(Intent(this, RepairActivity::class.java))
            }
            "我的投诉" -> {
                startActivity(Intent(this, ComplaintActivity::class.java))
            }
            "系统设置" -> {
                // 跳转到系统设置
                Toast.makeText(this, "打开系统设置", Toast.LENGTH_SHORT).show()
            }
            "关于我们" -> {
                showAboutDialog()
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("退出") { _, _ ->
                logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showChangePasswordDialog() {
        // 这里实现修改密码对话框
        Toast.makeText(this, "修改密码功能开发中", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        AlertDialog.Builder(this)
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
        // 清除所有用户数据
        PermissionUtil.clearAllUserData(this)

        // 清除SharedPreferences
        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show()

        // 跳转到登录页面并清除返回栈
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    // ProfileItem数据类
    data class ProfileItem(
        val title: String,
        val desc: String,
        val iconRes: Int
    )

    // ProfileItem适配器
    inner class ProfileItemAdapter(
        private val items: List<ProfileItem>,
        private val onItemClick: (ProfileItem) -> Unit
    ) : RecyclerView.Adapter<ProfileItemAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardView: MaterialCardView = itemView.findViewById(R.id.card_profile_item)
            val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
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

            holder.cardView.setOnClickListener {
                onItemClick(item)
            }
        }

        override fun getItemCount() = items.size
    }
}