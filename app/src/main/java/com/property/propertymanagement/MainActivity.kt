package com.property.propertymanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.activity.*
import com.property.propertymanagement.util.PermissionUtil
import com.property.propertymanagement.R
import com.property.propertymanagement.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var rvFunctions: RecyclerView
    private lateinit var apiService: com.property.propertymanagement.network.ApiService
    private var pendingCount = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        apiService = RetrofitClient.createApiService(this)
        initViews()
        loadFunctionCards()

        // 显示欢迎信息
        showWelcomeMessage()
    }

    override fun onResume() {
        super.onResume()
        if (PermissionUtil.isAdmin(this)) {
            loadPendingCount()
        }
    }

    private fun initViews() {
        rvFunctions = findViewById(R.id.rv_functions)
        rvFunctions.layoutManager = GridLayoutManager(this, 2)
    }

    private fun loadFunctionCards() {
        val functionItems = createFunctionItems()
        rvFunctions.adapter = FunctionAdapter(this, functionItems, pendingCount) { item ->
            startActivity(Intent(this, item.targetClass))
        }
    }

    private fun loadPendingCount() {
        apiService.getPendingCount().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Int>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Int>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Int>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    pendingCount = response.body()?.data ?: 0
                    loadFunctionCards()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Int>>, t: Throwable) {
                // 静默失败，不影响主功能
            }
        })
    }

    private fun createFunctionItems(): List<FunctionItem> {
        val items = mutableListOf<FunctionItem>()
        items.add(FunctionItem("楼栋管理", "管理小区楼栋和房屋信息", R.drawable.ic_building, BuildingManagementActivity::class.java))
        items.add(FunctionItem("管理费管理", "记录与管理小区管理费", R.mipmap.ic_launcher, FeeManagementActivity::class.java))
        items.add(FunctionItem("车位管理", "管理小区车位分配与使用", R.mipmap.ic_launcher, ParkingActivity::class.java))
        items.add(FunctionItem("维修管理", "处理业主维修申请与记录", R.mipmap.ic_launcher, RepairActivity::class.java))
        items.add(FunctionItem("投诉管理", "记录与跟进业主投诉", R.mipmap.ic_launcher, ComplaintActivity::class.java))

        if (PermissionUtil.isAdmin(this)) {
            items.add(FunctionItem("用户管理", "管理用户和注册审批", R.mipmap.ic_launcher, UserManagementActivity::class.java))
        }

        return items
    }

    // 添加菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_logout -> {
                showLogoutConfirmation()
                true
            }
            R.id.action_profile -> {
                showUserProfile()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showLogoutConfirmation() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("退出") { _, _ ->
                logout()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun logout() {
        // 清除所有保存的用户信息
        PermissionUtil.logout(this)

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

    private fun showUserProfile() {
        val username = PermissionUtil.getCurrentUsername(this)
        val role = PermissionUtil.getCurrentRole(this)
        val houseNumber = PermissionUtil.getCurrentHouseNumber(this)
        val name = PermissionUtil.getCurrentUserName(this)

        val roleText = if (role == "ADMIN") "管理员" else "居民"
        val houseText = if (houseNumber.isNullOrEmpty()) "无" else houseNumber

        val message = """
            用户名: $username
            姓名: ${name ?: "未设置"}
            角色: $roleText
            房号: $houseText
        """.trimIndent()

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("用户信息")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showWelcomeMessage() {
        val username = PermissionUtil.getCurrentUsername(this)
        val role = PermissionUtil.getCurrentRole(this)
        val roleText = if (role == "ADMIN") "管理员" else "居民"

        Toast.makeText(this, "欢迎回来，$username ($roleText)", Toast.LENGTH_SHORT).show()
    }

    // 功能项数据类
    data class FunctionItem(
        val title: String,
        val desc: String,
        val iconRes: Int,
        val targetClass: Class<*>
    )

    // 功能列表适配器
    class FunctionAdapter(
        private val context: Context,
        private val items: List<FunctionItem>,
        private val pendingCount: Int,
        private val onItemClick: (FunctionItem) -> Unit
    ) : RecyclerView.Adapter<FunctionAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val ivIcon: ImageView = itemView.findViewById(R.id.iv_icon)
            val tvTitle: TextView = itemView.findViewById(R.id.tv_title)
            val tvDesc: TextView = itemView.findViewById(R.id.tv_desc)
            val tvBadge: TextView = itemView.findViewById(R.id.tv_badge)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = android.view.LayoutInflater.from(parent.context)
                .inflate(R.layout.item_function_card, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            holder.ivIcon.setImageResource(item.iconRes)
            holder.tvTitle.text = item.title
            holder.tvDesc.text = item.desc

            // 显示待审批数量角标
            if (item.title == "用户管理" && pendingCount > 0) {
                holder.tvBadge.visibility = View.VISIBLE
                holder.tvBadge.text = if (pendingCount > 99) "99+" else pendingCount.toString()
            } else {
                holder.tvBadge.visibility = View.GONE
            }

            holder.itemView.setOnClickListener { onItemClick(item) }
        }

        override fun getItemCount() = items.size
    }
}