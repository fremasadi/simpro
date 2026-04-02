package com.zahwaalviana.simpro.ui.mandor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.databinding.FragmentDashboardMandorBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardMandorFragment : Fragment() {

    private var _binding: FragmentDashboardMandorBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardMandorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        if (email.isNotEmpty()) {
            val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            binding.tvGreetingName.text = "Selamat Datang, $name!"
        }
        binding.tvGreetingDate.text = dateFormat.format(Date())

        showLoading(true)
        loadProduksiStats()
    }

    private fun loadProduksiStats() {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        db.collection("produksi")
            .orderBy("tanggalProduksi", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { docs ->
                val hariIni = docs.filter { (it.getLong("tanggalProduksi") ?: 0L) >= startOfDay }
                val bulanIni = docs.filter { (it.getLong("tanggalProduksi") ?: 0L) >= startOfMonth }

                binding.tvProduksiHariIni.text = "${hariIni.size} batch"
                binding.tvProduksiBulanIni.text = "${bulanIni.size} batch"

                loadTotalItemBulanIni(bulanIni.map { it.id })
                showRecentProduksi(docs.documents.take(5).map { doc ->
                    Triple(
                        doc.getLong("tanggalProduksi") ?: 0L,
                        doc.getString("mandorName") ?: "-",
                        doc.id
                    )
                })
            }
            .addOnFailureListener { showLoading(false) }
    }

    private fun loadTotalItemBulanIni(produksiIds: List<String>) {
        if (produksiIds.isEmpty()) {
            binding.tvTotalItemBulanIni.text = "0 item"
            return
        }

        var totalItem = 0
        var completed = 0

        produksiIds.forEach { produksiId ->
            db.collection("produksi_items")
                .whereEqualTo("produksi_id", produksiId)
                .get()
                .addOnSuccessListener { items ->
                    items.forEach { totalItem += it.getLong("jumlahProduksi")?.toInt() ?: 0 }
                    completed++
                    if (completed == produksiIds.size) {
                        binding.tvTotalItemBulanIni.text = "$totalItem item"
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed == produksiIds.size) {
                        binding.tvTotalItemBulanIni.text = "$totalItem item"
                    }
                }
        }
    }

    private fun showRecentProduksi(items: List<Triple<Long, String, String>>) {
        showLoading(false)
        binding.llRecentProduksi.removeAllViews()

        if (items.isEmpty()) {
            val tv = TextView(requireContext()).apply {
                text = "Belum ada data produksi"
                setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
                textSize = 13f
                setPadding(0, 8, 0, 8)
            }
            binding.llRecentProduksi.addView(tv)
            return
        }

        items.forEach { (tanggal, mandorName, id) ->
            val itemView = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_dashboard_recent, binding.llRecentProduksi, false)

            itemView.findViewById<TextView>(R.id.tvRecentTitle).text =
                timeFormat.format(Date(tanggal))
            itemView.findViewById<TextView>(R.id.tvRecentSubtitle).text = "Mandor: $mandorName"
            itemView.findViewById<TextView>(R.id.tvRecentAmount).apply {
                text = "Lihat"
                setTextColor(android.graphics.Color.parseColor("#64B5F6"))
            }

            binding.llRecentProduksi.addView(itemView)
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
