package com.zahwaalviana.simpro.ui.penjualan

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.data.model.Penjualan
import com.zahwaalviana.simpro.data.model.PenjualanItem
import com.zahwaalviana.simpro.data.model.PenjualanWithItems
import com.zahwaalviana.simpro.databinding.FragmentPenjualanListBinding
import com.zahwaalviana.simpro.ui.penjualan.adapter.PenjualanAdapter
import java.text.SimpleDateFormat
import java.util.*

class PenjualanListFragment : Fragment() {

    private var _binding: FragmentPenjualanListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: PenjualanAdapter
    private var penjualanListener: ListenerRegistration? = null

    private val allPenjualanList = mutableListOf<PenjualanWithItems>()
    private val penjualanList = mutableListOf<PenjualanWithItems>()

    private var startDateMs: Long? = null
    private var endDateMs: Long? = null
    private val dateDisplayFormat = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID"))

    private var currentSortColumn = "tanggal" // "tanggal" or "total"
    private var isAscending = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPenjualanListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        binding.appBarLayout.visibility = View.GONE
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadPenjualanData()
    }

    private fun setupRecyclerView() {
        adapter = PenjualanAdapter(
            penjualanList,
            onDelete = { item -> showDeleteConfirmation(item) }
        )
        binding.rvPenjualan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPenjualan.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPenjualanData()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })

        val dateRangeClickListener = View.OnClickListener { showDateRangePicker() }
        binding.etStartDate.setOnClickListener(dateRangeClickListener)
        binding.etEndDate.setOnClickListener(dateRangeClickListener)
        binding.tilStartDate.setOnClickListener(dateRangeClickListener)
        binding.tilEndDate.setOnClickListener(dateRangeClickListener)

        binding.btnResetDate.setOnClickListener {
            startDateMs = null
            endDateMs = null
            binding.etStartDate.setText("")
            binding.etEndDate.setText("")
            binding.btnResetDate.visibility = View.GONE
            loadPenjualanData()
        }

        binding.tvHeaderTanggal.setOnClickListener { toggleSort("tanggal") }
        binding.tvHeaderTotal.setOnClickListener { toggleSort("total") }
    }

    private fun toggleSort(column: String) {
        if (currentSortColumn == column) {
            isAscending = !isAscending
        } else {
            currentSortColumn = column
            isAscending = true
        }
        updateSortHeaders()
        applyFilter()
    }

    private fun updateSortHeaders() {
        val arrow = if (isAscending) " ↑" else " ↓"
        binding.tvHeaderTanggal.text = "Tanggal" + (if (currentSortColumn == "tanggal") arrow else "")
        binding.tvHeaderTotal.text = "Total" + (if (currentSortColumn == "total") arrow else "")
    }

    private fun showDateRangePicker() {
        val picker = MaterialDatePicker.Builder.dateRangePicker()
            .setTitleText("Pilih Rentang Tanggal")
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val utcStart = selection.first ?: return@addOnPositiveButtonClickListener
            val utcEnd = selection.second ?: utcStart
            
            // MaterialDatePicker mengembalikan waktu dalam UTC midnight.
            // Kita konversi ke Local Time midnight agar filter "hari itu saja" akurat di waktu lokal.
            val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            
            cal.timeInMillis = utcStart
            val localStart = Calendar.getInstance()
            localStart.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 0, 0, 0)
            localStart.set(Calendar.MILLISECOND, 0)
            startDateMs = localStart.timeInMillis
            
            cal.timeInMillis = utcEnd
            val localEnd = Calendar.getInstance()
            localEnd.set(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH), 23, 59, 59)
            localEnd.set(Calendar.MILLISECOND, 999)
            endDateMs = localEnd.timeInMillis

            binding.etStartDate.setText(dateDisplayFormat.format(Date(startDateMs!!)))
            binding.etEndDate.setText(dateDisplayFormat.format(Date(endDateMs!!)))
            binding.btnResetDate.visibility = View.VISIBLE
            
            // Langsung ambil data dari Firestore berdasarkan filter tanggal baru
            loadPenjualanData()
        }

        picker.show(parentFragmentManager, "date_range_picker")
    }

    private fun loadPenjualanData() {
        showLoading(true)

        penjualanListener?.remove()
        
        var query: Query = db.collection("penjualan")
        
        // Filter di sisi database agar lebih efisien (get data hari itu saja)
        if (startDateMs != null && endDateMs != null) {
            query = query.whereGreaterThanOrEqualTo("tanggal", startDateMs!!)
                         .whereLessThanOrEqualTo("tanggal", endDateMs!!)
        }
        
        // Urutkan DESCENDING agar data terbaru di atas
        penjualanListener = query.orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                allPenjualanList.clear()
                val docs = snapshots?.documents ?: emptyList()
                var processedDocs = 0

                if (docs.isEmpty()) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    applyFilter()
                    return@addSnapshotListener
                }

                docs.forEach { doc ->
                    val penjualan = Penjualan(
                        id = doc.id,
                        tanggal = doc.getLong("tanggal") ?: 0L,
                        totalHarga = doc.getLong("total_harga")?.toInt() ?: 0,
                        totalBayar = doc.getLong("total_bayar")?.toInt() ?: 0,
                        kembalian = doc.getLong("kembalian")?.toInt() ?: 0
                    )

                    loadItemsForPenjualan(penjualan) { items ->
                        allPenjualanList.add(PenjualanWithItems(penjualan, items))
                        processedDocs++

                        if (processedDocs == docs.size) {
                            showLoading(false)
                            binding.swipeRefresh.isRefreshing = false
                            applyFilter()
                        }
                    }
                }
            }
    }

    private fun applyFilter() {
        val query = binding.etSearch.text.toString().trim()

        var filtered = allPenjualanList.filter { item ->
            val matchSearch = query.isEmpty() ||
                item.items.any { it.barangNama.contains(query, ignoreCase = true) }
            matchSearch
        }

        // Apply Sorting (Urutan terbaru di atas secara default)
        filtered = when (currentSortColumn) {
            "tanggal" -> {
                if (isAscending) filtered.sortedBy { it.penjualan.tanggal }
                else filtered.sortedByDescending { it.penjualan.tanggal }
            }
            "total" -> {
                if (isAscending) filtered.sortedBy { it.penjualan.totalHarga }
                else filtered.sortedByDescending { it.penjualan.totalHarga }
            }
            else -> filtered.sortedByDescending { it.penjualan.tanggal }
        }

        penjualanList.clear()
        penjualanList.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    private fun loadItemsForPenjualan(penjualan: Penjualan, callback: (List<PenjualanItem>) -> Unit) {
        db.collection("penjualan_items")
            .whereEqualTo("penjualan_id", penjualan.id)
            .get()
            .addOnSuccessListener { itemDocs ->
                val items = mutableListOf<PenjualanItem>()
                var processedItems = 0

                if (itemDocs.isEmpty) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                itemDocs.documents.forEach { itemDoc ->
                    val varianId = itemDoc.getString("varian_id") ?: ""

                    db.collection("barang_varian").document(varianId)
                        .get()
                        .addOnSuccessListener { varianDoc ->
                            val barangId = varianDoc.getString("barang_id") ?: ""
                            val kemasanId = varianDoc.getString("kemasan_id") ?: ""

                            loadBarangAndKemasan(barangId, kemasanId) { barangNama, kemasanNama ->
                                val item = PenjualanItem(
                                    id = itemDoc.id,
                                    penjualanId = penjualan.id,
                                    varianId = varianId,
                                    barangNama = barangNama,
                                    kemasanNama = kemasanNama,
                                    hargaSatuan = itemDoc.getLong("harga_satuan")?.toInt() ?: 0,
                                    jumlah = itemDoc.getLong("jumlah")?.toInt() ?: 0,
                                    subtotal = itemDoc.getLong("subtotal")?.toInt() ?: 0
                                )
                                items.add(item)
                                processedItems++

                                if (processedItems == itemDocs.size()) callback(items)
                            }
                        }
                        .addOnFailureListener {
                            processedItems++
                            if (processedItems == itemDocs.size()) callback(items)
                        }
                }
            }
            .addOnFailureListener { callback(emptyList()) }
    }

    private fun loadBarangAndKemasan(
        barangId: String,
        kemasanId: String,
        callback: (String, String) -> Unit
    ) {
        var barangNama = ""
        var kemasanNama = ""
        var completed = 0

        db.collection("master_barang").document(barangId)
            .get()
            .addOnSuccessListener { doc ->
                barangNama = doc.getString("nama_barang") ?: ""
                completed++
                if (completed == 2) callback(barangNama, kemasanNama)
            }

        db.collection("master_kemasan").document(kemasanId)
            .get()
            .addOnSuccessListener { doc ->
                kemasanNama = doc.getString("nama_kemasan") ?: ""
                completed++
                if (completed == 2) callback(barangNama, kemasanNama)
            }
    }

    private fun showDeleteConfirmation(item: PenjualanWithItems) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Transaksi")
            .setMessage("Yakin ingin menghapus transaksi ini? Stok barang akan dikembalikan.")
            .setPositiveButton("Hapus") { _, _ -> deletePenjualan(item) }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePenjualan(penjualanWithItems: PenjualanWithItems) {
        db.collection("penjualan_items")
            .whereEqualTo("penjualan_id", penjualanWithItems.penjualan.id)
            .get()
            .addOnSuccessListener { itemDocs ->
                val batch = db.batch()

                itemDocs.documents.forEach { doc ->
                    val varianId = doc.getString("varian_id") ?: ""
                    val jumlah = doc.getLong("jumlah")?.toInt() ?: 0

                    if (varianId.isNotEmpty() && jumlah > 0) {
                        batch.update(
                            db.collection("barang_varian").document(varianId),
                            "stok", FieldValue.increment(jumlah.toLong())
                        )
                    }

                    batch.delete(doc.reference)
                }

                batch.delete(db.collection("penjualan").document(penjualanWithItems.penjualan.id))

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Transaksi berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun updateEmptyState() {
        if (penjualanList.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvPenjualan.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvPenjualan.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        penjualanListener?.remove()
        _binding = null
    }
}
