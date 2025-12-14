package com.zahwaalviana.simpro.ui.admin.barang

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
import com.zahwaalviana.simpro.data.model.Barang
import com.zahwaalviana.simpro.databinding.FragmentBarangListBinding
import com.zahwaalviana.simpro.ui.admin.barang.adapter.BarangAdapter

class BarangListFragment : Fragment() {
    private var _binding: FragmentBarangListBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var barangAdapter: BarangAdapter
    private var barangListener: ListenerRegistration? = null

    private var allBarang = listOf<Barang>()
    private var filteredBarang = listOf<Barang>()

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
        setupRecyclerView()
        setupSearchView()
        setupListeners()
        loadBarang()
    }

    private fun setupRecyclerView() {
        barangAdapter = BarangAdapter(
            onEditClick = { barang ->
                navigateToEditBarang(barang)
            },
            onDeleteClick = { barang ->
                showDeleteConfirmation(barang)
            }
        )

        binding.recyclerViewBarang.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = barangAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBarang(newText ?: "")
                return true
            }
        })
    }

    private fun setupListeners() {
        binding.btnAddBarang.setOnClickListener {
            navigateToAddBarang()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            loadBarang()
        }
    }

    private fun loadBarang() {
        showLoading(true)

        barangListener = db.collection("master_barang")
            .addSnapshotListener { snapshot, error ->
                showLoading(false)

                if (error != null) {
                    showError("Gagal memuat data: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    allBarang = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Barang::class.java)?.copy(barangId = doc.id)
                    }
                    filteredBarang = allBarang
                    barangAdapter.submitList(filteredBarang)
                    showEmptyState(false)
                } else {
                    allBarang = emptyList()
                    filteredBarang = emptyList()
                    barangAdapter.submitList(emptyList())
                    showEmptyState(true)
                }
            }
    }

    private fun filterBarang(query: String) {
        filteredBarang = if (query.isEmpty()) {
            allBarang
        } else {
            allBarang.filter {
                it.namaBahanBaku.contains(query, ignoreCase = true) ||
                        it.namaBarangJadi.contains(query, ignoreCase = true)
            }
        }
        barangAdapter.submitList(filteredBarang)
        showEmptyState(filteredBarang.isEmpty())
    }

    private fun navigateToAddBarang() {
        startActivity(Intent(requireContext(), BarangFormActivity::class.java))
    }

    private fun navigateToEditBarang(barang: Barang) {
        val intent = Intent(requireContext(), BarangFormActivity::class.java).apply {
            putExtra("BARANG_DATA", barang)
        }
        startActivity(intent)
    }

    private fun showDeleteConfirmation(barang: Barang) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Barang")
            .setMessage("Apakah Anda yakin ingin menghapus ${barang.namaBarangJadi}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteBarang(barang)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteBarang(barang: Barang) {
        barang.barangId?.let { id ->
            db.collection("master_barang").document(id)
                .delete()
                .addOnSuccessListener {
                    showError("Barang berhasil dihapus")
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
        binding.recyclerViewBarang.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        barangListener?.remove()
        _binding = null
    }
}