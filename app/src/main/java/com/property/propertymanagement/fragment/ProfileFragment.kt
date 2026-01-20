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
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.property.propertymanagement.R
import com.property.propertymanagement.activity.BuildingManagementActivity
import com.property.propertymanagement.activity.HouseManagementActivity
import com.property.propertymanagement.activity.UserManagementActivity
import com.property.propertymanagement.util.PermissionUtil
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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

        if (PermissionUtil.isAdmin(requireContext())) {
            items.add(ProfileItem("用户管理", "管理用户和权限", R.drawable.ic_user))
            items.add(ProfileItem("楼栋管理", "管理楼栋信息", R.drawable.ic_building))
            items.add(ProfileItem("房屋管理", "管理房屋信息", R.drawable.ic_house))
        }

        items.add(ProfileItem("系统设置", "应用设置和关于", R.drawable.ic_settings))
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
            "系统设置" -> {
                Toast.makeText(requireContext(), "打开系统设置", Toast.LENGTH_SHORT).show()
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

    private fun showChangePasswordDialog() {
        Toast.makeText(requireContext(), "修改密码功能开发中", Toast.LENGTH_SHORT).show()
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