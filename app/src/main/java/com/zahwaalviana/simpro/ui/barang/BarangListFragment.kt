package com.zahwaalviana.simpro.ui.barang

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.Barang
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.data.model.BarangWithVarian
import com.zahwaalviana.simpro.databinding.FragmentBarangListBinding
import com.zahwaalviana.simpro.ui.barang.adapter.BarangAdapter
import com.zahwaalviana.simpro.ui.barang.adapter.StokDetailAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ceil

class BarangListFragment : Fragment() {

    private var _binding: FragmentBarangListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: BarangAdapter
    private var barangListener: ListenerRegistration? = null
    
    private val allBarangList = mutableListOf<BarangWithVarian>()
    private val displayList = mutableListOf<BarangWithVarian>()

    // Pagination variables
    private var currentPage = 1
    private val pageSize = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBarangListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadBarangData()
    }

    private fun setupToolbar() {
        binding.appBarLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = BarangAdapter(
            displayList,
            onEditClick = { barangWithVarian ->
                val intent = Intent(requireContext(), BarangFormActivity::class.java)
                intent.putExtra("BARANG_ID", barangWithVarian.barang.id)
                startActivity(intent)
            },
            onDeleteClick = { barangWithVarian ->
                showDeleteConfirmation(barangWithVarian)
            },
            onVarianClick = { varian, barangNama ->
                showStokDetailDialog(varian, barangNama)
            }
        )

        binding.rvBarang.layoutManager = LinearLayoutManager(requireContext())
        binding.rvBarang.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), BarangFormActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadBarangData()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentPage = 1
                applyFilter()
            }
        })

        // Pagination buttons
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                applyFilter()
            }
        }

        binding.btnNextPage.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            val filteredCount = if (query.isEmpty()) {
                allBarangList.size
            } else {
                allBarangList.count { item ->
                    item.barang.namaBarang.contains(query, ignoreCase = true) ||
                    item.varianList.any { it.kemasanNama.contains(query, ignoreCase = true) }
                }
            }
            val totalPage = ceil(filteredCount.toDouble() / pageSize).toInt()
            if (currentPage < totalPage) {
                currentPage++
                applyFilter()
            }
        }
    }

    private fun applyFilter() {
        val query = binding.etSearch.text.toString().trim()
        val filteredList = if (query.isEmpty()) {
            allBarangList
        } else {
            allBarangList.filter { item ->
                item.barang.namaBarang.contains(query, ignoreCase = true) ||
                item.varianList.any { it.kemasanNama.contains(query, ignoreCase = true) }
            }
        }

        val totalItems = filteredList.size
        val totalPage = ceil(totalItems.toDouble() / pageSize).toInt().coerceAtLeast(1)

        if (currentPage > totalPage) currentPage = totalPage

        val startIndex = (currentPage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(totalItems)

        displayList.clear()
        if (totalItems > 0) {
            displayList.addAll(filteredList.subList(startIndex, endIndex))
        }

        adapter.notifyDataSetChanged()
        updateEmptyState()
        updatePaginationUI(currentPage, totalPage)
    }

    private fun updatePaginationUI(current: Int, total: Int) {
        binding.cardPagination.visibility = if (allBarangList.isEmpty() && displayList.isEmpty()) View.GONE else View.VISIBLE
        binding.tvPageInfo.text = "$current / $total"
        
        binding.btnPrevPage.isEnabled = current > 1
        binding.btnPrevPage.alpha = if (current > 1) 1.0f else 0.3f
        
        binding.btnNextPage.isEnabled = current < total
        binding.btnNextPage.alpha = if (current < total) 1.0f else 0.3f
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadBarangData() {
        showLoading(true)

        barangListener?.remove()
        barangListener = db.collection("master_barang")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                allBarangList.clear()
                val loadedBarang = snapshots?.documents?.size ?: 0
                var processedBarang = 0

                if (loadedBarang == 0) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    applyFilter()
                    return@addSnapshotListener
                }

                snapshots?.documents?.forEach { doc ->
                    val barang = Barang(
                        id = doc.id,
                        namaBarang = doc.getString("nama_barang") ?: ""
                    )

                    loadVarianForBarang(barang) { varianList ->
                        allBarangList.add(BarangWithVarian(barang, varianList))
                        processedBarang++

                        if (processedBarang == loadedBarang) {
                            showLoading(false)
                            binding.swipeRefresh.isRefreshing = false
                            applyFilter()
                        }
                    }
                }
            }
    }

    private fun loadVarianForBarang(barang: Barang, callback: (List<BarangVarian>) -> Unit) {
        db.collection("barang_varian")
            .whereEqualTo("barang_id", barang.id)
            .get()
            .addOnSuccessListener { varianDocs ->
                val varianList = mutableListOf<BarangVarian>()
                val totalVarian = varianDocs.size()
                var processedVarian = 0

                if (totalVarian == 0) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                varianDocs.documents.forEach { varianDoc ->
                    val kemasanId = varianDoc.getString("kemasan_id") ?: ""

                    db.collection("master_kemasan")
                        .document(kemasanId)
                        .get()
                        .addOnSuccessListener { kemasanDoc ->
                            val varian = BarangVarian(
                                id = varianDoc.id,
                                barangId = varianDoc.getString("barang_id") ?: "",
                                kemasanId = kemasanId,
                                kemasanNama = kemasanDoc.getString("nama_kemasan") ?: "",
                                kemasanSatuan = kemasanDoc.getString("satuan") ?: "",
                                shelfLifeHari = varianDoc.getLong("shelf_life_hari")?.toInt() ?: 0,
                                hargaJual = varianDoc.getLong("harga_jual")?.toInt() ?: 0,
                                stok = varianDoc.getLong("stok")?.toInt() ?: 0
                            )
                            varianList.add(varian)
                            processedVarian++

                            if (processedVarian == totalVarian) {
                                callback(varianList)
                            }
                        }
                        .addOnFailureListener {
                            processedVarian++
                            if (processedVarian == totalVarian) {
                                callback(varianList)
                            }
                        }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
            }
    }

    @SuppressLint("SetTextI18n", "NotifyDataSetChanged")
    private fun showStokDetailDialog(varian: BarangVarian, barangNama: String) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_stok_detail, null)

        val tvTotalStok = dialogView.findViewById<TextView>(R.id.tvTotalStok)
        val tvEmpty = dialogView.findViewById<TextView>(R.id.tvEmpty)
        val progressBar = dialogView.findViewById<ProgressBar>(R.id.progressBar)
        val rvStokDetail = dialogView.findViewById<RecyclerView>(R.id.rvStokDetail)

        tvTotalStok.text = "Total Stok: ${varian.stok} ${varian.kemasanSatuan}"

        val detailList = mutableListOf<StokDetailItem>()
        val detailAdapter = StokDetailAdapter(detailList)
        rvStokDetail.layoutManager = LinearLayoutManager(requireContext())
        rvStokDetail.adapter = detailAdapter

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("$barangNama - ${varian.kemasanNama}")
            .setView(dialogView)
            .setPositiveButton("Tutup", null)
            .show()

        progressBar.visibility = View.VISIBLE
        db.collection("produksi_items")
            .whereEqualTo("varian_id", varian.id)
            .get()
            .addOnSuccessListener { itemDocs ->
                if (itemDocs.isEmpty) {
                    progressBar.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }

                val totalItems = itemDocs.size()
                var processedItems = 0

                itemDocs.documents.forEach { itemDoc ->
                    val produksiId = itemDoc.getString("produksi_id") ?: ""
                    val jumlah = itemDoc.getLong("jumlah_produksi")?.toInt() ?: 0

                    db.collection("produksi").document(produksiId)
                        .get()
                        .addOnSuccessListener { produksiDoc ->
                            val tanggal = produksiDoc.getLong("tanggal_produksi") ?: 0L
                            val mandorId = produksiDoc.getString("mandor_id") ?: ""

                            db.collection("users").document(mandorId)
                                .get()
                                .addOnSuccessListener { mandorDoc ->
                                    val mandorName = mandorDoc.getString("name") ?: "Unknown"
                                    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                                    val tanggalStr = dateFormat.format(Date(tanggal))

                                    detailList.add(
                                        StokDetailItem(
                                            mandorName = mandorName,
                                            tanggal = tanggalStr,
                                            jumlah = jumlah
                                        )
                                    )
                                    processedItems++

                                    if (processedItems == totalItems) {
                                        progressBar.visibility = View.GONE
                                        detailAdapter.notifyDataSetChanged()
                                        if (detailList.isEmpty()) {
                                            tvEmpty.visibility = View.VISIBLE
                                        }
                                    }
                                }
                                .addOnFailureListener {
                                    processedItems++
                                    if (processedItems == totalItems) {
                                        progressBar.visibility = View.GONE
                                        detailAdapter.notifyDataSetChanged()
                                    }
                                }
                        }
                        .addOnFailureListener {
                            processedItems++
                            if (processedItems == totalItems) {
                                progressBar.visibility = View.GONE
                                detailAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
            }
    }

    private fun showDeleteConfirmation(barangWithVarian: BarangWithVarian) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Barang")
            .setMessage("Apakah Anda yakin ingin menghapus barang \"${barangWithVarian.barang.namaBarang}\"? Semua varian dan stok terkait juga akan terhapus.")
            .setPositiveButton("Hapus") { _, _ ->
                deleteBarang(barangWithVarian)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteBarang(barangWithVarian: BarangWithVarian) {
        showLoading(true)
        val batch = db.batch()

        // 1. Delete all varian for this barang
        barangWithVarian.varianList.forEach { varian ->
            batch.delete(db.collection("barang_varian").document(varian.id))
        }

        // 2. Delete the barang itself
        batch.delete(db.collection("master_barang").document(barangWithVarian.barang.id))

        batch.commit()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(requireContext(), "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                showLoading(true)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        binding.tvEmpty.visibility = if (displayList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barangListener?.remove()
        _binding = null
    }

    data class StokDetailItem(
        val mandorName: String,
        val tanggal: String,
        val jumlah: Int
    )
}
