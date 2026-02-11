package com.zahwaalviana.simpro.ui.pengeluaran

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
import com.zahwaalviana.simpro.data.model.MasterPengeluaran
import com.zahwaalviana.simpro.data.model.Pengeluaran
import com.zahwaalviana.simpro.databinding.FragmentPengeluaranListBinding
import com.zahwaalviana.simpro.ui.pengeluaran.adapter.PengeluaranAdapter

class PengeluaranListFragment : Fragment() {

    private var _binding: FragmentPengeluaranListBinding? = null
    private val binding get() = _binding!!

    private val db by lazy { FirebaseFirestore.getInstance() }

    private val pengeluaranList = mutableListOf<Pengeluaran>()
    private val masterMap = mutableMapOf<String, Pair<String, String>>()

    private lateinit var adapter: PengeluaranAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentPengeluaranListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()
        setupRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        loadMasterPengeluaran()
    }

    private fun setupToolbar() {
        // Toolbar sudah diatur di MainActivity, jadi hide toolbar fragment
        binding.appBarLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = PengeluaranAdapter(
            pengeluaranList,
            masterMap,
            onEdit = {
                Toast.makeText(requireContext(), "Edit ${it.id}", Toast.LENGTH_SHORT).show()
            },
            onDelete = {
                confirmDelete(it)
            }
        )

        binding.rvPengeluaran.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPengeluaran.adapter = adapter
        binding.fabTambah.setOnClickListener {
            startActivity(
                Intent(requireActivity(), PengeluaranFormActivity::class.java)
            )
        }
    }

    private fun loadMasterPengeluaran() {
        db.collection("master_pengeluaran")
            .get()
            .addOnSuccessListener { snapshot ->
                masterMap.clear()
                snapshot.documents.forEach {
                    val nama = it.getString("nama_pengeluaran") ?: ""
                    val kategori = it.getString("kategori") ?: ""
                    masterMap[it.id] = Pair(nama, kategori)
                }
                loadPengeluaran()
            }
            .addOnFailureListener {
                toast(it.message)
            }
    }

    private fun loadPengeluaran() {
        db.collection("pengeluaran")
            .orderBy("tanggal")
            .get()
            .addOnSuccessListener { snapshot ->
                pengeluaranList.clear()
                snapshot.documents.forEach { doc ->
                    pengeluaranList.add(
                        Pengeluaran(
                            id = doc.id,
                            masterPengeluaranId = doc.getString("master_pengeluaran_id") ?: "",
                            produksiId = doc.getString("produksi_id") ?: "",
                            tanggal = doc.getString("tanggal") ?: "",
                            keterangan = doc.getString("keterangan") ?: "",
                            biaya = doc.getLong("biaya")?.toInt() ?: 0
                        )
                    )
                }
                adapter.notifyDataSetChanged()
                updateEmptyState()
            }
            .addOnFailureListener {
                toast(it.message)
            }
    }

    private fun confirmDelete(item: Pengeluaran) {
        AlertDialog.Builder(requireContext())
            .setTitle("Hapus Pengeluaran")
            .setMessage("Yakin ingin menghapus data ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deletePengeluaran(item.id)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePengeluaran(id: String) {
        db.collection("pengeluaran")
            .document(id)
            .delete()
            .addOnSuccessListener {
                toast("Berhasil dihapus")
                loadPengeluaran()
            }
            .addOnFailureListener {
                toast(it.message)
            }
    }

    private fun updateEmptyState() {
        binding.tvEmpty.visibility =
            if (pengeluaranList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun toast(msg: String?) {
        Toast.makeText(requireContext(), msg ?: "Error", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}