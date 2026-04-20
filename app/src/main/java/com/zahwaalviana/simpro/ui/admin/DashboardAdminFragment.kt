package com.zahwaalviana.simpro.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
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

    private var startTs: Long = 0L
    private var endTs: Long = 0L

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
        
        setupFilter()
        loadWithFilter("today")
    }

    private fun setupFilter() {
        binding.chipGroupFilter.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.chipToday -> loadWithFilter("today")
                R.id.chipMonth -> loadWithFilter("month")
                R.id.chipCustom -> showDateRangePicker()
            }
        }
    }

    private fun loadWithFilter(type: String) {
        val cal = Calendar.getInstance()
        when (type) {
            "today" -> {
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startTs = cal.timeInMillis
                endTs = System.currentTimeMillis()
                binding.tvProduksiLabel.text = "batch hari ini"
            }
            "month" -> {
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0)
                cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                startTs = cal.timeInMillis
                endTs = System.currentTimeMillis()
                binding.tvProduksiLabel.text = "batch bulan ini"
            }
        }
        loadAllStats()
    }

    private fun showDateRangePicker() {
        val cal = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, year, month, day ->
            val startCal = Calendar.getInstance().apply {
                set(year, month, day, 0, 0, 0)
            }
            startTs = startCal.timeInMillis
            
            DatePickerDialog(requireContext(), { _, y2, m2, d2 ->
                val endCal = Calendar.getInstance().apply {
                    set(y2, m2, d2, 23, 59, 59)
                }
                endTs = endCal.timeInMillis
                binding.tvProduksiLabel.text = "batch custom"
                loadAllStats()
            }, year, month, day).apply {
                setTitle("Sampai Tanggal")
                show()
            }
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).apply {
            setTitle("Dari Tanggal")
            show()
        }
    }

    private fun loadAllStats() {
        showLoading(true)
        loadPenjualanStats()
        loadPengeluaranStats()
        loadProduksiStats()
        loadRecentPenjualan()
    }

    private fun loadPenjualanStats() {
        db.collection("penjualan")
            .whereGreaterThanOrEqualTo("tanggal", startTs)
            .whereLessThanOrEqualTo("tanggal", endTs)
            .get()
            .addOnSuccessListener { docs ->
                var total = 0
                docs.forEach { total += it.getLong("total_harga")?.toInt() ?: 0 }
                binding.tvPenjualanStat.text = currencyFormat.format(total)
                binding.tvTransaksiCount.text = "${docs.size()} transaksi"
            }
    }

    private fun loadPengeluaranStats() {
        // Pengeluaran di simpro menggunakan format String "yyyy-MM-dd"
        // Kita perlu konversi startTs & endTs ke format tersebut untuk filter (atau ambil semua lalu filter di client)
        val sdfFilter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val startStr = sdfFilter.format(Date(startTs))
        val endStr = sdfFilter.format(Date(endTs))

        db.collection("pengeluaran")
            .get()
            .addOnSuccessListener { docs ->
                var total = 0
                docs.forEach { doc ->
                    val tgl = doc.getString("tanggal") ?: ""
                    if (tgl in startStr..endStr) {
                        total += doc.getLong("biaya")?.toInt() ?: 0
                    }
                }
                binding.tvPengeluaranStat.text = currencyFormat.format(total)
            }
    }

    private fun loadProduksiStats() {
        db.collection("produksi")
            .whereGreaterThanOrEqualTo("tanggal_produksi", startTs)
            .whereLessThanOrEqualTo("tanggal_produksi", endTs)
            .get()
            .addOnSuccessListener { docs ->
                binding.tvProduksiStat.text = "${docs.size()} batch"
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
