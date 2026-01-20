package com.property.propertymanagement

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.property.propertymanagement.fragment.*
import com.property.propertymanagement.util.PermissionUtil

class MainActivity : AppCompatActivity() {
    private lateinit var bottomNavigation: BottomNavigationView
    private var currentFragmentTag = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_with_nav)

        initViews()
        setupBottomNavigation()

        // 默认显示物业费页面
        showFragment(FeeFragment(), "fee")
    }

    override fun onResume() {
        super.onResume()
        showWelcomeMessage()
    }

    private fun initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation)
    }

    private fun setupBottomNavigation() {
        bottomNavigation.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_fee -> {
                    showFragment(FeeFragment(), "fee")
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
                R.id.nav_profile -> {
                    showFragment(ProfileFragment(), "profile")
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

    private fun showWelcomeMessage() {
        val username = PermissionUtil.getCurrentUsername(this)
        val role = PermissionUtil.getCurrentRole(this)
        val roleText = if (role == "ADMIN") "管理员" else "居民"

        // 只在第一次进入时显示
        if (!isMessageShown()) {
            Toast.makeText(this, "欢迎回来，$username ($roleText)", Toast.LENGTH_SHORT).show()
            setMessageShown()
        }
    }

    private fun isMessageShown(): Boolean {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPref.getBoolean("welcome_shown", false)
    }

    private fun setMessageShown() {
        val sharedPref = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPref.edit().putBoolean("welcome_shown", true).apply()
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
            "fee" -> {
                val fragment = supportFragmentManager.findFragmentByTag("fee") as? FeeFragment
                fragment?.loadFeeData()
            }
            "repair" -> {
                val fragment = supportFragmentManager.findFragmentByTag("repair") as? RepairFragment
                fragment?.loadRepairData()
            }
            "complaint" -> {
                val fragment = supportFragmentManager.findFragmentByTag("complaint") as? ComplaintFragment
                fragment?.loadComplaintData()
            }
            "parking" -> {
                val fragment = supportFragmentManager.findFragmentByTag("parking") as? ParkingFragment
                fragment?.loadParkingData()
            }
            "profile" -> {
                val fragment = supportFragmentManager.findFragmentByTag("profile") as? ProfileFragment
                fragment?.loadUserInfo()
            }
        }
    }
}