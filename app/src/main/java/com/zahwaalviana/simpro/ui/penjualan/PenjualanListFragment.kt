package com.zahwaalviana.simpro.ui.penjualan

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.data.model.Penjualan
import com.zahwaalviana.simpro.data.model.PenjualanItem
import com.zahwaalviana.simpro.data.model.PenjualanWithItems
import com.zahwaalviana.simpro.databinding.FragmentPenjualanListBinding
import com.zahwaalviana.simpro.ui.penjualan.adapter.PenjualanAdapter

class PenjualanListFragment : Fragment() {

    private var _binding: FragmentPenjualanListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: PenjualanAdapter
    private var penjualanListener: ListenerRegistration? = null
    private val penjualanList = mutableListOf<PenjualanWithItems>()

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
        db = FirebaseFirestore.getInstance()

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
            onDelete = { item ->
                showDeleteConfirmation(item)
            }
        )
        binding.rvPenjualan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPenjualan.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), PenjualanFormActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadPenjualanData()
        }
    }

    private fun loadPenjualanData() {
        showLoading(true)

        penjualanListener = db.collection("penjualan")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                penjualanList.clear()
                val docs = snapshots?.documents ?: emptyList()
                val totalDocs = docs.size
                var processedDocs = 0

                if (totalDocs == 0) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    adapter.notifyDataSetChanged()
                    updateEmptyState()
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
                        penjualanList.add(PenjualanWithItems(penjualan, items))
                        processedDocs++

                        if (processedDocs == totalDocs) {
                            showLoading(false)
                            binding.swipeRefresh.isRefreshing = false
                            adapter.notifyDataSetChanged()
                            updateEmptyState()
                        }
                    }
                }
            }
    }

    private fun loadItemsForPenjualan(penjualan: Penjualan, callback: (List<PenjualanItem>) -> Unit) {
        db.collection("penjualan_items")
            .whereEqualTo("penjualan_id", penjualan.id)
            .get()
            .addOnSuccessListener { itemDocs ->
                val items = mutableListOf<PenjualanItem>()
                val totalItems = itemDocs.size()
                var processedItems = 0

                if (totalItems == 0) {
                    callback(emptyList())
                    return@addOnSuccessListener
                }

                itemDocs.documents.forEach { itemDoc ->
                    val varianId = itemDoc.getString("varian_id") ?: ""

                    // Load varian to get barang name
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
            .setPositiveButton("Hapus") { _, _ ->
                deletePenjualan(item)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePenjualan(penjualanWithItems: PenjualanWithItems) {
        db.collection("penjualan_items")
            .whereEqualTo("penjualan_id", penjualanWithItems.penjualan.id)
            .get()
            .addOnSuccessListener { itemDocs ->
                val batch = db.batch()

                // Revert stok for each item
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

                // Delete penjualan
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
