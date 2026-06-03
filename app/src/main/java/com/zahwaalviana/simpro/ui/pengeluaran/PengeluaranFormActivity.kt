package com.zahwaalviana.simpro.ui.pengeluaran

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.RelatedItem
import com.zahwaalviana.simpro.databinding.ActivityPengeluaranFormBinding
import com.zahwaalviana.simpro.databinding.DialogSelectRelatedBinding
import com.zahwaalviana.simpro.ui.pengeluaran.adapter.RelatedItemSelectAdapter
import java.text.SimpleDateFormat
import java.util.*

class PengeluaranFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPengeluaranFormBinding
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val masterNamaList = mutableListOf<String>()
    private val masterMap = mutableMapOf<String, String>()

    private val allRelatedItems = mutableListOf<RelatedItem>()
    private val selectedRelatedItems = mutableListOf<RelatedItem>()

    private var selectedMasterId: String? = null
    private var pengeluaranId: String? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

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
        val totalToLoad = 3 // Master Pengeluaran, Barang, Kemasan

        fun checkComplete() {
            loadedCount++
            if (loadedCount == totalToLoad) {
                showLoading(false)
                checkEditMode()
            }
        }

        allRelatedItems.clear()

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

        // 2. Load Barang (Bahan Baku)
        db.collection("master_barang").get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach {
                val nama = it.getString("nama_barang") ?: return@forEach
                allRelatedItems.add(RelatedItem(it.id, "barang", "Bahan Baku: $nama"))
            }
            checkComplete()
        }

        // 3. Load Kemasan
        db.collection("master_kemasan").get().addOnSuccessListener { snapshot ->
            snapshot.documents.forEach {
                val nama = it.getString("nama_kemasan") ?: return@forEach
                allRelatedItems.add(RelatedItem(it.id, "kemasan", "Kemasan: $nama"))
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
            
            // Load related items
            val itemsData = doc.get("relatedItems") as? List<Map<String, Any>>
            selectedRelatedItems.clear()
            itemsData?.forEach { map ->
                selectedRelatedItems.add(RelatedItem(
                    id = map["id"] as? String ?: "",
                    type = map["type"] as? String ?: "",
                    name = map["name"] as? String ?: ""
                ))
            }

            // Pre-select master pengeluaran
            masterMap.entries.find { it.value == selectedMasterId }?.let {
                binding.actvMasterPengeluaran.setText(it.key, false)
            }
            
            updateChips()
        }
    }

    private fun setupListener() {
        binding.btnPilihTerkait.setOnClickListener {
            showMultiSelectDialog()
        }
        
        // Update button text to be more specific
        binding.btnPilihTerkait.text = "Pilih Bahan Baku atau Kemasan"

        binding.btnSimpan.setOnClickListener {
            if (!validate()) return@setOnClickListener
            savePengeluaran()
        }
    }

    private fun showMultiSelectDialog() {
        val dialogBinding = DialogSelectRelatedBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        val tempSelected = selectedRelatedItems.toMutableList()
        
        val adapter = RelatedItemSelectAdapter(allRelatedItems, tempSelected)
        dialogBinding.rvSelectRelated.layoutManager = LinearLayoutManager(this)
        dialogBinding.rvSelectRelated.adapter = adapter

        dialogBinding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                adapter.filter(s.toString())
            }
        })

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnDone.setOnClickListener {
            selectedRelatedItems.clear()
            selectedRelatedItems.addAll(tempSelected)
            updateChips()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateChips() {
        binding.chipGroupTerkait.removeAllViews()
        selectedRelatedItems.forEach { item ->
            val chip = Chip(this).apply {
                text = item.name
                isCloseIconVisible = true
                setOnCloseIconClickListener {
                    selectedRelatedItems.removeAll { it.id == item.id }
                    updateChips()
                }
            }
            binding.chipGroupTerkait.addView(chip)
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

        val data = mutableMapOf<String, Any?>(
            "master_pengeluaran_id" to selectedMasterId!!,
            "relatedItems" to selectedRelatedItems.map { 
                mapOf("id" to it.id, "type" to it.type, "name" to it.name) 
            },
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
        binding.btnPilihTerkait.isEnabled = !show
    }

    private fun toast(msg: String?) {
        Toast.makeText(this, msg ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
    }
}
