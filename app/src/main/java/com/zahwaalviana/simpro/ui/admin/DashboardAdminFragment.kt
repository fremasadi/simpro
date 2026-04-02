package com.zahwaalviana.simpro.ui.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.card.MaterialCardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.databinding.FragmentDashboardAdminBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DashboardAdminFragment : Fragment() {

    private var _binding: FragmentDashboardAdminBinding? = null
    private val binding get() = _binding!!

    private val db = FirebaseFirestore.getInstance()
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    private val timeFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDashboardAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.tvGreetingDate.text = dateFormat.format(Date())
        showLoading(true)
        loadAllStats()
    }

    private fun loadAllStats() {
        loadPenjualanStats()
        loadPengeluaranStats()
        loadProduksiStats()
        loadRecentPenjualan()
    }

    private fun loadPenjualanStats() {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        db.collection("penjualan")
            .whereGreaterThanOrEqualTo("tanggal", startOfDay)
            .get()
            .addOnSuccessListener { docs ->
                var total = 0
                docs.forEach { total += it.getLong("total_harga")?.toInt() ?: 0 }
                binding.tvPenjualanHariIni.text = currencyFormat.format(total)
                binding.tvTransaksiCount.text = "${docs.size()} transaksi"
            }
    }

    private fun loadPengeluaranStats() {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        val monthPrefix = "$year-${month.toString().padStart(2, '0')}"

        db.collection("pengeluaran")
            .get()
            .addOnSuccessListener { docs ->
                var total = 0
                docs.forEach { doc ->
                    val tanggal = doc.getString("tanggal") ?: ""
                    if (tanggal.startsWith(monthPrefix)) {
                        total += doc.getLong("biaya")?.toInt() ?: 0
                    }
                }
                binding.tvPengeluaranBulanIni.text = currencyFormat.format(total)
            }
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
            .get()
            .addOnSuccessListener { docs ->
                val hariIni = docs.filter { (it.getLong("tanggalProduksi") ?: 0L) >= startOfDay }.size
                val bulanIni = docs.filter { (it.getLong("tanggalProduksi") ?: 0L) >= startOfMonth }.size
                binding.tvProduksiHariIni.text = "$hariIni batch"
                binding.tvProduksiBulanIni.text = "$bulanIni batch"
            }
    }

    private fun loadRecentPenjualan() {
        db.collection("penjualan")
            .orderBy("tanggal", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { docs ->
                showLoading(false)
                binding.llRecentSales.removeAllViews()

                if (docs.isEmpty) {
                    val tv = TextView(requireContext()).apply {
                        text = "Belum ada transaksi"
                        setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
                        textSize = 13f
                        setPadding(0, 8, 0, 8)
                    }
                    binding.llRecentSales.addView(tv)
                    return@addOnSuccessListener
                }

                docs.forEach { doc ->
                    val itemView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.item_dashboard_recent, binding.llRecentSales, false)

                    val tanggal = doc.getLong("tanggal") ?: 0L
                    val totalHarga = doc.getLong("total_harga")?.toInt() ?: 0

                    itemView.findViewById<TextView>(R.id.tvRecentTitle).text =
                        timeFormat.format(Date(tanggal))
                    itemView.findViewById<TextView>(R.id.tvRecentSubtitle).text =
                        "Bayar: ${currencyFormat.format(doc.getLong("total_bayar")?.toInt() ?: 0)}"
                    itemView.findViewById<TextView>(R.id.tvRecentAmount).text =
                        currencyFormat.format(totalHarga)

                    binding.llRecentSales.addView(itemView)
                }
            }
            .addOnFailureListener { showLoading(false) }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
