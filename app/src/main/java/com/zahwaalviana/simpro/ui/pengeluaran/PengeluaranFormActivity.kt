package com.zahwaalviana.simpro.ui.pengeluaran

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.databinding.ActivityPengeluaranFormBinding
import java.text.SimpleDateFormat
import java.util.*

class PengeluaranFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPengeluaranFormBinding
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val masterNamaList = mutableListOf<String>()
    private val masterMap = mutableMapOf<String, String>()

    private val produksiNamaList = mutableListOf<String>()
    private val produksiMap = mutableMapOf<String, String>()

    private val barangNamaList = mutableListOf<String>()
    private val barangMap = mutableMapOf<String, String>()

    private val kemasanNamaList = mutableListOf<String>()
    private val kemasanMap = mutableMapOf<String, String>()

    private var selectedMasterId: String? = null
    private var selectedProduksiId: String? = null
    private var selectedBarangId: String? = null
    private var selectedKemasanId: String? = null

    private var pengeluaranId: String? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPengeluaranFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        pengeluaranId = intent.getStringExtra("PENGELUARAN_ID")

        setupToolbar()
        setupTanggalPicker()
        loadAllMasterData()
        setupListener()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupTanggalPicker() {
        binding.etTanggal.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selected = Calendar.getInstance().apply {
                        set(year, month, day)
                    }
                    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    binding.etTanggal.setText(format.format(selected.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun loadAllMasterData() {
        showLoading(true)
        var loadedCount = 0
        val totalToLoad = 4

        fun checkComplete() {
            loadedCount++
            if (loadedCount == totalToLoad) {
                showLoading(false)
                checkEditMode()
            }
        }

        // 1. Load Master Pengeluaran
        db.collection("master_pengeluaran").get().addOnSuccessListener { snapshot ->
            masterNamaList.clear()
            masterMap.clear()
            snapshot.documents.forEach {
                val nama = it.getString("nama_pengeluaran") ?: return@forEach
                masterNamaList.add(nama)
                masterMap[nama] = it.id
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, masterNamaList)
            binding.actvMasterPengeluaran.setAdapter(adapter)
            binding.actvMasterPengeluaran.setOnItemClickListener { _, _, position, _ ->
                selectedMasterId = masterMap[masterNamaList[position]]
            }
            checkComplete()
        }

        // 2. Load Produksi
        db.collection("produksi").get().addOnSuccessListener { snapshot ->
            produksiNamaList.clear()
            produksiMap.clear()
            snapshot.documents.forEach { doc ->
                val tglMs = doc.getLong("tanggal_produksi") ?: 0L
                val dateStr = sdf.format(Date(tglMs))
                val display = "Produksi - $dateStr"
                produksiNamaList.add(display)
                produksiMap[display] = doc.id
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, produksiNamaList)
            binding.actvProduksi.setAdapter(adapter)
            binding.actvProduksi.setOnItemClickListener { _, _, position, _ ->
                selectedProduksiId = produksiMap[produksiNamaList[position]]
            }
            checkComplete()
        }

        // 3. Load Barang
        db.collection("master_barang").get().addOnSuccessListener { snapshot ->
            barangNamaList.clear()
            barangMap.clear()
            snapshot.documents.forEach {
                val nama = it.getString("nama_barang") ?: return@forEach
                barangNamaList.add(nama)
                barangMap[nama] = it.id
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, barangNamaList)
            binding.actvBarang.setAdapter(adapter)
            binding.actvBarang.setOnItemClickListener { _, _, position, _ ->
                selectedBarangId = barangMap[barangNamaList[position]]
            }
            checkComplete()
        }

        // 4. Load Kemasan
        db.collection("master_kemasan").get().addOnSuccessListener { snapshot ->
            kemasanNamaList.clear()
            kemasanMap.clear()
            snapshot.documents.forEach {
                val nama = it.getString("nama_kemasan") ?: return@forEach
                kemasanNamaList.add(nama)
                kemasanMap[nama] = it.id
            }
            val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, kemasanNamaList)
            binding.actvKemasan.setAdapter(adapter)
            binding.actvKemasan.setOnItemClickListener { _, _, position, _ ->
                selectedKemasanId = kemasanMap[kemasanNamaList[position]]
            }
            checkComplete()
        }
    }

    private fun checkEditMode() {
        if (pengeluaranId == null) return

        supportActionBar?.title = "Edit Pengeluaran"
        showLoading(true)

        db.collection("pengeluaran").document(pengeluaranId!!).get().addOnSuccessListener { doc ->
            showLoading(false)
            if (!doc.exists()) return@addOnSuccessListener

            binding.etTanggal.setText(doc.getString("tanggal"))
            binding.etBiaya.setText(doc.getLong("biaya")?.toString() ?: "")
            binding.etKeterangan.setText(doc.getString("keterangan"))

            selectedMasterId = doc.getString("master_pengeluaran_id")
            selectedProduksiId = doc.getString("produksi_id")
            selectedBarangId = doc.getString("barang_id")
            selectedKemasanId = doc.getString("kemasan_id")

            // Pre-select items in dropdowns
            masterMap.entries.find { it.value == selectedMasterId }?.let {
                binding.actvMasterPengeluaran.setText(it.key, false)
            }
            produksiMap.entries.find { it.value == selectedProduksiId }?.let {
                binding.actvProduksi.setText(it.key, false)
            }
            barangMap.entries.find { it.value == selectedBarangId }?.let {
                binding.actvBarang.setText(it.key, false)
            }
            kemasanMap.entries.find { it.value == selectedKemasanId }?.let {
                binding.actvKemasan.setText(it.key, false)
            }
        }
    }

    private fun setupListener() {
        binding.btnSimpan.setOnClickListener {
            if (!validate()) return@setOnClickListener
            savePengeluaran()
        }
    }

    private fun validate(): Boolean {
        if (selectedMasterId == null) {
            toast("Pilih jenis pengeluaran")
            return false
        }
        if (binding.etTanggal.text.isNullOrEmpty()) {
            toast("Tanggal wajib diisi")
            return false
        }
        if (binding.etBiaya.text.isNullOrEmpty()) {
            toast("Biaya wajib diisi")
            return false
        }
        return true
    }

    private fun savePengeluaran() {
        showLoading(true)

        // Reset IDs if the text is empty (user cleared it)
        if (binding.actvProduksi.text.isEmpty()) selectedProduksiId = null
        if (binding.actvBarang.text.isEmpty()) selectedBarangId = null
        if (binding.actvKemasan.text.isEmpty()) selectedKemasanId = null

        val data = mutableMapOf<String, Any?>(
            "master_pengeluaran_id" to selectedMasterId!!,
            "produksi_id" to selectedProduksiId,
            "barang_id" to selectedBarangId,
            "kemasan_id" to selectedKemasanId,
            "tanggal" to binding.etTanggal.text.toString(),
            "keterangan" to binding.etKeterangan.text.toString(),
            "biaya" to binding.etBiaya.text.toString().toInt(),
            "updated_at" to System.currentTimeMillis()
        )

        val task = if (pengeluaranId == null) {
            data["created_at"] = System.currentTimeMillis()
            db.collection("pengeluaran").add(data)
        } else {
            db.collection("pengeluaran").document(pengeluaranId!!).update(data)
        }

        task.addOnSuccessListener {
            showLoading(false)
            toast("Pengeluaran berhasil disimpan")
            finish()
        }.addOnFailureListener {
            showLoading(false)
            toast(it.message)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !show
    }

    private fun toast(msg: String?) {
        Toast.makeText(this, msg ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
    }
}
