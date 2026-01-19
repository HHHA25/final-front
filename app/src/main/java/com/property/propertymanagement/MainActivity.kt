package com.property.propertymanagement

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.property.propertymanagement.util.PermissionUtil
import com.property.propertymanagement.R
import com.property.propertymanagement.fragment.HomeFragment
import com.property.propertymanagement.network.RetrofitClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.property.propertymanagement.activity.ComplaintActivity
import com.property.propertymanagement.activity.FeeManagementActivity
import com.property.propertymanagement.activity.LoginActivity
import com.property.propertymanagement.activity.ProfileActivity
import com.property.propertymanagement.activity.RepairActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView
    private lateinit var apiService: com.property.propertymanagement.network.ApiService
    private var pendingCount = 0
    private var currentFragmentTag = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_with_nav)

        // 设置Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        apiService = RetrofitClient.createApiService(this)
        initViews()
        setupBottomNavigation()

        // 默认显示首页
        showFragment(HomeFragment(), "home")

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
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(HomeFragment(), "home")
                    true
                }
                R.id.nav_fee -> {
                    startActivity(Intent(this, FeeManagementActivity::class.java))
                    true
                }
                R.id.nav_repair -> {
                    startActivity(Intent(this, RepairActivity::class.java))
                    true
                }
                R.id.nav_complaint -> {
                    startActivity(Intent(this, ComplaintActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    // 跳转到个人中心页面（全屏Activity）
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun showFragment(fragment: Fragment, tag: String) {
        if (currentFragmentTag == tag) return

        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()

        // 隐藏当前Fragment
        val currentFragment = fragmentManager.findFragmentByTag(currentFragmentTag)
        if (currentFragment != null) {
            transaction.hide(currentFragment)
        }

        // 如果Fragment已经存在，显示它；否则添加
        val targetFragment = fragmentManager.findFragmentByTag(tag)
        if (targetFragment != null) {
            transaction.show(targetFragment)
        } else {
            transaction.add(R.id.fragment_container, fragment, tag)
        }

        transaction.commit()
        currentFragmentTag = tag
    }

    private fun loadPendingCount() {
        apiService.getPendingCount().enqueue(object : Callback<com.property.propertymanagement.network.ApiResult<Int>> {
            override fun onResponse(
                call: Call<com.property.propertymanagement.network.ApiResult<Int>>,
                response: Response<com.property.propertymanagement.network.ApiResult<Int>>
            ) {
                if (response.isSuccessful && response.body()?.code == 200) {
                    pendingCount = response.body()?.data ?: 0
                    // 更新首页的待审批数量（如果首页Fragment存在）
                    updateHomeFragmentBadge()
                }
            }

            override fun onFailure(call: Call<com.property.propertymanagement.network.ApiResult<Int>>, t: Throwable) {
                // 静默失败
            }
        })
    }

    private fun updateHomeFragmentBadge() {
        val homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
        homeFragment?.updatePendingCount(pendingCount)
    }

    // 移除菜单（因为现在在个人中心退出）
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                // 刷新数据
                if (currentFragmentTag == "home") {
                    val homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
                    homeFragment?.refreshData()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showWelcomeMessage() {
        val username = PermissionUtil.getCurrentUsername(this)
        val role = PermissionUtil.getCurrentRole(this)
        val roleText = if (role == "ADMIN") "管理员" else "居民"

        Toast.makeText(this, "欢迎回来，$username ($roleText)", Toast.LENGTH_SHORT).show()
    }

    // 处理Token过期，返回到登录页面
    fun handleTokenExpired() {
        AlertDialog.Builder(this)
            .setTitle("登录已过期")
            .setMessage("您的登录状态已过期，请重新登录")
            .setCancelable(false)
            .setPositiveButton("重新登录") { _, _ ->
                logout()
            }
            .show()
    }

    private fun logout() {
        PermissionUtil.clearAllUserData(this)

        val sharedPref = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}