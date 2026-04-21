package com.zahwaalviana.simpro.ui.laporan_keuangan

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.databinding.FragmentLaporanBinding
import com.zahwaalviana.simpro.ui.laporan.LaporanAdapter
import com.zahwaalviana.simpro.ui.laporan.LaporanItem
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class LaporanKeuanganFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val sdfIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val db = FirebaseFirestore.getInstance()
    
    private val listItems = mutableListOf<LaporanItem>()
    private lateinit var adapter: LaporanAdapter
    private val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    data class FinanceRecord(
        val date: Long,
        val description: String,
        val income: Int = 0,
        val expense: Int = 0
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()

        binding.btnStartDate.setOnClickListener {
            showDatePicker { cal ->
                startDate = cal
                binding.btnStartDate.text = "Dari: ${sdf.format(cal.time)}"
                tryFetch()
            }
        }

        binding.btnEndDate.setOnClickListener {
            showDatePicker { cal ->
                endDate = cal
                binding.btnEndDate.text = "Sampai: ${sdf.format(cal.time)}"
                tryFetch()
            }
        }

        binding.btnExportPdf.setOnClickListener {
            if (startDate == null || endDate == null) {
                Toast.makeText(context, "Pilih rentang tanggal terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fetchData(true)
        }
    }

    private fun tryFetch() {
        if (startDate != null && endDate != null) {
            fetchData(false)
        }
    }

    private fun setupRecyclerView() {
        adapter = LaporanAdapter(listItems)
        binding.rvLaporan.layoutManager = LinearLayoutManager(context)
        binding.rvLaporan.adapter = adapter
    }

    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val cal = Calendar.getInstance()
            cal.set(y, m, d, 0, 0, 0)
            onDateSelected(cal)
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
    }

    private fun fetchData(isExportPdf: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Mengambil data keuangan..."

        val startTs = startDate!!.timeInMillis
        val endTs = endDate!!.timeInMillis + 86400000

        val financeRecords = mutableListOf<FinanceRecord>()
        
        // Fetch Penjualan (Pemasukan)
        db.collection("penjualan")
            .whereGreaterThanOrEqualTo("tanggal", startTs)
            .whereLessThanOrEqualTo("tanggal", endTs)
            .get().addOnSuccessListener { penjualanDocs ->
                penjualanDocs.forEach { doc ->
                    val tgl = doc.getLong("tanggal") ?: 0L
                    val harga = doc.getLong("total_harga")?.toInt() ?: 0
                    financeRecords.add(FinanceRecord(tgl, "Penjualan Produk", income = harga))
                }

                // Fetch Pengeluaran
                db.collection("pengeluaran").get().addOnSuccessListener { pengeluaranDocs ->
                    pengeluaranDocs.forEach { doc ->
                        val tglStr = doc.getString("tanggal") ?: ""
                        try {
                            val date = sdfIso.parse(tglStr)
                            if (date != null && date.time >= startTs && date.time <= endTs) {
                                financeRecords.add(FinanceRecord(
                                    date.time, 
                                    doc.getString("keterangan") ?: "Pengeluaran", 
                                    expense = doc.getLong("biaya")?.toInt() ?: 0
                                ))
                            }
                        } catch (e: Exception) {}
                    }
                    processRecords(financeRecords, isExportPdf)
                }.addOnFailureListener {
                    processRecords(financeRecords, isExportPdf)
                }
            }.addOnFailureListener {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Gagal mengambil data penjualan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun processRecords(records: List<FinanceRecord>, isExportPdf: Boolean) {
        val sorted = records.sortedBy { it.date }
        listItems.clear()
        
        var totalIncome = 0
        var totalExpense = 0
        
        sorted.forEach { record ->
            totalIncome += record.income
            totalExpense += record.expense
            
            val type = if (record.income > 0) "PEMASUKAN" else "PENGELUARAN"
            val amount = if (record.income > 0) record.income else record.expense
            
            listItems.add(LaporanItem.PenjualanUI(
                "$type: ${record.description}",
                record.date,
                "Jumlah: ${formatter.format(amount)}"
            ))
        }

        binding.cardSummary.visibility = View.VISIBLE
        binding.tvSummaryProduksi.text = "Total Pemasukan: ${formatter.format(totalIncome)}"
        binding.tvSummaryPenjualan.visibility = View.VISIBLE
        binding.tvSummaryPenjualan.text = "Total Pengeluaran: ${formatter.format(totalExpense)}"
        
        val saldo = totalIncome - totalExpense
        binding.tvStatus.text = "Saldo Akhir: ${formatter.format(saldo)}"
        binding.tvStatus.setTextColor(if (saldo >= 0) Color.BLACK else Color.RED)
        
        binding.progressBar.visibility = View.GONE
        adapter.notifyDataSetChanged()

        if (isExportPdf && sorted.isNotEmpty()) {
            generateFinancePdf(sorted)
        } else if (isExportPdf) {
            Toast.makeText(context, "Tidak ada data untuk diekspor", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateFinancePdf(list: List<FinanceRecord>) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        val textPaint = Paint().apply { textSize = 10f }
        val headerPaint = Paint().apply { textSize = 10f; isFakeBoldText = true }
        
        var y = 50f
        
        // Title
        paint.textSize = 16f
        paint.isFakeBoldText = true
        canvas.drawText("LAPORAN KEUANGAN SIMPRO", 180f, y, paint)
        y += 20f
        paint.textSize = 12f
        canvas.drawText("Periode: ${sdf.format(startDate!!.time)} - ${sdf.format(endDate!!.time)}", 40f, y, paint)
        y += 40f

        // Table Header
        val cols = floatArrayOf(40f, 120f, 280f, 380f, 480f) // Tgl, Ket, Masuk, Keluar, Saldo
        canvas.drawLine(40f, y-15, 550f, y-15, paint)
        canvas.drawText("Tanggal", cols[0], y, headerPaint)
        canvas.drawText("Keterangan", cols[1], y, headerPaint)
        canvas.drawText("Pemasukan", cols[2], y, headerPaint)
        canvas.drawText("Pengeluaran", cols[3], y, headerPaint)
        canvas.drawText("Saldo", cols[4], y, headerPaint)
        y += 10f
        canvas.drawLine(40f, y, 550f, y, paint)
        y += 20f

        var runningSaldo = 0
        var totalIn = 0
        var totalOut = 0

        list.forEach { rec ->
            runningSaldo += (rec.income - rec.expense)
            totalIn += rec.income
            totalOut += rec.expense
            
            canvas.drawText(sdf.format(Date(rec.date)), cols[0], y, textPaint)
            canvas.drawText(rec.description, cols[1], y, textPaint)
            if(rec.income > 0) canvas.drawText(formatter.format(rec.income), cols[2], y, textPaint)
            if(rec.expense > 0) canvas.drawText(formatter.format(rec.expense), cols[3], y, textPaint)
            canvas.drawText(formatter.format(runningSaldo), cols[4], y, textPaint)
            
            y += 20f
            if (y > 780) { /* skip multi-page logic for simplicity */ }
        }

        y += 10f
        canvas.drawLine(40f, y, 550f, y, paint)
        y += 20f
        canvas.drawText("TOTAL", cols[1], y, headerPaint)
        canvas.drawText(formatter.format(totalIn), cols[2], y, headerPaint)
        canvas.drawText(formatter.format(totalOut), cols[3], y, headerPaint)
        
        y += 30f
        canvas.drawText("Sisa Saldo: ${formatter.format(runningSaldo)}", 40f, y, headerPaint)
        val status = if (runningSaldo >= 0) "POSITIF" else "NEGATIF"
        canvas.drawText("Kondisi Keuangan: $status", 40f, y + 20, headerPaint)

        pdf.finishPage(page)

        val fileName = "Laporan_Keuangan_${System.currentTimeMillis()}.pdf"
        savePdf(pdf, fileName)
    }

    private fun savePdf(pdf: PdfDocument, fileName: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = requireContext().contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Simpro")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { pdf.writeTo(it) }
                    showSuccessDialog(uri, fileName)
                }
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val simproDir = File(downloadsDir, "Simpro")
                if (!simproDir.exists()) simproDir.mkdirs()
                val file = File(simproDir, fileName)
                pdf.writeTo(FileOutputStream(file))
                val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.provider", file)
                showSuccessDialog(uri, fileName)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Gagal simpan PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdf.close()
        }
    }

    private fun showSuccessDialog(uri: Uri, fileName: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Berhasil")
            .setMessage("Laporan berhasil disimpan: $fileName")
            .setPositiveButton("Buka PDF") { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/pdf")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(context, "Tidak ada aplikasi untuk membuka PDF", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Tutup", null).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
