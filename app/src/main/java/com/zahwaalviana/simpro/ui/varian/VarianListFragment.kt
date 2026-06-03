package com.zahwaalviana.simpro.ui.varian

import android.annotation.SuppressLint
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
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.data.model.StokDetailItem
import com.zahwaalviana.simpro.databinding.FragmentVarianListBinding
import com.zahwaalviana.simpro.ui.barang.adapter.StokDetailAdapter
import com.zahwaalviana.simpro.ui.varian.adapter.VarianAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VarianListFragment : Fragment() {

    private var _binding: FragmentVarianListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: VarianAdapter
    private var varianListener: ListenerRegistration? = null
    
    private val allVarianList = mutableListOf<Pair<String, BarangVarian>>()
    private val displayList = mutableListOf<Pair<String, BarangVarian>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVarianListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupListeners()
        loadVarianData()
    }

    private fun setupRecyclerView() {
        adapter = VarianAdapter(displayList) { barangNama, varian ->
            showStokDetailDialog(varian, barangNama)
        }
        binding.rvVarian.layoutManager = LinearLayoutManager(requireContext())
        binding.rvVarian.adapter = adapter
    }

    private fun setupListeners() {
        binding.swipeRefresh.setOnRefreshListener {
            loadVarianData()
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                applyFilter()
            }
        })
    }

    private fun applyFilter() {
        val query = binding.etSearch.text.toString().trim()
        val filtered = if (query.isEmpty()) {
            allVarianList
        } else {
            allVarianList.filter { (barangNama, varian) ->
                barangNama.contains(query, ignoreCase = true) ||
                varian.kemasanNama.contains(query, ignoreCase = true)
            }
        }

        displayList.clear()
        displayList.addAll(filtered)
        adapter.notifyDataSetChanged()
        updateEmptyState()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadVarianData() {
        showLoading(true)

        varianListener?.remove()
        varianListener = db.collection("barang_varian")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val varianDocs = snapshots?.documents ?: emptyList()
                if (varianDocs.isEmpty()) {
                    showLoading(false)
                    binding.swipeRefresh.isRefreshing = false
                    allVarianList.clear()
                    applyFilter()
                    return@addSnapshotListener
                }

                val tempVarianList = mutableListOf<Pair<String, BarangVarian>>()
                var processedCount = 0

                varianDocs.forEach { doc ->
                    val barangId = doc.getString("barang_id") ?: ""
                    val kemasanId = doc.getString("kemasan_id") ?: ""
                    
                    db.collection("master_barang").document(barangId).get()
                        .addOnSuccessListener { barangDoc ->
                            val barangNama = barangDoc.getString("nama_barang") ?: "Unknown Barang"
                            
                            db.collection("master_kemasan").document(kemasanId).get()
                                .addOnSuccessListener { kemasanDoc ->
                                    val kemasanNama = kemasanDoc.getString("nama_kemasan") ?: ""
                                    val satuan = kemasanDoc.getString("satuan") ?: ""
                                    
                                    val varian = BarangVarian(
                                        id = doc.id,
                                        barangId = barangId,
                                        kemasanId = kemasanId,
                                        kemasanNama = kemasanNama,
                                        kemasanSatuan = satuan,
                                        stok = doc.getLong("stok")?.toInt() ?: 0,
                                        hargaJual = doc.getLong("harga_jual")?.toInt() ?: 0,
                                        shelfLifeHari = doc.getLong("shelf_life_hari")?.toInt() ?: 0
                                    )
                                    
                                    tempVarianList.add(barangNama to varian)
                                    processedCount++
                                    
                                    if (processedCount == varianDocs.size) {
                                        showLoading(false)
                                        binding.swipeRefresh.isRefreshing = false
                                        allVarianList.clear()
                                        allVarianList.addAll(tempVarianList.sortedByDescending { it.second.stok })
                                        applyFilter()
                                    }
                                }
                                .addOnFailureListener {
                                    processedCount++
                                    if (processedCount == varianDocs.size) {
                                        showLoading(false)
                                        binding.swipeRefresh.isRefreshing = false
                                        allVarianList.clear()
                                        allVarianList.addAll(tempVarianList)
                                        applyFilter()
                                    }
                                }
                        }
                        .addOnFailureListener {
                            processedCount++
                            if (processedCount == varianDocs.size) {
                                showLoading(false)
                                binding.swipeRefresh.isRefreshing = false
                                applyFilter()
                            }
                        }
                }
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

        AlertDialog.Builder(requireContext())
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

                    db.collection("produksi").document(produksiId).get()
                        .addOnSuccessListener { produksiDoc ->
                            val tanggal = produksiDoc.getLong("tanggal_produksi") ?: 0L
                            val mandorId = produksiDoc.getString("mandor_id") ?: ""

                            db.collection("users").document(mandorId).get()
                                .addOnSuccessListener { mandorDoc ->
                                    val mandorName = mandorDoc.getString("name") ?: "Unknown"
                                    val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                                    
                                    detailList.add(StokDetailItem(mandorName, dateFormat.format(Date(tanggal)), jumlah, tanggal))
                                    processedItems++

                                    if (processedItems == totalItems) {
                                        progressBar.visibility = View.GONE
                                        detailList.sortByDescending { it.timestamp } 
                                        detailAdapter.notifyDataSetChanged()
                                    }
                                }
                                .addOnFailureListener {
                                    processedItems++
                                    if (processedItems == totalItems) progressBar.visibility = View.GONE
                                }
                        }
                        .addOnFailureListener {
                            processedItems++
                            if (processedItems == totalItems) progressBar.visibility = View.GONE
                        }
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                tvEmpty.visibility = View.VISIBLE
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
        varianListener?.remove()
        _binding = null
    }
}
