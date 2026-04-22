package com.zahwaalviana.simpro.ui.produksi

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.data.model.Produksi
import com.zahwaalviana.simpro.data.model.ProduksiItem
import com.zahwaalviana.simpro.data.model.ProduksiWithItems
import com.zahwaalviana.simpro.databinding.FragmentProduksiListBinding
import com.zahwaalviana.simpro.ui.produksi.adapter.ProduksiAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class ProduksiListFragment : Fragment() {

    companion object {
        private const val ARG_USER_ROLE = "user_role"

        fun newInstance(role: String): ProduksiListFragment {
            return ProduksiListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_USER_ROLE, role)
                }
            }
        }
    }

    private var _binding: FragmentProduksiListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: ProduksiAdapter
    private var produksiListener: ListenerRegistration? = null
    
    private val allProduksiList = mutableListOf<ProduksiWithItems>()
    private val displayList = mutableListOf<ProduksiWithItems>()
    
    private val userRole: String get() = arguments?.getString(ARG_USER_ROLE) ?: "mandor"
    private val currentUserId: String get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private var currentSortColumn = "tanggal" // "tanggal"
    private var isAscending = false

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
        setupRoleUI()
    }

    override fun onResume() {
        super.onResume()
        loadProduksiData()
    }

    private fun setupToolbar() {
        binding.appBarLayout.visibility = View.GONE
    }

    private fun setupRoleUI() {
        binding.fabAdd.visibility =
            if (userRole == "mandor") View.VISIBLE
            else View.GONE
        
        // Sembunyikan kolom mandor di header jika user adalah mandor
        if (userRole == "mandor") {
            val tvHeaderMandor = binding.headerTable.getChildAt(1) as? TextView
            tvHeaderMandor?.visibility = View.GONE
            binding.tilSearch.hint = "Cari item produksi..."
        }
    }

    private fun setupRecyclerView() {
        adapter = ProduksiAdapter(
            displayList,
            userRole,
            onEditClick = { item ->
                val intent = Intent(requireContext(), ProduksiFormActivity::class.java)
                intent.putExtra("PRODUKSI_ID", item.produksi.id)
                intent.putExtra("USER_ROLE", userRole)
                startActivity(intent)
            },
            onDeleteClick = { item ->
                showDeleteConfirmation(item)
            }
        )

        binding.rvProduksi.layoutManager = LinearLayoutManager(requireContext())
        binding.rvProduksi.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            val intent = Intent(requireContext(), ProduksiFormActivity::class.java)
            intent.putExtra("USER_ROLE", userRole)
            startActivity(intent)
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadProduksiData()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) { applyFilter() }
        })

        binding.tvHeaderTanggal.setOnClickListener { toggleSort("tanggal") }
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
    }

    private fun loadProduksiData() {
        showLoading(true)

        produksiListener?.remove()
        produksiListener = db.collection("produksi")
            .orderBy("tanggal_produksi", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                allProduksiList.clear()

                val docs = if (userRole == "mandor") {
                    snapshots?.documents?.filter { it.getString("mandor_id") == currentUserId }
                } else {
                    snapshots?.documents
                } ?: emptyList()

                val totalDocs = docs.size
                var processedDocs = 0

                if (totalDocs == 0) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    applyFilter()
                    return@addSnapshotListener
                }

                docs.forEach { doc ->
                    val mandorId = doc.getString("mandor_id") ?: ""

                    db.collection("users").document(mandorId)
                        .get()
                        .addOnSuccessListener { mandorDoc ->
                            val produksi = Produksi(
                                id = doc.id,
                                tanggalProduksi = doc.getLong("tanggal_produksi") ?: 0L,
                                mandorId = mandorId,
                                mandorName = mandorDoc.getString("name") ?: "Unknown",
                            )

                            loadItemsForProduksi(produksi) { items ->
                                allProduksiList.add(ProduksiWithItems(produksi, items))
                                processedDocs++

                                if (processedDocs == totalDocs) {
                                    showLoading(false)
                                    binding.swipeRefresh.isRefreshing = false
                                    applyFilter()
                                }
                            }
                        }
                        .addOnFailureListener {
                            processedDocs++
                            if (processedDocs == totalDocs) {
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

        var filtered = allProduksiList.filter { item ->
            val matchSearch = query.isEmpty() ||
                item.produksi.mandorName.contains(query, ignoreCase = true) ||
                item.items.any { it.barangNama.contains(query, ignoreCase = true) }
            matchSearch
        }

        filtered = when (currentSortColumn) {
            "tanggal" -> {
                if (isAscending) filtered.sortedBy { it.produksi.tanggalProduksi }
                else filtered.sortedByDescending { it.produksi.tanggalProduksi }
            }
            else -> filtered
        }

        displayList.clear()
        displayList.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateEmptyState()
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

                    db.collection("barang_varian").document(varianId)
                        .get()
                        .addOnSuccessListener { varianDoc ->
                            val barangId = varianDoc.getString("barang_id") ?: ""
                            val kemasanId = varianDoc.getString("kemasan_id") ?: ""

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

                                if (processedItems == totalItems) callback(items)
                            }
                        }
                        .addOnFailureListener {
                            processedItems++
                            if (processedItems == totalItems) callback(items)
                        }
                }
            }
            .addOnFailureListener { callback(emptyList()) }
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

    private fun showDeleteConfirmation(item: ProduksiWithItems) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Produksi")
            .setMessage("Apakah Anda yakin ingin menghapus data produksi ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteProduksi(item)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteProduksi(item: ProduksiWithItems) {
        showLoading(true)
        val batch = db.batch()

        // 1. Kembalikan stok di master_kemasan (jika ada logika itu, tapi biasanya produksi hanya menambah stok barang_varian)
        // 2. Kurangi stok di barang_varian yang sudah diproduksi
        item.items.forEach { pItem ->
            val varianRef = db.collection("barang_varian").document(pItem.varianId)
            batch.update(varianRef, "stok", FieldValue.increment(-pItem.jumlahProduksi.toLong()))
            batch.delete(db.collection("produksi_items").document(pItem.id))
        }

        // 3. Hapus data produksi utama
        batch.delete(db.collection("produksi").document(item.produksi.id))

        batch.commit().addOnSuccessListener {
            showLoading(false)
            Toast.makeText(requireContext(), "Data produksi berhasil dihapus", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
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
        _binding = null
    }
}
