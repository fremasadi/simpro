package com.zahwaalviana.simpro.ui.admin.barang

import android.content.Intent
import android.os.Bundle
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
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.data.model.BarangWithVarian
import com.zahwaalviana.simpro.databinding.FragmentBarangListBinding
import com.zahwaalviana.simpro.ui.admin.barang.adapter.BarangAdapter

class BarangListFragment : Fragment() {

    private var _binding: FragmentBarangListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: BarangAdapter
    private var barangListener: ListenerRegistration? = null
    private val barangList = mutableListOf<BarangWithVarian>()

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
            barangList,
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
    }

//    override fun onResume() {
//        super.onResume()
//        // Reload data setiap kali fragment muncul kembali
//        loadBarangData()
//    }

    private fun loadBarangData() {
        showLoading(true)

        barangListener = db.collection("master_barang")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                barangList.clear()
                val loadedBarang = snapshots?.documents?.size ?: 0
                var processedBarang = 0

                if (loadedBarang == 0) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    updateEmptyState()
                    return@addSnapshotListener
                }

                snapshots?.documents?.forEach { doc ->
                    val barang = Barang(
                        id = doc.id,
                        namaBarang = doc.getString("nama_barang") ?: ""
                    )

                    // Load varian for this barang
                    loadVarianForBarang(barang) { varianList ->
                        barangList.add(BarangWithVarian(barang, varianList))
                        processedBarang++

                        if (processedBarang == loadedBarang) {
                            showLoading(false)
                            binding.swipeRefresh.isRefreshing = false
                            adapter.notifyDataSetChanged()
                            updateEmptyState()
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

                    // Load kemasan data
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
                                hargaJual = varianDoc.getLong("harga_jual")?.toInt() ?: 0
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

    private fun showDeleteConfirmation(barangWithVarian: BarangWithVarian) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Barang")
            .setMessage("Apakah Anda yakin ingin menghapus ${barangWithVarian.barang.namaBarang}? Semua varian juga akan dihapus.")
            .setPositiveButton("Hapus") { _, _ ->
                deleteBarang(barangWithVarian)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteBarang(barangWithVarian: BarangWithVarian) {
        // Delete all varian first
        db.collection("barang_varian")
            .whereEqualTo("barang_id", barangWithVarian.barang.id)
            .get()
            .addOnSuccessListener { varianDocs ->
                val batch = db.batch()
                varianDocs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                // Delete barang
                batch.delete(db.collection("master_barang").document(barangWithVarian.barang.id))

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        if (barangList.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvBarang.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvBarang.visibility = View.VISIBLE
        }
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