package com.zahwaalviana.simpro.ui.mandor.produksi

import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.zahwaalviana.simpro.data.model.Produksi
import com.zahwaalviana.simpro.databinding.FragmentProduksiListBinding

import com.zahwaalviana.simpro.ui.mandor.produksi.adapter.ProduksiAdapter

class ProduksiListFragment : Fragment() {

    private var _binding: FragmentProduksiListBinding? = null
    private val b get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private lateinit var adapter: ProduksiAdapter
    private var listener: ListenerRegistration? = null

    private var all = listOf<Produksi>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProduksiListBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupRecycler()
        setupSearch()
        setupActions()
        loadData()
    }

    private fun setupRecycler() {
        adapter = ProduksiAdapter(
            onEdit = { item ->
                val i = Intent(requireContext(), ProdukFormActivity::class.java)
                i.putExtra("PRODUKSI_DATA", item)
                startActivity(i)
            },
            onDelete = { item ->
                Toast.makeText(requireContext(), "Implement delete here", Toast.LENGTH_SHORT).show()
            }
        )

        b.recyclerViewProduksi.layoutManager = LinearLayoutManager(requireContext())
        b.recyclerViewProduksi.adapter = adapter
    }

    private fun setupSearch() {
        b.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(q: String?) = false
            override fun onQueryTextChange(q: String?): Boolean {
                filter(q ?: "")
                return true
            }
        })
    }

    private fun setupActions() {
        b.btnAddProduksi.setOnClickListener {
            startActivity(Intent(requireContext(), ProdukFormActivity::class.java))
        }

        b.swipeRefreshLayout.setOnRefreshListener { loadData() }
    }

    private fun loadData() {
        b.progressBar.visibility = View.VISIBLE
        listener?.remove()

        listener = db.collection("produksi_harian")
            .orderBy("tanggal")
            .addSnapshotListener { snap, e ->
                b.progressBar.visibility = View.GONE
                b.swipeRefreshLayout.isRefreshing = false

                if (e != null) return@addSnapshotListener

                if (snap != null && !snap.isEmpty) {
                    all = snap.documents.mapNotNull { d ->
                        d.toObject(Produksi::class.java)?.copy(produksiId = d.id)
                    }
                    adapter.submitData(all)
                    showEmpty(false)
                } else {
                    all = emptyList()
                    adapter.submitData(emptyList())
                    showEmpty(true)
                }
            }
    }

    private fun filter(q: String) {
        val filtered = all.filter {
            it.namaBarangJadi.contains(q, ignoreCase = true)
        }
        adapter.submitData(filtered)
        showEmpty(filtered.isEmpty())
    }

    private fun showEmpty(empty: Boolean) {
        b.emptyStateLayout.visibility = if (empty) View.VISIBLE else View.GONE
        b.recyclerViewProduksi.visibility = if (empty) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener?.remove()
        _binding = null
    }
}
