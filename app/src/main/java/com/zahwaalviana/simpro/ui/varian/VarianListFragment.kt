package com.zahwaalviana.simpro.ui.varian

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.databinding.FragmentVarianListBinding
import com.zahwaalviana.simpro.ui.varian.adapter.VarianAdapter

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
        adapter = VarianAdapter(displayList)
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
        // Listen to varian changes
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
                    
                    // Fetch barang name and kemasan details
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
                                        allVarianList.addAll(tempVarianList.sortedByDescending { it.second.stok }) // Sort by stock as default
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
