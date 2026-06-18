package com.zahwaalviana.simpro.ui.kemasan

import android.R
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.databinding.ActivityKemasanFormBinding

class KemasanFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKemasanFormBinding
    private lateinit var db: FirebaseFirestore
    private var kemasanId: String? = null
    private var isEditMode = false

    private val satuanList = listOf("Kg", "Liter", "Gram", "Ml", "Pcs", "Box", "Karton", "Botol", "Kaleng")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKemasanFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupSatuanDropdown()
        checkEditMode()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupSatuanDropdown() {
        val adapter = ArrayAdapter(this, R.layout.simple_dropdown_item_1line, satuanList)
        binding.actvSatuan.setAdapter(adapter)
    }

    private fun checkEditMode() {
        kemasanId = intent.getStringExtra("KEMASAN_ID")
        isEditMode = kemasanId != null

        if (isEditMode) {
            supportActionBar?.title = "Edit Kemasan"
            loadKemasanData()
        } else {
            supportActionBar?.title = "Tambah Kemasan"
        }
    }

    private fun loadKemasanData() {
        showLoading(true)
        db.collection("master_kemasan")
            .document(kemasanId!!)
            .get()
            .addOnSuccessListener { document ->
                showLoading(false)
                if (document.exists()) {
                    binding.etNamaKemasan.setText(document.getString("nama_kemasan"))
                    binding.actvSatuan.setText(document.getString("satuan"), false)
                    binding.etStok.setText(document.getDouble("stok")?.toString() ?: "0")
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        binding.btnSimpan.setOnClickListener {
            if (validateInput()) {
                saveKemasan()
            }
        }
    }

    private fun validateInput(): Boolean {
        val namaKemasan = binding.etNamaKemasan.text.toString().trim()
        val satuan = binding.actvSatuan.text.toString().trim()
        val stok = binding.etStok.text.toString().trim()

        when {
            namaKemasan.isEmpty() -> {
                binding.tilNamaKemasan.error = "Nama kemasan tidak boleh kosong"
                return false
            }
            satuan.isEmpty() -> {
                binding.tilSatuan.error = "Satuan tidak boleh kosong"
                return false
            }
            stok.isEmpty() -> {
                binding.tilStok.error = "Stok tidak boleh kosong"
                return false
            }
            else -> {
                binding.tilNamaKemasan.error = null
                binding.tilSatuan.error = null
                binding.tilStok.error = null
                return true
            }
        }
    }

    private fun saveKemasan() {
        val namaKemasan = binding.etNamaKemasan.text.toString().trim()
        val satuan = binding.actvSatuan.text.toString().trim()
        val stok = binding.etStok.text.toString().toDoubleOrNull() ?: 0.0

        showLoading(true)

        // Cek apakah nama kemasan sudah ada
        db.collection("master_kemasan")
            .whereEqualTo("nama_kemasan", namaKemasan)
            .get()
            .addOnSuccessListener { documents ->
                var isDuplicate = false
                for (doc in documents) {
                    if (isEditMode) {
                        // Jika edit, abaikan jika ID sama dengan data yang sedang diedit
                        if (doc.id != kemasanId) {
                            isDuplicate = true
                            break
                        }
                    } else {
                        // Jika tambah baru, semua temuan adalah duplikat
                        isDuplicate = true
                        break
                    }
                }

                if (isDuplicate) {
                    showLoading(false)
                    binding.tilNamaKemasan.error = "Nama kemasan sudah ada"
                    Toast.makeText(this, "Data kemasan sudah ada", Toast.LENGTH_SHORT).show()
                } else {
                    executeSave(namaKemasan, satuan, stok)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error checking duplicate: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun executeSave(namaKemasan: String, satuan: String, stok: Double) {
        val kemasanData = hashMapOf(
            "nama_kemasan" to namaKemasan,
            "satuan" to satuan,
            "stok" to stok,
            "updated_at" to System.currentTimeMillis()
        )

        if (isEditMode) {
            // Update existing data
            db.collection("master_kemasan")
                .document(kemasanId!!)
                .update(kemasanData as Map<String, Any>)
                .addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(this, "Data berhasil diupdate", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Add new data
            kemasanData["created_at"] = System.currentTimeMillis()

            db.collection("master_kemasan")
                .add(kemasanData)
                .addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(this, "Data berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !show
    }
}