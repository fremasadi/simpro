package com.zahwaalviana.simpro.ui.pengeluaran

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
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.data.model.Pengeluaran
import com.zahwaalviana.simpro.databinding.FragmentPengeluaranListBinding
import com.zahwaalviana.simpro.ui.pengeluaran.adapter.PengeluaranAdapter
import kotlin.math.ceil

class PengeluaranListFragment : Fragment() {

    private var _binding: FragmentPengeluaranListBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { FirebaseFirestore.getInstance() }

    private val allPengeluaranList = mutableListOf<Pengeluaran>()
    private val displayList = mutableListOf<Pengeluaran>()
    private val masterMap = mutableMapOf<String, Pair<String, String>>()

    private lateinit var adapter: PengeluaranAdapter

    private var currentSortColumn = "tanggal" // "tanggal" or "biaya"
    private var isAscending = false

    // Pagination variables
    private var currentPage = 1
    private val pageSize = 10

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPengeluaranListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadMasterPengeluaran()
    }

    private fun setupToolbar() {
        binding.appBarLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = PengeluaranAdapter(
            displayList,
            masterMap,
            onEdit = {
                val intent = Intent(requireContext(), PengeluaranFormActivity::class.java)
                intent.putExtra("PENGELUARAN_ID", it.id)
                startActivity(intent)
            },
            onDelete = {
                confirmDelete(it)
            }
        )

        binding.rvPengeluaran.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPengeluaran.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabTambah.setOnClickListener {
            startActivity(Intent(requireActivity(), PengeluaranFormActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadMasterPengeluaran()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentPage = 1
                applyFilter()
            }
        })

        binding.tvHeaderTanggal.setOnClickListener { toggleSort("tanggal") }
        binding.tvHeaderBiaya.setOnClickListener { toggleSort("biaya") }

        // Pagination buttons
        binding.btnPrevPage.setOnClickListener {
            if (currentPage > 1) {
                currentPage--
                applyFilter()
            }
        }

        binding.btnNextPage.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            val filteredCount = allPengeluaranList.count { item ->
                val master = masterMap[item.masterPengeluaranId]
                val namaMatch = master?.first?.contains(query, ignoreCase = true) == true
                val ketMatch = item.keterangan.contains(query, ignoreCase = true)
                query.isEmpty() || namaMatch || ketMatch
            }
            val totalPage = ceil(filteredCount.toDouble() / pageSize).toInt().coerceAtLeast(1)
            if (currentPage < totalPage) {
                currentPage++
                applyFilter()
            }
        }
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
        binding.tvHeaderBiaya.text = "Biaya" + (if (currentSortColumn == "biaya") arrow else "")
    }

    private fun loadMasterPengeluaran() {
        showLoading(true)
        db.collection("master_pengeluaran")
            .get()
            .addOnSuccessListener { snapshot ->
                masterMap.clear()
                snapshot.documents.forEach {
                    val nama = it.getString("nama_pengeluaran") ?: ""
                    val kategori = it.getString("kategori") ?: ""
                    masterMap[it.id] = Pair(nama, kategori)
                }
                loadPengeluaran()
            }
            .addOnFailureListener {
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
                toast(it.message)
            }
    }

    private fun loadPengeluaran() {
        db.collection("pengeluaran")
            .get()
            .addOnSuccessListener { snapshot ->
                allPengeluaranList.clear()
                snapshot.documents.forEach { doc ->
                    allPengeluaranList.add(
                        Pengeluaran(
                            id = doc.id,
                            masterPengeluaranId = doc.getString("master_pengeluaran_id") ?: "",
                            produksiId = doc.getString("produksi_id") ?: "",
                            tanggal = doc.getString("tanggal") ?: "",
                            keterangan = doc.getString("keterangan") ?: "",
                            biaya = doc.getLong("biaya")?.toInt() ?: 0
                        )
                    )
                }
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
                applyFilter()
            }
            .addOnFailureListener {
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false
                toast(it.message)
            }
    }

    private fun applyFilter() {
        val query = binding.etSearch.text.toString().trim()
        
        var filtered = allPengeluaranList.filter { item ->
            val master = masterMap[item.masterPengeluaranId]
            val namaMatch = master?.first?.contains(query, ignoreCase = true) == true
            val ketMatch = item.keterangan.contains(query, ignoreCase = true)
            query.isEmpty() || namaMatch || ketMatch
        }

        filtered = when (currentSortColumn) {
            "tanggal" -> {
                if (isAscending) filtered.sortedBy { it.tanggal }
                else filtered.sortedByDescending { it.tanggal }
            }
            "biaya" -> {
                if (isAscending) filtered.sortedBy { it.biaya }
                else filtered.sortedByDescending { it.biaya }
            }
            else -> filtered
        }

        val totalItems = filtered.size
        val totalPage = ceil(totalItems.toDouble() / pageSize).toInt().coerceAtLeast(1)

        if (currentPage > totalPage) currentPage = totalPage

        val startIndex = (currentPage - 1) * pageSize
        val endIndex = (startIndex + pageSize).coerceAtMost(totalItems)

        displayList.clear()
        if (totalItems > 0) {
            displayList.addAll(filtered.subList(startIndex, endIndex))
        }

        adapter.notifyDataSetChanged()
        updateEmptyState()
        updatePaginationUI(currentPage, totalPage)
    }

    private fun updatePaginationUI(current: Int, total: Int) {
        binding.cardPagination.visibility = if (allPengeluaranList.isEmpty() && displayList.isEmpty()) View.GONE else View.VISIBLE
        binding.tvPageInfo.text = "$current / $total"
        
        binding.btnPrevPage.isEnabled = current > 1
        binding.btnPrevPage.alpha = if (current > 1) 1.0f else 0.3f
        
        binding.btnNextPage.isEnabled = current < total
        binding.btnNextPage.alpha = if (current < total) 1.0f else 0.3f
    }

    private fun confirmDelete(item: Pengeluaran) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pengeluaran")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deletePengeluaran(item.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePengeluaran(id: String) {
        db.collection("pengeluaran")
            .document(id)
            .delete()
            .addOnSuccessListener {
                toast("Berhasil dihapus")
                loadMasterPengeluaran()
            }
            .addOnFailureListener {
                toast(it.message)
            }
    }

    private fun updateEmptyState() {
        binding.tvEmpty.visibility =
            if (displayList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String?) {
        Toast.makeText(requireContext(), msg ?: "Error", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
