package com.zahwaalviana.simpro.ui.produksi

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
import com.zahwaalviana.simpro.data.model.Produksi
import com.zahwaalviana.simpro.data.model.ProduksiItem
import com.zahwaalviana.simpro.data.model.ProduksiWithItems
import com.zahwaalviana.simpro.databinding.FragmentProduksiListBinding
import com.zahwaalviana.simpro.ui.produksi.adapter.ProduksiAdapter


class ProduksiListFragment : Fragment() {

    private var _binding: FragmentProduksiListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ProduksiAdapter
    private var produksiListener: ListenerRegistration? = null
    private val produksiList = mutableListOf<ProduksiWithItems>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProduksiListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupListeners()
    }

    override fun onResume() {
        super.onResume()
        loadProduksiData()
    }

    private fun setupToolbar() {
        binding.appBarLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = ProduksiAdapter(
            produksiList,
            onEditClick = { produksiWithItems ->
                val intent = Intent(requireContext(), ProduksiFormActivity::class.java)
                intent.putExtra("PRODUKSI_ID", produksiWithItems.produksi.id)
                startActivity(intent)
            },
            onDeleteClick = { produksiWithItems ->
                showDeleteConfirmation(produksiWithItems)
            }
        )

        binding.rvProduksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProduksi.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), ProduksiFormActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadProduksiData()
        }
    }

    private fun loadProduksiData() {
        showLoading(true)

        produksiListener = db.collection("produksi")
            .orderBy("tanggal_produksi", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                produksiList.clear()
                val loadedProduksi = snapshots?.documents?.size ?: 0
                var processedProduksi = 0

                if (loadedProduksi == 0) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    updateEmptyState()
                    return@addSnapshotListener
                }

                snapshots?.documents?.forEach { doc ->
                    // Load mandor data
                    val mandorId = doc.getString("mandor_id") ?: ""

                    db.collection("users").document(mandorId)
                        .get()
                        .addOnSuccessListener { mandorDoc ->
                            val produksi = Produksi(
                                id = doc.id,
                                tanggalProduksi = doc.getLong("tanggal_produksi") ?: 0L,
                                mandorId = mandorId,
                                mandorName = mandorDoc.getString("name") ?: "Unknown",
                                totalBiayaProduksi = doc.getLong("total_biaya_produksi")?.toInt()
                                    ?: 0
                            )

                            // Load items for this produksi
                            loadItemsForProduksi(produksi) { items ->
                                produksiList.add(ProduksiWithItems(produksi, items))
                                processedProduksi++

                                if (processedProduksi == loadedProduksi) {
                                    showLoading(false)
                                    binding.swipeRefresh.isRefreshing = false
                                    adapter.notifyDataSetChanged()
                                    updateEmptyState()
                                }
                            }
                        }
                        .addOnFailureListener {
                            processedProduksi++
                            if (processedProduksi == loadedProduksi) {
                                showLoading(false)
                                binding.swipeRefresh.isRefreshing = false
                                adapter.notifyDataSetChanged()
                                updateEmptyState()
                            }
                        }
                }
            }
    }

    private fun loadItemsForProduksi(produksi: Produksi, callback: (List<ProduksiItem>) -> Unit) {
        db.collection("produksi_items")
            .whereEqualTo("produksi_id", produksi.id)
            .get()
            .addOnSuccessListener { itemDocs ->
                val items = mutableListOf<ProduksiItem>()
                val totalItems = itemDocs.size()
                var processedItems = 0

                if (totalItems == 0) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                itemDocs.documents.forEach { itemDoc ->
                    val varianId = itemDoc.getString("varian_id") ?: ""

                    // Load varian data untuk mendapatkan nama barang dan kemasan
                    db.collection("barang_varian").document(varianId)
                        .get()
                        .addOnSuccessListener { varianDoc ->
                            val barangId = varianDoc.getString("barang_id") ?: ""
                            val kemasanId = varianDoc.getString("kemasan_id") ?: ""

                            // Load barang dan kemasan
                            loadBarangAndKemasan(barangId, kemasanId) { barangNama, kemasanNama, kemasanSatuan ->
                                val item = ProduksiItem(
                                    id = itemDoc.id,
                                    produksiId = itemDoc.getString("produksi_id") ?: "",
                                    varianId = varianId,
                                    barangNama = barangNama,
                                    kemasanNama = kemasanNama,
                                    kemasanSatuan = kemasanSatuan,
                                    jumlahProduksi = itemDoc.getLong("jumlah_produksi")?.toInt() ?: 0,
                                    expiredAt = itemDoc.getLong("expired_at") ?: 0L,
                                    shelfLifeHari = varianDoc.getLong("shelf_life_hari")?.toInt() ?: 0
                                )
                                items.add(item)
                                processedItems++

                                if (processedItems == totalItems) {
                                    callback(items)
                                }
                            }
                        }
                        .addOnFailureListener {
                            processedItems++
                            if (processedItems == totalItems) {
                                callback(items)
                            }
                        }
                }
            }
            .addOnFailureListener {
                callback(emptyList())
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
            .addOnSuccessListener { barangDoc ->
                barangNama = barangDoc.getString("nama_barang") ?: ""
                completed++
                if (completed == 2) callback(barangNama, kemasanNama, kemasanSatuan)
            }

        db.collection("master_kemasan").document(kemasanId)
            .get()
            .addOnSuccessListener { kemasanDoc ->
                kemasanNama = kemasanDoc.getString("nama_kemasan") ?: ""
                kemasanSatuan = kemasanDoc.getString("satuan") ?: ""
                completed++
                if (completed == 2) callback(barangNama, kemasanNama, kemasanSatuan)
            }
    }

    private fun showDeleteConfirmation(produksiWithItems: ProduksiWithItems) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Produksi")
            .setMessage("Apakah Anda yakin ingin menghapus data produksi ini? Semua item juga akan dihapus.")
            .setPositiveButton("Hapus") { _, _ ->
                deleteProduksi(produksiWithItems)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteProduksi(produksiWithItems: ProduksiWithItems) {
        // Delete all items first
        db.collection("produksi_items")
            .whereEqualTo("produksi_id", produksiWithItems.produksi.id)
            .get()
            .addOnSuccessListener { itemDocs ->
                val batch = db.batch()
                itemDocs.documents.forEach { doc ->
                    batch.delete(doc.reference)
                }

                // Delete produksi
                batch.delete(db.collection("produksi").document(produksiWithItems.produksi.id))

                batch.commit()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    private fun updateEmptyState() {
        if (produksiList.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvProduksi.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvProduksi.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        produksiListener?.remove()
        _binding = null
    }
}