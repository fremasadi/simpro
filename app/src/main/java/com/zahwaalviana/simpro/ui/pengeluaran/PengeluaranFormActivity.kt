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
import java.util.Calendar
import java.util.Locale

class PengeluaranFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPengeluaranFormBinding
    private val db by lazy { FirebaseFirestore.getInstance() }

    private val masterNamaList = mutableListOf<String>()
    private val masterMap = mutableMapOf<String, String>()
    // nama -> master_pengeluaran_id

    private var selectedMasterId: String? = null
    private var produksiId: String = ""
    private var pengeluaranId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPengeluaranFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        produksiId = intent.getStringExtra("PRODUKSI_ID") ?: ""
        pengeluaranId = intent.getStringExtra("PENGELUARAN_ID")

        setupToolbar()
        setupTanggalPicker()
        loadMasterPengeluaran()
        setupListener()
        checkEditMode()
    }

    // ---------------- TOOLBAR ----------------
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    // ---------------- DATE PICKER ----------------
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

    // ---------------- LOAD MASTER PENGELUARAN ----------------
    private fun loadMasterPengeluaran() {
        db.collection("master_pengeluaran")
            .get()
            .addOnSuccessListener { snapshot ->
                masterNamaList.clear()
                masterMap.clear()

                snapshot.documents.forEach {
                    val nama = it.getString("nama_pengeluaran") ?: return@forEach
                    masterNamaList.add(nama)
                    masterMap[nama] = it.id
                }

                val adapter = ArrayAdapter(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    masterNamaList
                )
                binding.actvMasterPengeluaran.setAdapter(adapter)

                binding.actvMasterPengeluaran.setOnItemClickListener { _, _, position, _ ->
                    val nama = masterNamaList[position]
                    selectedMasterId = masterMap[nama]
                }
            }
            .addOnFailureListener {
                toast(it.message)
            }
    }

    // ---------------- EDIT MODE ----------------
    private fun checkEditMode() {
        if (pengeluaranId == null) return

        supportActionBar?.title = "Edit Pengeluaran"
        showLoading(true)

        db.collection("pengeluaran")
            .document(pengeluaranId!!)
            .get()
            .addOnSuccessListener { doc ->
                showLoading(false)
                if (!doc.exists()) return@addOnSuccessListener

                binding.etTanggal.setText(doc.getString("tanggal"))
                binding.etBiaya.setText(doc.getLong("biaya")?.toString() ?: "")
                binding.etKeterangan.setText(doc.getString("keterangan"))

                selectedMasterId = doc.getString("master_pengeluaran_id")

                // set nama master ke dropdown
                masterMap.entries.find { it.value == selectedMasterId }?.let {
                    binding.actvMasterPengeluaran.setText(it.key, false)
                }
            }
            .addOnFailureListener {
                showLoading(false)
                toast(it.message)
            }
    }

    // ---------------- SAVE ----------------
    private fun setupListener() {
        binding.btnSimpan.setOnClickListener {
            if (!validate()) return@setOnClickListener
            savePengeluaran()
        }
    }

    private fun validate(): Boolean {
        when {
            selectedMasterId == null -> {
                toast("Pilih jenis pengeluaran")
                return false
            }
            binding.etTanggal.text.isNullOrEmpty() -> {
                toast("Tanggal wajib diisi")
                return false
            }
            binding.etBiaya.text.isNullOrEmpty() -> {
                toast("Biaya wajib diisi")
                return false
            }
        }
        return true
    }

    private fun savePengeluaran() {
        showLoading(true)

        val data = mutableMapOf<String, Any>(
            "master_pengeluaran_id" to selectedMasterId!!,
            "produksi_id" to produksiId,
            "tanggal" to binding.etTanggal.text.toString(),
            "keterangan" to binding.etKeterangan.text.toString(),
            "biaya" to binding.etBiaya.text.toString().toInt(),
            "updated_at" to System.currentTimeMillis()
        )

        if (pengeluaranId == null) {
            data["created_at"] = System.currentTimeMillis()

            db.collection("pengeluaran")
                .add(data)
                .addOnSuccessListener {
                    showLoading(false)
                    toast("Pengeluaran berhasil ditambahkan")
                    finish()
                }
                .addOnFailureListener {
                    showLoading(false)
                    toast(it.message)
                }

        } else {
            db.collection("pengeluaran")
                .document(pengeluaranId!!)
                .update(data)
                .addOnSuccessListener {
                    showLoading(false)
                    toast("Pengeluaran berhasil diupdate")
                    finish()
                }
                .addOnFailureListener {
                    showLoading(false)
                    toast(it.message)
                }
        }
    }

    // ---------------- UTIL ----------------
    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !show
    }

    private fun toast(msg: String?) {
        Toast.makeText(this, msg ?: "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
    }
}