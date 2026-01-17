package com.zahwaalviana.simpro.ui.produksi

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.databinding.ActivityProduksiFormBinding
import java.text.SimpleDateFormat
import java.util.*

class ProduksiFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProduksiFormBinding
    private lateinit var db: FirebaseFirestore
    private var produksiId: String? = null
    private var isEditMode = false

    private val itemViews = mutableListOf<View>()
    private val varianList = mutableListOf<BarangVarian>()
    private var varianMap = mutableMapOf<String, BarangVarian>()
    private val mandorList = mutableListOf<Pair<String, String>>() // (id, name)
    private var mandorMap = mutableMapOf<String, String>() // name -> id

    private var selectedDate: Calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProduksiFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupListeners()

        // Load data sequentially
        loadMandorData {
            loadVarianData {
                checkEditMode()
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun loadMandorData(onComplete: () -> Unit) {
        showLoading(true)
        db.collection("users")
            .whereEqualTo("role", "mandor")
            .get()
            .addOnSuccessListener { documents ->
                mandorList.clear()
                mandorMap.clear()

                if (documents.isEmpty) {
                    showLoading(false)
                    Toast.makeText(this, "Belum ada data mandor", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                documents.forEach { doc ->
                    val id = doc.id
                    val name = doc.getString("name") ?: ""
                    mandorList.add(Pair(id, name))
                    mandorMap[name] = id
                }

                setupMandorDropdown()
                showLoading(false)
                onComplete()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
    }

    private fun setupMandorDropdown() {
        val mandorNames = mandorList.map { it.second }
        val adapterMandor = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, mandorNames)
        binding.actvMandor.setAdapter(adapterMandor)

        binding.actvMandor.setOnClickListener {
            binding.actvMandor.showDropDown()
        }
    }

    private fun loadVarianData(onComplete: () -> Unit) {
        showLoading(true)
        db.collection("barang_varian")
            .get()
            .addOnSuccessListener { varianDocs ->
                varianList.clear()
                varianMap.clear()

                val totalVarian = varianDocs.size()
                var processedVarian = 0

                if (totalVarian == 0) {
                    showLoading(false)
                    Toast.makeText(this, "Belum ada data barang varian", Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }

                varianDocs.documents.forEach { varianDoc ->
                    val barangId = varianDoc.getString("barang_id") ?: ""
                    val kemasanId = varianDoc.getString("kemasan_id") ?: ""

                    loadBarangAndKemasan(barangId, kemasanId) { barangNama, kemasanNama, kemasanSatuan ->
                        val varian = BarangVarian(
                            id = varianDoc.id,
                            barangId = barangId,
                            kemasanId = kemasanId,
                            kemasanNama = kemasanNama,
                            kemasanSatuan = kemasanSatuan,
                            shelfLifeHari = varianDoc.getLong("shelf_life_hari")?.toInt() ?: 0,
                            hargaJual = varianDoc.getLong("harga_jual")?.toInt() ?: 0
                        )

                        val displayText = "$barangNama - $kemasanNama ($kemasanSatuan)"
                        varianList.add(varian)
                        varianMap[displayText] = varian

                        processedVarian++
                        if (processedVarian == totalVarian) {
                            showLoading(false)
                            onComplete()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadBarangAndKemasan(
        barangId: String,
        kemasanId: String,
        callback: (String, String, String) -> Unit
    ) {
        var barangNama = ""
        var kemasanNama = ""
        var kemasanSatuan = ""
        var completed = 0

        db.collection("master_barang").document(barangId)
            .get()
            .addOnSuccessListener { doc ->
                barangNama = doc.getString("nama_barang") ?: ""
                completed++
                if (completed == 2) callback(barangNama, kemasanNama, kemasanSatuan)
            }

        db.collection("master_kemasan").document(kemasanId)
            .get()
            .addOnSuccessListener { doc ->
                kemasanNama = doc.getString("nama_kemasan") ?: ""
                kemasanSatuan = doc.getString("satuan") ?: ""
                completed++
                if (completed == 2) callback(barangNama, kemasanNama, kemasanSatuan)
            }
    }

    private fun checkEditMode() {
        produksiId = intent.getStringExtra("PRODUKSI_ID")
        isEditMode = produksiId != null

        if (isEditMode) {
            supportActionBar?.title = "Edit Produksi"
            loadProduksiData()
        } else {
            supportActionBar?.title = "Tambah Produksi"
            // Set today as default date
            binding.etTanggalProduksi.setText(dateFormat.format(selectedDate.time))
            addItemView()
        }
    }

    private fun loadProduksiData() {
        showLoading(true)
        db.collection("produksi")
            .document(produksiId!!)
            .get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val tanggal = doc.getLong("tanggal_produksi") ?: 0L
                    selectedDate.timeInMillis = tanggal
                    binding.etTanggalProduksi.setText(dateFormat.format(Date(tanggal)))

                    val mandorId = doc.getString("mandor_id") ?: ""
                    val mandorName = mandorList.find { it.first == mandorId }?.second ?: ""
                    binding.actvMandor.setText(mandorName, false)

                    binding.etTotalBiaya.setText(doc.getLong("total_biaya_produksi")?.toString() ?: "0")

                    loadItemsData()
                } else {
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadItemsData() {
        db.collection("produksi_items")
            .whereEqualTo("produksi_id", produksiId)
            .get()
            .addOnSuccessListener { docs ->
                showLoading(false)
                if (docs.isEmpty) {
                    addItemView()
                } else {
                    docs.documents.forEach { doc ->
                        val varianId = doc.getString("varian_id") ?: ""
                        val jumlah = doc.getLong("jumlah_produksi")?.toInt() ?: 0
                        val itemId = doc.id

                        addItemView(itemId, varianId, jumlah)
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupListeners() {
        binding.etTanggalProduksi.setOnClickListener {
            showDatePicker()
        }

        binding.tilTanggalProduksi.setEndIconOnClickListener {
            showDatePicker()
        }

        binding.btnTambahItem.setOnClickListener {
            addItemView()
        }

        binding.btnSimpan.setOnClickListener {
            if (validateInput()) {
                saveProduksi()
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                selectedDate.set(year, month, day)
                binding.etTanggalProduksi.setText(dateFormat.format(selectedDate.time))

                // Update expired date for all items
                updateAllExpiredDates()
            },
            selectedDate.get(Calendar.YEAR),
            selectedDate.get(Calendar.MONTH),
            selectedDate.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun addItemView(itemId: String = "", varianId: String = "", jumlah: Int = 0) {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_produksi_item_form, binding.llItemContainer, false)

        val tvItemNumber = itemView.findViewById<TextView>(R.id.tvItemNumber)
        val ivRemove = itemView.findViewById<ImageView>(R.id.ivRemoveItem)
        val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
        val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlahProduksi)
        val tvExpired = itemView.findViewById<TextView>(R.id.tvExpiredInfo)

        tvItemNumber.text = "Item ${itemViews.size + 1}"

        // Setup varian dropdown
        val varianNames = varianMap.keys.toList()
        val adapterVarian = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, varianNames)
        actvVarian.setAdapter(adapterVarian)

        actvVarian.setOnClickListener {
            actvVarian.showDropDown()
        }

        // Update expired date when varian is selected
        actvVarian.setOnItemClickListener { _, _, _, _ ->
            updateExpiredDate(itemView)
        }

        // Set data if editing
        if (varianId.isNotEmpty()) {
            val varianEntry = varianMap.entries.find { it.value.id == varianId }
            varianEntry?.let {
                actvVarian.setText(it.key, false)
                updateExpiredDate(itemView)
            }
        }
        if (jumlah > 0) etJumlah.setText(jumlah.toString())

        // Store item ID in tag
        itemView.tag = itemId

        ivRemove.setOnClickListener {
            if (itemViews.size > 1) {
                binding.llItemContainer.removeView(itemView)
                itemViews.remove(itemView)
                updateItemNumbers()

                if (itemId.isNotEmpty()) {
                    db.collection("produksi_items").document(itemId).delete()
                }
            } else {
                Toast.makeText(this, "Minimal harus ada 1 item", Toast.LENGTH_SHORT).show()
            }
        }

        itemViews.add(itemView)
        binding.llItemContainer.addView(itemView)
    }

    private fun updateExpiredDate(itemView: View) {
        val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
        val tvExpired = itemView.findViewById<TextView>(R.id.tvExpiredInfo)

        val varianText = actvVarian.text.toString()
        val varian = varianMap[varianText]

        if (varian != null) {
            val expiredCal = Calendar.getInstance()
            expiredCal.timeInMillis = selectedDate.timeInMillis
            expiredCal.add(Calendar.DAY_OF_YEAR, varian.shelfLifeHari)

            tvExpired.text = "Expired: ${dateFormat.format(expiredCal.time)} (${varian.shelfLifeHari} hari)"
            tvExpired.setTextColor(android.graphics.Color.parseColor("#E59BA6"))
        } else {
            tvExpired.text = "Expired: -"
            tvExpired.setTextColor(android.graphics.Color.parseColor("#757575"))
        }
    }

    private fun updateAllExpiredDates() {
        itemViews.forEach { itemView ->
            updateExpiredDate(itemView)
        }
    }

    private fun updateItemNumbers() {
        itemViews.forEachIndexed { index, view ->
            val tvNumber = view.findViewById<TextView>(R.id.tvItemNumber)
            tvNumber.text = "Item ${index + 1}"
        }
    }

    private fun validateInput(): Boolean {
        if (binding.etTanggalProduksi.text.toString().isEmpty()) {
            Toast.makeText(this, "Tanggal produksi harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.actvMandor.text.toString().isEmpty()) {
            Toast.makeText(this, "Mandor harus dipilih", Toast.LENGTH_SHORT).show()
            return false
        }

        if (binding.etTotalBiaya.text.toString().isEmpty()) {
            Toast.makeText(this, "Total biaya harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }

        if (itemViews.isEmpty()) {
            Toast.makeText(this, "Minimal harus ada 1 item produksi", Toast.LENGTH_SHORT).show()
            return false
        }

        // Validate each item
        for (itemView in itemViews) {
            val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
            val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlahProduksi)

            if (actvVarian.text.toString().isEmpty()) {
                Toast.makeText(this, "Varian barang harus dipilih", Toast.LENGTH_SHORT).show()
                return false
            }
            if (etJumlah.text.toString().isEmpty()) {
                Toast.makeText(this, "Jumlah produksi harus diisi", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        return true
    }

    private fun saveProduksi() {
        showLoading(true)

        val mandorName = binding.actvMandor.text.toString()
        val mandorId = mandorMap[mandorName] ?: ""
        val totalBiaya = binding.etTotalBiaya.text.toString().toIntOrNull() ?: 0

        val produksiData = hashMapOf(
            "tanggal_produksi" to selectedDate.timeInMillis,
            "mandor_id" to mandorId,
            "total_biaya_produksi" to totalBiaya,
            "updated_at" to System.currentTimeMillis()
        )

        if (isEditMode) {
            db.collection("produksi")
                .document(produksiId!!)
                .update(produksiData as Map<String, Any>)
                .addOnSuccessListener {
                    saveAllItems(produksiId!!)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            produksiData["created_at"] = System.currentTimeMillis()

            db.collection("produksi")
                .add(produksiData)
                .addOnSuccessListener { docRef ->
                    saveAllItems(docRef.id)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveAllItems(produksiId: String) {
        val batch = db.batch()

        itemViews.forEach { itemView ->
            val itemId = itemView.tag as? String
            val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
            val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlahProduksi)

            val varianText = actvVarian.text.toString()
            val varian = varianMap[varianText]
            val jumlah = etJumlah.text.toString().toIntOrNull() ?: 0

            if (varian != null) {
                // Calculate expired_at = tanggal_produksi + shelf_life_hari
                val expiredCal = Calendar.getInstance()
                expiredCal.timeInMillis = selectedDate.timeInMillis
                expiredCal.add(Calendar.DAY_OF_YEAR, varian.shelfLifeHari)

                val itemData = hashMapOf(
                    "produksi_id" to produksiId,
                    "varian_id" to varian.id,
                    "jumlah_produksi" to jumlah,
                    "expired_at" to expiredCal.timeInMillis,
                    "updated_at" to System.currentTimeMillis()
                )

                if (!itemId.isNullOrEmpty()) {
                    val docRef = db.collection("produksi_items").document(itemId)
                    batch.update(docRef, itemData as Map<String, Any>)
                } else {
                    itemData["created_at"] = System.currentTimeMillis()
                    val docRef = db.collection("produksi_items").document()
                    batch.set(docRef, itemData)
                }
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
        binding.btnTambahItem.isEnabled = !show
    }
}