package com.zahwaalviana.simpro.ui.admin.kemasan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zahwaalviana.simpro.data.model.Kemasan
import com.zahwaalviana.simpro.databinding.FragmentKemasanListBinding
import com.zahwaalviana.simpro.ui.admin.kemasan.adapter.KemasanAdapter

class KemasanListFragment : Fragment() {
    private var _binding: FragmentKemasanListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var kemasanAdapter: KemasanAdapter
    private var kemasanListener: ListenerRegistration? = null

    private var allKemasan = listOf<Kemasan>()
    private var filteredKemasan = listOf<Kemasan>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKemasanListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupSearchView()
        setupListeners()
        loadKemasan()
    }

    private fun setupRecyclerView() {
        kemasanAdapter = KemasanAdapter(
            onEditClick = { kemasan ->
                navigateToEditKemasan(kemasan)
            },
            onDeleteClick = { kemasan ->
                showDeleteConfirmation(kemasan)
            }
        )

        binding.recyclerViewKemasan.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = kemasanAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterKemasan(newText ?: "")
                return true
            }
        })
    }

    private fun setupListeners() {
        binding.btnAddKemasan.setOnClickListener {
            navigateToAddKemasan()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadKemasan()
        }
    }

    private fun loadKemasan() {
        showLoading(true)

        kemasanListener = db.collection("master_kemasan")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    showLoading(false)
                    showError("Gagal memuat data: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val kemasanList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Kemasan::class.java)?.copy(kemasanId = doc.id)
                    }

                    // Load nama barang jadi untuk setiap kemasan
                    loadBarangNames(kemasanList)
                } else {
                    showLoading(false)
                    allKemasan = emptyList()
                    filteredKemasan = emptyList()
                    kemasanAdapter.submitList(emptyList())
                    showEmptyState(true)
                }
            }
    }

    private fun loadBarangNames(kemasanList: List<Kemasan>) {
        val barangIds = kemasanList.map { it.barangId }.distinct()

        if (barangIds.isEmpty()) {
            showLoading(false)
            allKemasan = kemasanList
            filteredKemasan = kemasanList
            kemasanAdapter.submitList(kemasanList)
            showEmptyState(false)
            return
        }

        db.collection("master_barang")
            .whereIn("__name__", barangIds)
            .get()
            .addOnSuccessListener { barangSnapshot ->
                showLoading(false)

                val barangMap = barangSnapshot.documents.associate { doc ->
                    doc.id to (doc.getString("namaBarangJadi") ?: "Unknown")
                }

                allKemasan = kemasanList.map { kemasan ->
                    kemasan.copy(namaBarangJadi = barangMap[kemasan.barangId] ?: "Unknown")
                }
                filteredKemasan = allKemasan
                kemasanAdapter.submitList(filteredKemasan)
                showEmptyState(false)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showError("Gagal memuat nama barang: ${e.message}")
                allKemasan = kemasanList
                filteredKemasan = kemasanList
                kemasanAdapter.submitList(kemasanList)
                showEmptyState(false)
            }
    }

    private fun filterKemasan(query: String) {
        filteredKemasan = if (query.isEmpty()) {
            allKemasan
        } else {
            allKemasan.filter {
                it.namaKemasan.contains(query, ignoreCase = true) ||
                        it.namaBarangJadi.contains(query, ignoreCase = true)
            }
        }
        kemasanAdapter.submitList(filteredKemasan)
        showEmptyState(filteredKemasan.isEmpty())
    }

    private fun navigateToAddKemasan() {
        startActivity(Intent(requireContext(), KemasanFormActivity::class.java))
    }

    private fun navigateToEditKemasan(kemasan: Kemasan) {
        val intent = Intent(requireContext(), KemasanFormActivity::class.java).apply {
            putExtra("KEMASAN_DATA", kemasan)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(kemasan: Kemasan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Kemasan")
            .setMessage("Apakah Anda yakin ingin menghapus ${kemasan.namaKemasan}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteKemasan(kemasan)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteKemasan(kemasan: Kemasan) {
        kemasan.kemasanId?.let { id ->
            db.collection("master_kemasan").document(id)
                .delete()
                .addOnSuccessListener {
                    showError("Kemasan berhasil dihapus")
                }
                .addOnFailureListener { e ->
                    showError("Gagal menghapus: ${e.message}")
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.swipeRefreshLayout.isRefreshing = isLoading
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyState(isEmpty: Boolean) {
        binding.emptyStateLayout.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerViewKemasan.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        kemasanListener?.remove()
        _binding = null
    }
}