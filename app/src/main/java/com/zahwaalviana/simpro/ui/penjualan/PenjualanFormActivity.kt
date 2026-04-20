package com.zahwaalviana.simpro.ui.penjualan

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.databinding.ActivityPenjualanFormBinding
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class PenjualanFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPenjualanFormBinding
    private lateinit var db: FirebaseFirestore

    private val itemViews = mutableListOf<View>()
    private val varianList = mutableListOf<BarangVarian>()
    private var varianMap = mutableMapOf<String, BarangVarian>()

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val numberFormat = NumberFormat.getNumberInstance(Locale("id", "ID"))
    private var totalHarga = 0
    private var isFormattingBayar = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPenjualanFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupListeners()
        loadVarianData {
            addItemView()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupListeners() {
        binding.btnTambahItem.setOnClickListener {
            addItemView()
        }

        binding.btnSimpan.setOnClickListener {
            if (validateInput()) {
                savePenjualan()
            }
        }

        binding.etBayar.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isFormattingBayar) return
                isFormattingBayar = true
                val digits = s.toString().replace(".", "")
                val number = digits.toLongOrNull()
                if (number != null) {
                    val formatted = numberFormat.format(number)
                    s?.replace(0, s.length, formatted)
                } else if (digits.isEmpty()) {
                    s?.clear()
                }
                isFormattingBayar = false
                updateKembalian()
            }
        })
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
                            hargaJual = varianDoc.getLong("harga_jual")?.toInt() ?: 0,
                            stok = varianDoc.getLong("stok")?.toInt() ?: 0
                        )

                        val displayText = "$barangNama - $kemasanNama"
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

    private fun addItemView() {
        val itemView = LayoutInflater.from(this)
            .inflate(R.layout.item_penjualan_item_form, binding.llItemContainer, false)

        val tvItemNumber = itemView.findViewById<TextView>(R.id.tvItemNumber)
        val ivRemove = itemView.findViewById<ImageView>(R.id.ivRemoveItem)
        val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
        val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlah)
        val tvStokInfo = itemView.findViewById<TextView>(R.id.tvStokInfo)
        val tvHargaLabel = itemView.findViewById<TextView>(R.id.tvHargaLabel)

        tvItemNumber.text = "Item ${itemViews.size + 1}"

        val varianNames = varianMap.keys.toList()
        val adapterVarian = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, varianNames)
        actvVarian.setAdapter(adapterVarian)

        actvVarian.setOnClickListener {
            actvVarian.showDropDown()
        }

        actvVarian.setOnItemClickListener { _, _, _, _ ->
            val varianText = actvVarian.text.toString()
            val varian = varianMap[varianText]
            if (varian != null) {
                tvStokInfo.text = "Stok: ${varian.stok} ${varian.kemasanSatuan}"
                tvHargaLabel.text = "Harga: ${currencyFormat.format(varian.hargaJual)}"
                updateItemSubtotal(itemView)
            }
        }

        etJumlah.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateItemSubtotal(itemView)
            }
        })

        ivRemove.setOnClickListener {
            if (itemViews.size > 1) {
                binding.llItemContainer.removeView(itemView)
                itemViews.remove(itemView)
                updateItemNumbers()
                updateTotalHarga()
            } else {
                Toast.makeText(this, "Minimal harus ada 1 item", Toast.LENGTH_SHORT).show()
            }
        }

        itemViews.add(itemView)
        binding.llItemContainer.addView(itemView)
    }

    private fun updateItemSubtotal(itemView: View) {
        val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
        val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlah)
        val tvSubtotal = itemView.findViewById<TextView>(R.id.tvSubtotal)

        val varianText = actvVarian.text.toString()
        val varian = varianMap[varianText]
        val jumlah = etJumlah.text.toString().toIntOrNull() ?: 0

        if (varian != null && jumlah > 0) {
            val subtotal = varian.hargaJual * jumlah
            tvSubtotal.text = "Subtotal: ${currencyFormat.format(subtotal)}"
        } else {
            tvSubtotal.text = "Subtotal: Rp0"
        }

        updateTotalHarga()
    }

    private fun updateTotalHarga() {
        totalHarga = 0
        itemViews.forEach { itemView ->
            val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
            val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlah)

            val varian = varianMap[actvVarian.text.toString()]
            val jumlah = etJumlah.text.toString().toIntOrNull() ?: 0

            if (varian != null) {
                totalHarga += varian.hargaJual * jumlah
            }
        }

        binding.tvTotalHarga.text = currencyFormat.format(totalHarga)
        updateKembalian()
    }

    private fun updateKembalian() {
        val bayar = binding.etBayar.text.toString().replace(".", "").toIntOrNull() ?: 0
        val kembalian = bayar - totalHarga
        binding.tvKembalian.text = currencyFormat.format(if (kembalian > 0) kembalian else 0)

        if (bayar > 0 && kembalian < 0) {
            binding.tvKembalian.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
        } else {
            binding.tvKembalian.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
        }
    }

    private fun updateItemNumbers() {
        itemViews.forEachIndexed { index, view ->
            val tvNumber = view.findViewById<TextView>(R.id.tvItemNumber)
            tvNumber.text = "Item ${index + 1}"
        }
    }

    private fun validateInput(): Boolean {
        if (itemViews.isEmpty()) {
            Toast.makeText(this, "Minimal harus ada 1 item", Toast.LENGTH_SHORT).show()
            return false
        }

        for (itemView in itemViews) {
            val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
            val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlah)

            if (actvVarian.text.toString().isEmpty()) {
                Toast.makeText(this, "Barang harus dipilih", Toast.LENGTH_SHORT).show()
                return false
            }

            val jumlah = etJumlah.text.toString().toIntOrNull() ?: 0
            if (jumlah <= 0) {
                Toast.makeText(this, "Jumlah harus diisi", Toast.LENGTH_SHORT).show()
                return false
            }

            val varian = varianMap[actvVarian.text.toString()]
            if (varian != null && jumlah > varian.stok) {
                Toast.makeText(this, "Jumlah melebihi stok ${varian.kemasanNama} (${varian.stok})", Toast.LENGTH_SHORT).show()
                return false
            }
        }

        if (totalHarga <= 0) {
            Toast.makeText(this, "Total harga harus lebih dari 0", Toast.LENGTH_SHORT).show()
            return false
        }

        val bayar = binding.etBayar.text.toString().replace(".", "").toIntOrNull() ?: 0
        if (bayar <= 0) {
            Toast.makeText(this, "Total bayar harus diisi", Toast.LENGTH_SHORT).show()
            return false
        }

        if (bayar < totalHarga) {
            Toast.makeText(this, "Bayar kurang dari total harga", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun savePenjualan() {
        showLoading(true)

        val bayar = binding.etBayar.text.toString().replace(".", "").toIntOrNull() ?: 0
        val kembalian = bayar - totalHarga

        val penjualanData = hashMapOf(
            "tanggal" to Calendar.getInstance().timeInMillis,
            "total_harga" to totalHarga,
            "total_bayar" to bayar,
            "kembalian" to kembalian,
            "created_at" to System.currentTimeMillis()
        )

        db.collection("penjualan")
            .add(penjualanData)
            .addOnSuccessListener { docRef ->
                saveAllItems(docRef.id)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAllItems(penjualanId: String) {
        val batch = db.batch()

        itemViews.forEach { itemView ->
            val actvVarian = itemView.findViewById<AutoCompleteTextView>(R.id.actvVarian)
            val etJumlah = itemView.findViewById<TextInputEditText>(R.id.etJumlah)

            val varianText = actvVarian.text.toString()
            val varian = varianMap[varianText]
            val jumlah = etJumlah.text.toString().toIntOrNull() ?: 0

            if (varian != null && jumlah > 0) {
                val subtotal = varian.hargaJual * jumlah

                // Cari barangNama asli dari varianText (Format: "Nama Barang - Kemasan")
                val barangNama = varianText.substringBefore(" - ")

                val itemData = hashMapOf(
                    "penjualan_id" to penjualanId,
                    "varian_id" to varian.id,
                    "barang_nama" to barangNama, // Tambahkan ini agar Laporan bisa membaca
                    "kemasan_nama" to varian.kemasanNama, // Tambahkan ini agar Laporan bisa membaca
                    "harga_satuan" to varian.hargaJual,
                    "jumlah" to jumlah,
                    "subtotal" to subtotal,
                    "created_at" to System.currentTimeMillis()
                )

                val docRef = db.collection("penjualan_items").document()
                batch.set(docRef, itemData)

                val varianRef = db.collection("barang_varian").document(varian.id)
                batch.update(varianRef, "stok", FieldValue.increment(-jumlah.toLong()))
            }
        }

        batch.commit()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(this, "Transaksi berhasil disimpan", Toast.LENGTH_SHORT).show()
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
