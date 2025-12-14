package com.zahwaalviana.simpro

import android.content.Intent
import android.os.Bundle
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.postDelayed
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.ui.admin.AdminMainActivity
import com.zahwaalviana.simpro.ui.auth.LoginActivity
import com.zahwaalviana.simpro.ui.mandor.MandorMainActivity
import java.util.logging.Handler

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        android.os.Handler(Looper.getMainLooper()).postDelayed({
            checkUserAuth()
        }, 1500)
    }
    private fun checkUserAuth() {
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val user = auth.currentUser
        if (user == null) {
            // belum login, arahkan ke login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        } else {
            // sudah login, cek role
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    when (doc.getString("role")) {
                        "admin" -> startActivity(Intent(this, AdminMainActivity::class.java))
                        "mandor" -> startActivity(Intent(this, MandorMainActivity::class.java))
                        else -> startActivity(Intent(this, LoginActivity::class.java))
                    }
                    finish()
                }
                .addOnFailureListener {
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
        }    }
}
