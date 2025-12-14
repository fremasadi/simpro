package com.zahwaalviana.simpro.ui.admin.barang

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.data.model.Barang
import com.zahwaalviana.simpro.databinding.ActivityBarangFormBinding

class BarangFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBarangFormBinding
    private val db = FirebaseFirestore.getInstance()

    private var isEditMode = false
    private var barangData: Barang? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarangFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        checkEditMode()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun checkEditMode() {
        barangData = intent.getParcelableExtra("BARANG_DATA")

        if (barangData != null) {
            isEditMode = true
            supportActionBar?.title = "Edit Barang"
            populateData(barangData!!)
        } else {
            isEditMode = false
            supportActionBar?.title = "Tambah Barang"
        }
    }

    private fun populateData(barang: Barang) {
        binding.etNamaBahanBaku.setText(barang.namaBahanBaku)
        binding.etNamaBarangJadi.setText(barang.namaBarangJadi)
        binding.etTotalStok.setText(barang.total_stok.toString())

    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                if (isEditMode) {
                    updateBarang()
                } else {
                    saveBarang()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(): Boolean {
        val namaBahanBaku = binding.etNamaBahanBaku.text.toString().trim()
        val namaBarangJadi = binding.etNamaBarangJadi.text.toString().trim()
        val totalStok = binding.etTotalStok.text.toString().trim()
        if (totalStok.isEmpty()) {
            binding.etTotalStok.error = "Total stok tidak boleh kosong"
            binding.etTotalStok.requestFocus()
            return false
        }

        when {
            namaBahanBaku.isEmpty() -> {
                binding.etNamaBahanBaku.error = "Nama bahan baku tidak boleh kosong"
                binding.etNamaBahanBaku.requestFocus()
                return false
            }
            namaBarangJadi.isEmpty() -> {
                binding.etNamaBarangJadi.error = "Nama barang jadi tidak boleh kosong"
                binding.etNamaBarangJadi.requestFocus()
                return false
            }
        }

        return true
    }

    private fun saveBarang() {
        showLoading(true)

        val barang = Barang(
            namaBahanBaku = binding.etNamaBahanBaku.text.toString().trim(),
            namaBarangJadi = binding.etNamaBarangJadi.text.toString().trim(),
                    total_stok = binding.etTotalStok.text.toString().toInt()

        )

        db.collection("master_barang")
            .add(barang)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Barang berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Gagal menambahkan barang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBarang() {
        showLoading(true)

        val barangId = barangData?.barangId ?: return

        val updatedBarang = hashMapOf(
            "namaBahanBaku" to binding.etNamaBahanBaku.text.toString().trim(),
            "namaBarangJadi" to binding.etNamaBarangJadi.text.toString().trim(),
            "total_stok" to binding.etTotalStok.text.toString().toInt()

        )

        db.collection("master_barang")
            .document(barangId)
            .update(updatedBarang as Map<String, Any>)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Barang berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Gagal memperbarui barang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
        binding.etNamaBahanBaku.isEnabled = !isLoading
        binding.etNamaBarangJadi.isEnabled = !isLoading
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}