package com.zahwaalviana.simpro.ui.barang

import android.annotation.SuppressLint
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.data.model.Barang
import com.zahwaalviana.simpro.data.model.BarangWithVarian
import com.zahwaalviana.simpro.databinding.FragmentBarangListBinding
import com.zahwaalviana.simpro.ui.barang.adapter.BarangAdapter
import com.zahwaalviana.simpro.ui.barang.adapter.StokDetailAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BarangListFragment : Fragment() {

    private var _binding: FragmentBarangListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: BarangAdapter
    private var barangListener: ListenerRegistration? = null
    
    private val allBarangList = mutableListOf<BarangWithVarian>()
    private val displayList = mutableListOf<BarangWithVarian>()
    private val barangSortMap = mutableMapOf<String, Long>()

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
        val sortedAll = allBarangList.sortedByDescending { barangSortMap[it.barang.id] ?: 0L }

        val filteredList = if (query.isEmpty()) {
            sortedAll
        } else {
            sortedAll.filter { item ->
                item.barang.namaBarang.contains(query, ignoreCase = true)
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
            .orderBy("updated_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                allBarangList.clear()
                barangSortMap.clear()

                snapshots?.documents?.forEach { doc ->
                    val barangId = doc.id
                    val updatedAt = doc.getLong("updated_at") ?: 0L
                    barangSortMap[barangId] = updatedAt

                    val barang = Barang(
                        id = barangId,
                        namaBarang = doc.getString("nama_barang") ?: ""
                    )
                    // We only need the barang object now, variant list can be empty
                    allBarangList.add(BarangWithVarian(barang, emptyList()))
                }
                applyFilter()
            }
    }

    private fun showDeleteConfirmation(barangWithVarian: BarangWithVarian) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Barang")
            .setMessage("Apakah Anda yakin ingin menghapus barang \"${barangWithVarian.barang.namaBarang}\"?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteBarang(barangWithVarian.barang.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteBarang(id: String) {
        showLoading(true)
        db.collection("master_barang").document(id).delete()
            .addOnSuccessListener {
                showLoading(false)
                Toast.makeText(requireContext(), "Barang berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                showLoading(false)
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
}
