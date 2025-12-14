package com.zahwaalviana.simpro.ui.mandor

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.ui.auth.LoginActivity
import com.zahwaalviana.simpro.ui.mandor.produksi.ProduksiListFragment


class MandorMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: androidx.drawerlayout.widget.DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mandor_main)

        auth = FirebaseAuth.getInstance()

        // Setup toolbar
        toolbar = findViewById(R.id.toolbar_mandor)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Dashboard Mandor"

        // Setup drawer
        drawerLayout = findViewById(R.id.drawer_layout_mandor)
        navigationView = findViewById(R.id.nav_view_mandor)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.nav_produksi -> {
                supportActionBar?.title = "Produksi Harian"
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_mandor, ProduksiListFragment())
                    .commit()
            }

            R.id.nav_laporan -> {
                supportActionBar?.title = "Laporan Mandor"
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.container_mandor, LaporanMandorFragment())
//                    .commit()
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }
}
