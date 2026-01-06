package com.property.propertymanagement.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.property.propertymanagement.R
import com.property.propertymanagement.network.RetrofitClient
import com.property.propertymanagement.util.PermissionUtil
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class UserManagementActivity : AppCompatActivity() {
    private lateinit var rvUsers: RecyclerView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var apiService: com.property.propertymanagement.network.ApiService
    private var userList = mutableListOf<com.property.propertymanagement.network.UserResponse>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_management)

        // 非管理员禁止进入
        if (!PermissionUtil.isAdmin(this)) {
            Toast.makeText(this, "无权限访问", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        apiService = RetrofitClient.createApiService(this)

        rvUsers = findViewById(R.id.rv_users)
        fabAdd = findViewById(R.id.fab_add)

        initRecyclerView()
        loadUserData()

        fabAdd.setOnClickListener {
            showAddUserOptionsDialog()
        }
    }

    private fun initRecyclerView() {
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = UserAdapter(userList,
            onItemLongClick = { user ->
                // 不能删除管理员
                if (user.role == "ADMIN") {
                    Toast.makeText(this, "不能删除管理员", Toast.LENGTH_SHORT).show()
                    return@UserAdapter false
                }
                showDeleteUserDialog(user.id)
                true
            }
        )
    }

    private fun loadUserData() {
        apiService.getAllUsers().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.UserResponse>>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.UserResponse>>>,
                response: Response<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.UserResponse>>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    val data = response.body()?.data ?: emptyList()
                    userList.clear()
                    userList.addAll(data)
                    rvUsers.adapter?.notifyDataSetChanged()
                    updateEmptyView()
                } else {
                    Toast.makeText(this@UserManagementActivity, "加载用户数据失败", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<List<com.property.propertymanagement.network.UserResponse>>>, t: Throwable) {
                Toast.makeText(this@UserManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showDeleteUserDialog(userId: Long) {
        AlertDialog.Builder(this)
            .setTitle("删除用户")
            .setMessage("确定要删除该用户吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteUser(userId)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteUser(userId: Long) {
        apiService.deleteUser(userId).enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Void>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Void>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Void>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    Toast.makeText(this@UserManagementActivity, "删除成功", Toast.LENGTH_SHORT).show()
                    loadUserData() // 重新加载数据
                } else {
                    val errorMsg = response.body()?.msg ?: "删除失败"
                    Toast.makeText(this@UserManagementActivity, errorMsg, Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Void>>, t: Throwable) {
                Toast.makeText(this@UserManagementActivity, "网络错误: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    inner class UserAdapter(
        private val users: List<com.property.propertymanagement.network.UserResponse>,
        private val onItemLongClick: (com.property.propertymanagement.network.UserResponse) -> Boolean
    ) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {
        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tvUsername = itemView.findViewById<TextView>(R.id.tv_username)
            val tvRole = itemView.findViewById<TextView>(R.id.tv_role)
            val tvHouseNumber = itemView.findViewById<TextView>(R.id.tv_house_number)
            val tvPhone = itemView.findViewById<TextView>(R.id.tv_phone)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = layoutInflater.inflate(R.layout.item_user, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val user = users[position]
            holder.tvUsername.text = "用户名：${user.username}"
            holder.tvRole.text = "角色：${if (user.role == "ADMIN") "管理员" else "居民"}"
            holder.tvHouseNumber.text = "房号：${user.houseNumber ?: "无"}"
            holder.tvPhone.text = "电话：${user.phone ?: "无"}"
            holder.itemView.setOnLongClickListener { onItemLongClick(user) }
        }

        override fun getItemCount() = users.size
    }

    // 加载菜单
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.user_management_menu, menu)
        return true
    }

    // 处理菜单点击事件
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_approval -> {
                startActivity(Intent(this, RegistrationApprovalActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateEmptyView() {
        findViewById<TextView>(R.id.tv_empty).visibility =
            if (userList.isEmpty()) View.VISIBLE else View.GONE
    }
    private fun showAddUserOptionsDialog() {
        val options = arrayOf("直接添加用户", "审批注册请求")

        AlertDialog.Builder(this)
            .setTitle("添加用户")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddUserDialog()
                    1 -> startActivity(Intent(this, RegistrationApprovalActivity::class.java))
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddUserDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_user, null)
        val etUsername = dialogView.findViewById<TextInputEditText>(R.id.et_username)
        val etPassword = dialogView.findViewById<TextInputEditText>(R.id.et_password)
        val etName = dialogView.findViewById<TextInputEditText>(R.id.et_name)
        val etHouseNumber = dialogView.findViewById<TextInputEditText>(R.id.et_house_number)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.et_phone)
        val etRole = dialogView.findViewById<TextInputEditText>(R.id.et_role)

        AlertDialog.Builder(this)
            .setTitle("直接添加用户")
            .setView(dialogView)
            .setPositiveButton("添加") { _, _ ->
                val username = etUsername.text.toString().trim()
                val password = etPassword.text.toString().trim()
                val name = etName.text.toString().trim()
                val houseNumber = etHouseNumber.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                val role = etRole.text.toString().trim()

                if (validateUserInput(username, password, name, houseNumber, role)) {
                    addUserDirectly(username, password, name, houseNumber, phone, role)
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun validateUserInput(
        username: String,
        password: String,
        name: String,
        houseNumber: String,
        role: String
    ): Boolean {
        return when {
            username.isEmpty() -> {
                Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show()
                false
            }
            password.isEmpty() -> {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                false
            }
            name.isEmpty() -> {
                Toast.makeText(this, "请输入姓名", Toast.LENGTH_SHORT).show()
                false
            }
            houseNumber.isEmpty() && role != "ADMIN" -> {
                Toast.makeText(this, "居民必须填写房号", Toast.LENGTH_SHORT).show()
                false
            }
            role.isEmpty() -> {
                Toast.makeText(this, "请输入角色(ADMIN/RESIDENT)", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun addUserDirectly(username: String, password: String, name: String, houseNumber: String, phone: String?, role: String) {
        // 注意：后端需要提供直接添加用户的接口
        // 这里只是示例，实际需要调用后端的添加用户接口
        Toast.makeText(this, "直接添加用户功能需要后端接口支持", Toast.LENGTH_SHORT).show()
    }
}