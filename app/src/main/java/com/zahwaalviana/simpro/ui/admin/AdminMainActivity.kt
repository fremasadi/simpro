package com.zahwaalviana.simpro.ui.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.ui.admin.barang.BarangListFragment
import com.zahwaalviana.simpro.ui.admin.kemasan.KemasanListFragment
import com.zahwaalviana.simpro.ui.auth.LoginActivity
import com.zahwaalviana.simpro.ui.admin.user.UserListFragment

class AdminMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.parseColor("#E59BA6")

        setContentView(R.layout.activity_admin_main)

        auth = FirebaseAuth.getInstance()

        // Setup Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Setup Drawer
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        // Setup Hamburger Icon
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set title
        supportActionBar?.title = "Dashboard Admin"
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> {
                supportActionBar?.title = "Dashboard Admin"
            }

            R.id.nav_manage_users -> {
                supportActionBar?.title = "Kelola Pengguna"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, UserListFragment())
                    .commit()
            }

            R.id.nav_barang -> {   // ← Menu barang
                supportActionBar?.title = "Master Barang"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, BarangListFragment())
                    .commit()
            }

            R.id.nav_kemasan -> {   // ← Menu barang
                supportActionBar?.title = "Master Kemasan"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, KemasanListFragment())
                    .commit()
            }

            R.id.nav_logout -> {
                auth.signOut()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }



    @SuppressLint("GestureBackNavigation")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}