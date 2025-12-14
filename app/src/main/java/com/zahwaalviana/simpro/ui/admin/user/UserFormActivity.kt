package com.zahwaalviana.simpro.ui.admin.user

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.User
import java.util.UUID

class UserFormActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var userId: String? = null
    private var isEditMode = false

    private lateinit var tilName: TextInputLayout
    private lateinit var tilEmail: TextInputLayout
    private lateinit var tilPassword: TextInputLayout
    private lateinit var spinnerRole: Spinner
    private lateinit var btnSave: Button
    private lateinit var btnDelete: Button
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_form)

        // Setup toolbar/actionbar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Form Pengguna"

        initViews()
        setupSpinner()

        userId = intent.getStringExtra("uid")
        isEditMode = userId != null

        if (isEditMode) {
            loadUserData(userId!!)
            btnDelete.visibility = View.VISIBLE
            tilPassword.visibility = View.GONE
            supportActionBar?.title = "Edit Pengguna"
        } else {
            btnDelete.visibility = View.GONE
            tilPassword.visibility = View.VISIBLE
            supportActionBar?.title = "Tambah Pengguna"
        }

        btnSave.setOnClickListener { validateAndSaveUser() }
        btnDelete.setOnClickListener { showDeleteConfirmation() }
    }

    private fun initViews() {
        tilName = findViewById(R.id.tilName)
        tilEmail = findViewById(R.id.tilEmail)
        tilPassword = findViewById(R.id.tilPassword)
        spinnerRole = findViewById(R.id.spinnerRole)
        btnSave = findViewById(R.id.btnSaveUser)
        btnDelete = findViewById(R.id.btnDeleteUser)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        val roles = arrayOf("Pilih Role", "admin", "mandor")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, roles)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRole.adapter = adapter
    }

    private fun loadUserData(id: String) {
        showLoading(true)
        db.collection("users").document(id).get()
            .addOnSuccessListener { document ->
                showLoading(false)
                val user = document.toObject(User::class.java)
                if (user != null) {
                    tilName.editText?.setText(user.name)
                    tilEmail.editText?.setText(user.email)
                    tilEmail.isEnabled = false // tidak bisa ubah email

                    // Set spinner role
                    val rolePosition = when(user.role) {
                        "admin" -> 1
                        "mandor" -> 2
                        else -> 0
                    }
                    spinnerRole.setSelection(rolePosition)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Gagal memuat data: ${e.message}")
            }
    }

    private fun validateAndSaveUser() {
        val name = tilName.editText?.text.toString().trim()
        val email = tilEmail.editText?.text.toString().trim()
        val password = tilPassword.editText?.text.toString().trim()
        val rolePosition = spinnerRole.selectedItemPosition

        // Validasi
        var isValid = true

        if (name.isEmpty()) {
            tilName.error = "Nama wajib diisi"
            isValid = false
        } else {
            tilName.error = null
        }

        if (email.isEmpty()) {
            tilEmail.error = "Email wajib diisi"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.error = "Format email tidak valid"
            isValid = false
        } else {
            tilEmail.error = null
        }

        if (!isEditMode && password.isEmpty()) {
            tilPassword.error = "Password wajib diisi"
            isValid = false
        } else if (!isEditMode && password.length < 6) {
            tilPassword.error = "Password minimal 6 karakter"
            isValid = false
        } else {
            tilPassword.error = null
        }

        if (rolePosition == 0) {
            Toast.makeText(this, "Pilih role user", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        if (!isValid) return

        val role = spinnerRole.selectedItem.toString()
        saveUser(name, email, password, role)
    }

    private fun saveUser(name: String, email: String, password: String, role: String) {
        showLoading(true)

        if (isEditMode) {
            // Update user
            val user = User(userId!!, name, email, role)
            db.collection("users").document(userId!!).set(user)
                .addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(this, "Data berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showError("Gagal memperbarui: ${e.message}")
                }
        } else {
            // Tambah user baru
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val newUid = result.user?.uid ?: UUID.randomUUID().toString()
                    val newUser = User(newUid, name, email, role)

                    db.collection("users").document(newUid).set(newUser)
                        .addOnSuccessListener {
                            showLoading(false)
                            Toast.makeText(this, "User berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            showLoading(false)
                            // Hapus auth jika gagal simpan ke firestore
                            result.user?.delete()
                            showError("Gagal menyimpan: ${e.message}")
                        }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showError("Gagal membuat akun: ${e.message}")
                }
        }
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Pengguna")
            .setMessage("Apakah Anda yakin ingin menghapus pengguna ini? Tindakan ini tidak dapat dibatalkan.")
            .setPositiveButton("Hapus") { _, _ ->
                deleteUser()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteUser() {
        userId?.let { uid ->
            showLoading(true)

            // Hapus dari Firestore dulu
            db.collection("users").document(uid).delete()
                .addOnSuccessListener {
                    // Hapus dari Auth menggunakan Cloud Function
                    // Karena kita tidak bisa hapus user lain dari client
                    deleteUserFromAuth(uid)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showError("Gagal menghapus dari Firestore: ${e.message}")
                }
        }
    }

    private fun deleteUserFromAuth(uid: String) {
        // CATATAN: Untuk hapus user dari Auth, idealnya menggunakan Firebase Admin SDK
        // melalui Cloud Functions. Untuk sementara kita hanya hapus dari Firestore
        // Jika ingin hapus dari Auth juga, perlu setup Cloud Function

        // Alternatif sementara: hanya hapus dari Firestore
        showLoading(false)
        Toast.makeText(this, "User berhasil dihapus", Toast.LENGTH_SHORT).show()
        finish()

        /*
        // Kode untuk Cloud Function (perlu disetup terpisah):
        val data = hashMapOf("uid" to uid)
        functions.getHttpsCallable("deleteUser")
            .call(data)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "User berhasil dihapus", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                // Sudah dihapus dari Firestore, Auth gagal dihapus
                Toast.makeText(this, "User dihapus dari database, tapi tidak dari Auth", Toast.LENGTH_LONG).show()
                finish()
            }
        */
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSave.isEnabled = !isLoading
        btnDelete.isEnabled = !isLoading
        tilName.isEnabled = !isLoading
        if (!isEditMode) {
            tilEmail.isEnabled = !isLoading
            tilPassword.isEnabled = !isLoading
        }
        spinnerRole.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}