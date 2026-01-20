package com.property.propertymanagement

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.property.propertymanagement.fragment.*
import com.property.propertymanagement.util.PermissionUtil
import com.property.propertymanagement.R
import com.property.propertymanagement.databinding.ActivityMainWithNavBinding
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainWithNavBinding
    private lateinit var bottomNavigation: BottomNavigationView
    private var pendingCount = 0
    private var currentFragmentTag = "home"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainWithNavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        initViews()
        setupBottomNavigation()

        // 默认显示首页
        showFragment(HomeFragment(), "home")

        // 显示欢迎信息
        showWelcomeMessage()
    }

    override fun onResume() {
        super.onResume()
        // 如果当前是管理员的HomeFragment，加载待审批数量
        if (currentFragmentTag == "home" && PermissionUtil.isAdmin(this)) {
            loadPendingCount()
        }
    }

    private fun initViews() {
        bottomNavigation = binding.bottomNavigation
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showFragment(HomeFragment(), "home")
                    true
                }
                R.id.nav_fee -> {
                    showFragment(FeeManagementFragment(), "fee")
                    true
                }
                R.id.nav_repair -> {
                    showFragment(RepairFragment(), "repair")
                    true
                }
                R.id.nav_complaint -> {
                    showFragment(ComplaintFragment(), "complaint")
                    true
                }
                R.id.nav_parking -> {
                    showFragment(ParkingFragment(), "parking")
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

        transaction.commitAllowingStateLoss()
        currentFragmentTag = tag

        // 更新Toolbar标题
        updateToolbarTitle(tag)
    }

    private fun updateToolbarTitle(fragmentTag: String) {
        val title = when (fragmentTag) {
            "home" -> "物业管理系统"
            "fee" -> "物业费管理"
            "repair" -> "维修管理"
            "complaint" -> "投诉管理"
            "parking" -> "车位管理"
            else -> "物业管理系统"
        }
        supportActionBar?.title = title
    }

    private fun loadPendingCount() {
        // 这里可以加载待审批数量，如果有需要
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                refreshCurrentFragment()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshCurrentFragment() {
        when (currentFragmentTag) {
            "home" -> {
                val homeFragment = supportFragmentManager.findFragmentByTag("home") as? HomeFragment
                homeFragment?.refreshData()
            }
            "fee" -> {
                val feeFragment = supportFragmentManager.findFragmentByTag("fee") as? FeeManagementFragment
                feeFragment?.loadFeeData()
            }
            "repair" -> {
                val repairFragment = supportFragmentManager.findFragmentByTag("repair") as? RepairFragment
                repairFragment?.loadRepairData()
            }
            "complaint" -> {
                val complaintFragment = supportFragmentManager.findFragmentByTag("complaint") as? ComplaintFragment
                complaintFragment?.loadComplaintData()
            }
            "parking" -> {
                val parkingFragment = supportFragmentManager.findFragmentByTag("parking") as? ParkingFragment
                parkingFragment?.loadParkingData()
            }
        }
        Toast.makeText(this, "已刷新", Toast.LENGTH_SHORT).show()
    }

    private fun showWelcomeMessage() {
        val username = PermissionUtil.getCurrentUsername(this)
        val role = PermissionUtil.getCurrentRole(this)
        val roleText = if (role == "ADMIN") "管理员" else "居民"

        if (!username.isNullOrEmpty()) {
            Toast.makeText(this, "欢迎回来，$username ($roleText)", Toast.LENGTH_SHORT).show()
        }
    }

    // 处理Token过期，返回到登录页面
    fun handleTokenExpired() {
        logout()
    }

    private fun logout() {
        PermissionUtil.clearAllUserData(this)

        val sharedPref = getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            clear()
            apply()
        }

        val intent = android.content.Intent(this, com.property.propertymanagement.activity.LoginActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}