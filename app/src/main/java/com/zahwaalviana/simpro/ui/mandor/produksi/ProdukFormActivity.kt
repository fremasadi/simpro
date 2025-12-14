package com.zahwaalviana.simpro.ui.mandor.produksi

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.zahwaalviana.simpro.data.model.Barang
import com.zahwaalviana.simpro.data.model.Produksi
import com.zahwaalviana.simpro.databinding.ActivityProduksiFormBinding
import java.util.*

class ProdukFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProduksiFormBinding
    private val db = FirebaseFirestore.getInstance()

    private var isEditMode = false
    private var produksiData: Produksi? = null
    private var barangList = listOf<Barang>()
    private var selectedBarang: Barang? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProduksiFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        loadBarangForSpinner()
        checkEditMode()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadBarangForSpinner() {
        db.collection("master_barang")
            .get()
            .addOnSuccessListener { snap ->
                barangList = snap.documents.mapNotNull { d ->
                    d.toObject(Barang::class.java)?.copy(barangId = d.id)
                }
                val names = barangList.map { it.namaBarangJadi }
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, names)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                binding.spinnerBarang.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat barang: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkEditMode() {
        produksiData = intent.getParcelableExtra("PRODUKSI_DATA")
        if (produksiData != null) {
            isEditMode = true
            supportActionBar?.title = "Edit Produksi"
            populateData(produksiData!!)
        } else {
            isEditMode = false
            supportActionBar?.title = "Tambah Produksi"
            binding.etTanggal.setText(java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
        }
    }

    private fun populateData(p: Produksi) {
        // set spinner selection
        val index = barangList.indexOfFirst { it.barangId == p.barangId }
        if (index >= 0) binding.spinnerBarang.setSelection(index)
        binding.etJumlahProduksi.setText(p.jumlah_produksi.toString())
        binding.etJumlahKeluar.setText(p.jumlah_keluar.toString())
        binding.etTanggal.setText(p.tanggal?.toDate()?.let { java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it) } ?: "")
    }

    private fun setupListeners() {
        binding.etTanggal.setOnClickListener {
            showDatePicker()
        }

        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                if (isEditMode) updateProduksi()
                else saveProduksi()
            }
        }

        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun showDatePicker() {
        val cal = Calendar.getInstance()
        val dp = DatePickerDialog(this, { _, y, m, d ->
            val date = Calendar.getInstance().apply { set(y, m, d) }.time
            binding.etTanggal.setText(java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date))
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH))
        dp.show()
    }

    private fun validateInput(): Boolean {
        if (binding.spinnerBarang.selectedItemPosition < 0) {
            Toast.makeText(this, "Pilih barang", Toast.LENGTH_SHORT).show()
            return false
        }
        if (binding.etJumlahProduksi.text.toString().trim().isEmpty()) {
            binding.etJumlahProduksi.error = "Isi jumlah produksi"
            return false
        }
        if (binding.etJumlahKeluar.text.toString().trim().isEmpty()) {
            binding.etJumlahKeluar.error = "Isi jumlah keluar (0 jika tidak ada)"
            return false
        }
        return true
    }

    private fun saveProduksi() {
        showLoading(true)
        val barang = barangList[binding.spinnerBarang.selectedItemPosition]
        val jumlahProd = binding.etJumlahProduksi.text.toString().toInt()
        val jumlahKeluar = binding.etJumlahKeluar.text.toString().toInt()
        val dateStr = binding.etTanggal.text.toString()
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: Date()
        val produksi = Produksi(
            tanggal = Timestamp(date),
            barangId = barang.barangId ?: "",
            namaBarangJadi = barang.namaBarangJadi,
            jumlah_produksi = jumlahProd,
            jumlah_keluar = jumlahKeluar,
            mandorId = null
        )

        // add doc then update stok using transaction
        db.collection("produksi_harian")
            .add(produksi)
            .addOnSuccessListener { docRef ->
                val delta = (jumlahProd - jumlahKeluar).toLong()
                val barangRef = db.collection("master_barang").document(barang.barangId!!)
                db.runTransaction { transaction ->
                    val snap = transaction.get(barangRef)
                    val curr = (snap.getLong("total_stok") ?: 0L)
                    transaction.update(barangRef, "total_stok", curr + delta)
                }.addOnSuccessListener {
                    showLoading(false)
                    Toast.makeText(this, "Produksi berhasil disimpan", Toast.LENGTH_SHORT).show()
                    finish()
                }.addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Gagal update stok: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Gagal simpan produksi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProduksi() {
        val produksiId = produksiData?.produksiId ?: return
        showLoading(true)

        val barangBaru = barangList[binding.spinnerBarang.selectedItemPosition]
        val newProd = binding.etJumlahProduksi.text.toString().toInt()
        val newKeluar = binding.etJumlahKeluar.text.toString().toInt()
        val dateStr = binding.etTanggal.text.toString()
        val newDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr) ?: Date()

        val produksiRef = db.collection("produksi_harian").document(produksiId)

        db.runTransaction { trx ->

            // ===============================
            // 1️⃣ BACA DATA LAMA PRODUKSI
            // ===============================
            val snapOldProd = trx.get(produksiRef)
            val oldProd = snapOldProd.getLong("jumlah_produksi")?.toInt() ?: 0
            val oldKeluar = snapOldProd.getLong("jumlah_keluar")?.toInt() ?: 0
            val oldBarangId = snapOldProd.getString("barangId") ?: barangBaru.barangId!!

            val oldNet = oldProd - oldKeluar
            val newNet = newProd - newKeluar

            // ===============================
            // 2️⃣ READ semua dokumen barang dulu
            // ===============================

            // Barang Lama
            val oldBarangRef = db.collection("master_barang").document(oldBarangId)
            val snapOldBarang = trx.get(oldBarangRef)
            val stokOldBarang = snapOldBarang.getLong("total_stok") ?: 0L

            // Barang Baru (bisa sama, bisa beda)
            val newBarangRef = db.collection("master_barang").document(barangBaru.barangId!!)
            val snapNewBarang = trx.get(newBarangRef)
            val stokNewBarang = snapNewBarang.getLong("total_stok") ?: 0L

            // ===============================
            // 3️⃣ SETELAH SEMUA READ SELESAI → BOLEH WRITE
            // ===============================

            // update data produksi
            trx.update(produksiRef, mapOf(
                "tanggal" to Timestamp(newDate),
                "barangId" to barangBaru.barangId,
                "namaBarangJadi" to barangBaru.namaBarangJadi,
                "jumlah_produksi" to newProd,
                "jumlah_keluar" to newKeluar
            ))

            if (oldBarangId == barangBaru.barangId) {
                // barang sama → cukup update delta
                val delta = (newNet - oldNet)
                trx.update(newBarangRef, "total_stok", stokNewBarang + delta)
            } else {
                // barang berbeda → revert dari barang lama, apply ke barang baru
                trx.update(oldBarangRef, "total_stok", stokOldBarang - oldNet)
                trx.update(newBarangRef, "total_stok", stokNewBarang + newNet)
            }

        }.addOnSuccessListener {
            showLoading(false)
            Toast.makeText(this, "Produksi berhasil diperbarui", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener { e ->
            showLoading(false)
            Log.e("ProduksiUpdate", "Error update produksi", e)
            Toast.makeText(this, "Gagal update: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
