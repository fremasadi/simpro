package com.zahwaalviana.simpro.ui.barang

import Kemasan
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.databinding.ActivityBarangFormBinding

class BarangFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBarangFormBinding
    private lateinit var db: FirebaseFirestore
    private var barangId: String? = null
    private var isEditMode = false
    private val varianViews = mutableListOf<View>()
    private val kemasanList = mutableListOf<Kemasan>()
    private var kemasanMap = mutableMapOf<String, Kemasan>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBarangFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupListeners()

        // Load kemasan dulu, baru check edit mode
        loadKemasanData {
            checkEditMode()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadKemasanData(onComplete: () -> Unit = {}) {
        showLoading(true)
        db.collection("master_kemasan")
            .orderBy("nama_kemasan")
            .get()
            .addOnSuccessListener { documents ->
                kemasanList.clear()
                kemasanMap.clear()

                if (documents.isEmpty) {
                    showLoading(false)
                    Toast.makeText(this, "Belum ada data kemasan. Silakan tambah kemasan terlebih dahulu.", Toast.LENGTH_LONG).show()
                    finish()
                    return@addOnSuccessListener
                }

                documents.forEach { doc ->
                    val kemasan = Kemasan(
                        id = doc.id, // Doc ID dari Firestore (contoh: j0Svn0gZ1lla8b0De6HP)
                        namaKemasan = doc.getString("nama_kemasan") ?: "",
                        satuan = doc.getString("satuan") ?: "",
                        stok = doc.getDouble("stok") ?: 0.0
                    )
                    kemasanList.add(kemasan)
                    // Map untuk mencari kemasan berdasarkan display text
                    kemasanMap["${kemasan.namaKemasan} (${kemasan.satuan})"] = kemasan
                }
                showLoading(false)

                // Debug log
                Log.d("BarangForm", "Loaded ${kemasanList.size} kemasan: ${kemasanList.map { it.namaKemasan }}")

                onComplete()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error loading kemasan: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("BarangForm", "Error loading kemasan", e)
            }
    }

    private fun checkEditMode() {
        barangId = intent.getStringExtra("BARANG_ID")
        isEditMode = barangId != null

        if (isEditMode) {
            supportActionBar?.title = "Edit Barang"
            loadBarangData()
        } else {
            supportActionBar?.title = "Tambah Barang"
            addVarianView() // Add one default varian
        }
    }

    private fun loadBarangData() {
        showLoading(true)
        db.collection("master_barang")
            .document(barangId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.etNamaBarang.setText(document.getString("nama_barang"))
                    loadVarianData()
                } else {
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadVarianData() {
        db.collection("barang_varian")
            .whereEqualTo("barang_id", barangId)
            .get()
            .addOnSuccessListener { documents ->
                showLoading(false)
                if (documents.isEmpty) {
                    addVarianView()
                } else {
                    documents.forEach { doc ->
                        val kemasanId = doc.getString("kemasan_id") ?: ""
                        val shelfLife = doc.getLong("shelf_life_hari")?.toInt() ?: 0
                        val hargaJual = doc.getLong("harga_jual")?.toInt() ?: 0
                        val varianId = doc.id

                        addVarianView(varianId, kemasanId, shelfLife, hargaJual)
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        binding.btnTambahVarian.setOnClickListener {
            addVarianView()
        }

        binding.btnSimpan.setOnClickListener {
            if (validateInput()) {
                saveBarang()
            }
        }
    }

    private fun addVarianView(
        varianId: String = "",
        kemasanId: String = "",
        shelfLife: Int = 0,
        hargaJual: Int = 0
    ) {
        val varianView = LayoutInflater.from(this)
            .inflate(R.layout.item_varian_form, binding.llVarianContainer, false)

        val tvVarianNumber = varianView.findViewById<TextView>(R.id.tvVarianNumber)
        val ivRemove = varianView.findViewById<ImageView>(R.id.ivRemoveVarian)
        val tilKemasan = varianView.findViewById<TextInputLayout>(R.id.tilKemasan)
        val actvKemasan = varianView.findViewById<AutoCompleteTextView>(R.id.actvKemasan)
        val etShelfLife = varianView.findViewById<TextInputEditText>(R.id.etShelfLife)
        val etHargaJual = varianView.findViewById<TextInputEditText>(R.id.etHargaJual)

        tvVarianNumber.text = "Varian ${varianViews.size + 1}"

        // Setup kemasan dropdown - pastikan kemasanList sudah terisi
        if (kemasanList.isEmpty()) {
            Log.e("BarangForm", "kemasanList is empty when adding varian view!")
            Toast.makeText(this, "Data kemasan belum dimuat", Toast.LENGTH_SHORT).show()
            return
        }

        val kemasanNames = kemasanList.map { "${it.namaKemasan} (${it.satuan})" }
        Log.d("BarangForm", "Setting up dropdown with ${kemasanNames.size} items: $kemasanNames")

        val adapterKemasan = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, kemasanNames)
        actvKemasan.setAdapter(adapterKemasan)

        // Trigger dropdown to show on click
        actvKemasan.setOnClickListener {
            actvKemasan.showDropDown()
        }

        actvKemasan.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                actvKemasan.showDropDown()
            }
        }

        // Set data if editing
        if (kemasanId.isNotEmpty()) {
            val kemasan = kemasanList.find { it.id == kemasanId }
            kemasan?.let {
                actvKemasan.setText("${it.namaKemasan} (${it.satuan})", false)
            }
        }
        if (shelfLife > 0) etShelfLife.setText(shelfLife.toString())
        if (hargaJual > 0) etHargaJual.setText(hargaJual.toString())

        // Store varian ID in tag for update
        varianView.tag = varianId

        ivRemove.setOnClickListener {
            if (varianViews.size > 1) {
                binding.llVarianContainer.removeView(varianView)
                varianViews.remove(varianView)
                updateVarianNumbers()

                // Delete from Firestore if exists
                if (varianId.isNotEmpty()) {
                    db.collection("barang_varian").document(varianId).delete()
                }
            } else {
                Toast.makeText(this, "Minimal harus ada 1 varian", Toast.LENGTH_SHORT).show()
            }
        }

        varianViews.add(varianView)
        binding.llVarianContainer.addView(varianView)
    }

    private fun updateVarianNumbers() {
        varianViews.forEachIndexed { index, view ->
            val tvNumber = view.findViewById<TextView>(R.id.tvVarianNumber)
            tvNumber.text = "Varian ${index + 1}"
        }
    }

    private fun validateInput(): Boolean {
        val namaBarang = binding.etNamaBarang.text.toString().trim()

        if (namaBarang.isEmpty()) {
            binding.tilNamaBarang.error = "Nama barang tidak boleh kosong"
            return false
        }
        binding.tilNamaBarang.error = null

        if (varianViews.isEmpty()) {
            Toast.makeText(this, "Minimal harus ada 1 varian", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate each varian
        for (varianView in varianViews) {
            val actvKemasan = varianView.findViewById<AutoCompleteTextView>(R.id.actvKemasan)
            val etShelfLife = varianView.findViewById<TextInputEditText>(R.id.etShelfLife)
            val etHargaJual = varianView.findViewById<TextInputEditText>(R.id.etHargaJual)

            if (actvKemasan.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Kemasan tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            if (etShelfLife.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Shelf life tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            if (etHargaJual.text.toString().trim().isEmpty()) {
                Toast.makeText(this, "Harga jual tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun saveBarang() {
        showLoading(true)

        val namaBarang = binding.etNamaBarang.text.toString().trim()
        val barangData = hashMapOf(
            "nama_barang" to namaBarang,
            "updated_at" to System.currentTimeMillis()
        )

        if (isEditMode) {
            // Update barang
            db.collection("master_barang")
                .document(barangId!!)
                .update(barangData as Map<String, Any>)
                .addOnSuccessListener {
                    saveAllVarian(barangId!!)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Add new barang
            barangData["created_at"] = System.currentTimeMillis()

            db.collection("master_barang")
                .add(barangData)
                .addOnSuccessListener { documentReference ->
                    saveAllVarian(documentReference.id)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveAllVarian(barangId: String) {
        val batch = db.batch()

        varianViews.forEach { varianView ->
            val varianId = varianView.tag as? String
            val actvKemasan = varianView.findViewById<AutoCompleteTextView>(R.id.actvKemasan)
            val etShelfLife = varianView.findViewById<TextInputEditText>(R.id.etShelfLife)
            val etHargaJual = varianView.findViewById<TextInputEditText>(R.id.etHargaJual)

            val kemasanName = actvKemasan.text.toString().trim()
            val kemasan = kemasanMap[kemasanName]

            if (kemasan == null) {
                showLoading(false)
                Toast.makeText(this, "Kemasan '$kemasanName' tidak ditemukan", Toast.LENGTH_SHORT).show()
                return
            }

            val shelfLife = etShelfLife.text.toString().toIntOrNull() ?: 0
            val hargaJual = etHargaJual.text.toString().toIntOrNull() ?: 0

            val varianData = hashMapOf(
                "barang_id" to barangId,
                "kemasan_id" to kemasan.id, // Menyimpan doc_id (contoh: j0Svn0gZ1lla8b0De6HP)
                "shelf_life_hari" to shelfLife,
                "harga_jual" to hargaJual,
                "updated_at" to System.currentTimeMillis()
            )

            if (!varianId.isNullOrEmpty()) {
                // Update existing varian
                val docRef = db.collection("barang_varian").document(varianId)
                batch.update(docRef, varianData as Map<String, Any>)
            } else {
                // Add new varian
                varianData["created_at"] = System.currentTimeMillis()
                val docRef = db.collection("barang_varian").document()
                batch.set(docRef, varianData)
            }
        }

        batch.commit()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Data berhasil disimpan", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !show
        binding.btnTambahVarian.isEnabled = !show
    }
}