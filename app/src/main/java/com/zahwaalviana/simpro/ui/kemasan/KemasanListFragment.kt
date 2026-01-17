package com.zahwaalviana.simpro.ui.kemasan

import Kemasan
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
import com.zahwaalviana.simpro.ui.kemasan.adapter.KemasanAdapter
import com.zahwaalviana.simpro.databinding.FragmentKemasanListBinding


class KemasanListFragment : Fragment() {

    private var _binding: FragmentKemasanListBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: KemasanAdapter
    private var kemasanListener: ListenerRegistration? = null
    private val kemasanList = mutableListOf<Kemasan>()

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

        db = FirebaseFirestore.getInstance()

        setupToolbar()
        setupRecyclerView()
        setupListeners()
        loadKemasanData()
    }

    private fun setupToolbar() {
        // Toolbar sudah diatur di MainActivity, jadi hide toolbar fragment
        binding.appBarLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = KemasanAdapter(
            kemasanList,
            onEditClick = { kemasan ->
                val intent = Intent(requireContext(), KemasanFormActivity::class.java)
                intent.putExtra("KEMASAN_ID", kemasan.id)
                startActivity(intent)
            },
            onDeleteClick = { kemasan ->
                showDeleteConfirmation(kemasan)
            }
        )

        binding.rvKemasan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvKemasan.adapter = adapter
    }

    private fun setupListeners() {
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), KemasanFormActivity::class.java))
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadKemasanData()
        }
    }

    private fun loadKemasanData() {
        showLoading(true)

        kemasanListener = db.collection("master_kemasan")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                showLoading(false)
                binding.swipeRefresh.isRefreshing = false

                if (error != null) {
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                kemasanList.clear()
                snapshots?.documents?.forEach { doc ->
                    val kemasan = Kemasan(
                        id = doc.id,
                        namaKemasan = doc.getString("nama_kemasan") ?: "",
                        satuan = doc.getString("satuan") ?: "",
                        stok = doc.getDouble("stok") ?: 0.0
                    )
                    kemasanList.add(kemasan)
                }

                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
    }

    private fun showDeleteConfirmation(kemasan: Kemasan) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Kemasan")
            .setMessage("Apakah Anda yakin ingin menghapus ${kemasan.namaKemasan}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteKemasan(kemasan.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteKemasan(id: String) {
        db.collection("master_kemasan")
            .document(id)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Data berhasil dihapus", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateEmptyState() {
        if (kemasanList.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvKemasan.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvKemasan.visibility = View.VISIBLE
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        kemasanListener?.remove()
        _binding = null
    }
}