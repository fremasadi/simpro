package com.zahwaalviana.simpro.ui.admin.kemasan

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.data.model.Barang
import com.zahwaalviana.simpro.data.model.Kemasan
import com.zahwaalviana.simpro.databinding.ActivityKemasanFormBinding

class KemasanFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKemasanFormBinding
    private val db = FirebaseFirestore.getInstance()

    private var isEditMode = false
    private var kemasanData: Kemasan? = null

    private var barangList = listOf<Barang>()
    private var selectedBarangId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKemasanFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        checkEditMode()
        loadBarangList()
        setupListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun checkEditMode() {
        kemasanData = intent.getParcelableExtra("KEMASAN_DATA")

        if (kemasanData != null) {
            isEditMode = true
            supportActionBar?.title = "Edit Kemasan"
            selectedBarangId = kemasanData?.barangId
            populateData(kemasanData!!)
        } else {
            isEditMode = false
            supportActionBar?.title = "Tambah Kemasan"
        }
    }

    private fun loadBarangList() {
        showLoading(true)

        db.collection("master_barang")
            .get()
            .addOnSuccessListener { snapshot ->
                showLoading(false)

                if (snapshot.isEmpty) {
                    Toast.makeText(this, "Belum ada barang. Tambahkan barang terlebih dahulu.", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                barangList = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Barang::class.java)?.copy(barangId = doc.id)
                }

                setupBarangSpinner()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Gagal memuat data barang: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupBarangSpinner() {
        val barangNames = barangList.map { it.namaBarangJadi }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            barangNames
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerBarang.adapter = adapter

        binding.spinnerBarang.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedBarangId = barangList[position].barangId
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedBarangId = null
            }
        }

        // Set selected barang jika edit mode
        if (isEditMode && selectedBarangId != null) {
            val index = barangList.indexOfFirst { it.barangId == selectedBarangId }
            if (index != -1) {
                binding.spinnerBarang.setSelection(index)
            }
        }
    }

    private fun populateData(kemasan: Kemasan) {
        binding.etNamaKemasan.setText(kemasan.namaKemasan)
    }

    private fun setupListeners() {
        binding.btnSave.setOnClickListener {
            if (validateInput()) {
                if (isEditMode) {
                    updateKemasan()
                } else {
                    saveKemasan()
                }
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }
    }

    private fun validateInput(): Boolean {
        val namaKemasan = binding.etNamaKemasan.text.toString().trim()

        when {
            namaKemasan.isEmpty() -> {
                binding.etNamaKemasan.error = "Nama kemasan tidak boleh kosong"
                binding.etNamaKemasan.requestFocus()
                return false
            }
            selectedBarangId == null -> {
                Toast.makeText(this, "Pilih barang terlebih dahulu", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun saveKemasan() {
        showLoading(true)

        val kemasan = hashMapOf(
            "namaKemasan" to binding.etNamaKemasan.text.toString().trim(),
            "barangId" to selectedBarangId!!
        )

        db.collection("master_kemasan")
            .add(kemasan)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Kemasan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Gagal menambahkan kemasan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateKemasan() {
        showLoading(true)

        val kemasanId = kemasanData?.kemasanId ?: return

        val updatedKemasan = hashMapOf(
            "namaKemasan" to binding.etNamaKemasan.text.toString().trim(),
            "barangId" to selectedBarangId!!
        )

        db.collection("master_kemasan")
            .document(kemasanId)
            .update(updatedKemasan as Map<String, Any>)
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Kemasan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Gagal memperbarui kemasan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnCancel.isEnabled = !isLoading
        binding.etNamaKemasan.isEnabled = !isLoading
        binding.spinnerBarang.isEnabled = !isLoading
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}