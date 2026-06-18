package com.zahwaalviana.simpro.ui.mandor

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.databinding.FragmentDashboardMandorBinding
import com.zahwaalviana.simpro.ui.barang.BarangListFragment
import com.zahwaalviana.simpro.ui.kemasan.KemasanListFragment
import com.zahwaalviana.simpro.ui.produksi.ProduksiListFragment
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

    private var startTs: Long = 0L
    private var endTs: Long = 0L
    private var mandorId: String = ""

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

        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email ?: ""
        if (email.isNotEmpty()) {
            val name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
            binding.tvGreetingName.text = "Selamat Datang, $name!"
        }
        binding.tvGreetingDate.text = dateFormat.format(Date())
        
        mandorId = currentUser?.uid ?: ""

        setupFilter()
        setupMenuListeners()
        loadWithFilter("today")
    }

    private fun setupMenuListeners() {
        binding.btnMenuProduksi.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container_mandor, ProduksiListFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuBarang.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container_mandor, BarangListFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnMenuKemasan.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.container_mandor, KemasanListFragment())
                .addToBackStack(null)
                .commit()
        }
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
        if (mandorId.isNotEmpty()) loadAllStats()
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
        loadProduksiStats()
    }

    private fun loadProduksiStats() {
        db.collection("produksi")
            .whereEqualTo("mandor_id", mandorId) // Perbaikan field: mandor_id
            .whereGreaterThanOrEqualTo("tanggal_produksi", startTs) // Perbaikan field: tanggal_produksi
            .whereLessThanOrEqualTo("tanggal_produksi", endTs)
            .get()
            .addOnSuccessListener { docs ->
                val sorted = docs.documents.sortedByDescending { it.getLong("tanggal_produksi") ?: 0L }
                binding.tvProduksiStat.text = "${sorted.size} batch"

                loadTotalItemStat(sorted.map { it.id })
                showRecentProduksi(sorted.take(5).map { doc ->
                    Triple(
                        doc.getLong("tanggal_produksi") ?: 0L,
                        doc.getString("mandor_name") ?: "-", // Perbaikan field: mandor_name
                        doc.id
                    )
                })
            }
            .addOnFailureListener { showLoading(false) }
    }

    private fun loadTotalItemStat(produksiIds: List<String>) {
        if (produksiIds.isEmpty()) {
            binding.tvTotalItemStat.text = "0 item"
            showLoading(false)
            return
        }

        var totalItem = 0
        var completed = 0

        produksiIds.forEach { produksiId ->
            db.collection("produksi_items")
                .whereEqualTo("produksi_id", produksiId)
                .get()
                .addOnSuccessListener { items ->
                    items.forEach { totalItem += it.getLong("jumlah_produksi")?.toInt() ?: 0 } // Perbaikan field: jumlah_produksi
                    completed++
                    if (completed == produksiIds.size) {
                        binding.tvTotalItemStat.text = "$totalItem item"
                        showLoading(false)
                    }
                }
                .addOnFailureListener {
                    completed++
                    if (completed == produksiIds.size) {
                        binding.tvTotalItemStat.text = "$totalItem item"
                        showLoading(false)
                    }
                }
        }
    }

    private fun showRecentProduksi(items: List<Triple<Long, String, String>>) {
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
//            itemView.findViewById<TextView>(R.id.tvRecentAmount).apply {
//                text = "Lihat"
//                setTextColor(android.graphics.Color.parseColor("#64B5F6"))
//            }

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
