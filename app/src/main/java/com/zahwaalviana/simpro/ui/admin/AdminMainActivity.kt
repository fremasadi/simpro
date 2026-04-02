package com.zahwaalviana.simpro.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.toColorInt
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.ui.auth.LoginActivity
import com.zahwaalviana.simpro.ui.barang.BarangListFragment
import com.zahwaalviana.simpro.ui.kemasan.KemasanListFragment
import com.zahwaalviana.simpro.ui.pengeluaran.PengeluaranListFragment
import com.zahwaalviana.simpro.ui.penjualan.PenjualanListFragment
import com.zahwaalviana.simpro.ui.produksi.ProduksiListFragment
import com.zahwaalviana.simpro.ui.user.UserListFragment

class AdminMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = "#E59BA6".toColorInt()

        setContentView(R.layout.activity_admin_main)

        auth = FirebaseAuth.getInstance()

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        if (savedInstanceState == null) {
            loadDashboard()
        }
    }

    private fun loadDashboard() {
        supportActionBar?.title = "Dashboard Admin"
        supportFragmentManager.beginTransaction()
            .replace(R.id.container_admin, DashboardAdminFragment())
            .commit()
        navigationView.setCheckedItem(R.id.nav_dashboard)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.nav_dashboard -> loadDashboard()

            R.id.nav_manage_users -> {
                supportActionBar?.title = "Kelola Pengguna"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, UserListFragment())
                    .commit()
            }

            R.id.nav_barang -> {
                supportActionBar?.title = "Master Barang"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, BarangListFragment())
                    .commit()
            }

            R.id.nav_kemasan -> {
                supportActionBar?.title = "Master Kemasan"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, KemasanListFragment())
                    .commit()
            }

            R.id.nav_produksi -> {
                supportActionBar?.title = "Produksi"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, ProduksiListFragment.newInstance("admin"))
                    .commit()
            }

            R.id.nav_pengeluran -> {
                supportActionBar?.title = "Pengeluaran"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, PengeluaranListFragment())
                    .commit()
            }

            R.id.nav_penjualan -> {
                supportActionBar?.title = "POS / Kasir"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_admin, PenjualanListFragment())
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
}
