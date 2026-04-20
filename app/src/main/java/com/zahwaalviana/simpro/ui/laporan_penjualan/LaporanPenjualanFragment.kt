package com.zahwaalviana.simpro.ui.laporan_penjualan

import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.zahwaalviana.simpro.data.model.Penjualan
import com.zahwaalviana.simpro.data.model.PenjualanItem
import com.zahwaalviana.simpro.databinding.FragmentLaporanBinding
import com.zahwaalviana.simpro.ui.laporan.LaporanAdapter
import com.zahwaalviana.simpro.ui.laporan.LaporanItem
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class LaporanPenjualanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val db = FirebaseFirestore.getInstance()
    
    private val listItems = mutableListOf<LaporanItem>()
    private lateinit var adapter: LaporanAdapter
    
    private val rawData = mutableListOf<Pair<Penjualan, List<PenjualanItem>>>()
    private val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

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
            fetchDataAndProcess(true)
        }
    }

    private fun tryFetch() {
        if (startDate != null && endDate != null) {
            fetchDataAndProcess(false)
        }
    }

    private fun setupRecyclerView() {
        adapter = LaporanAdapter(listItems) { item ->
            showDetailDialog(item)
        }
        binding.rvLaporan.layoutManager = LinearLayoutManager(context)
        binding.rvLaporan.adapter = adapter
    }

    private fun showDetailDialog(item: LaporanItem) {
        val id = when(item) {
            is LaporanItem.PenjualanUI -> item.id
            else -> ""
        }
        
        val data = rawData.find { it.first.id == id } ?: return
        val (penjualan, items) = data

        val details = StringBuilder()
        items.forEach { 
            details.append("${it.barangNama}\n${it.jumlah} x ${formatter.format(it.hargaSatuan)} = ${formatter.format(it.subtotal)}\n\n")
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Detail Transaksi")
            .setMessage("Tanggal: ${sdf.format(Date(penjualan.tanggal))}\n\n$details" +
                    "TOTAL: ${formatter.format(penjualan.totalHarga)}")
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun showDatePicker(onDateSelected: (Calendar) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selectedDate = Calendar.getInstance()
                selectedDate.set(year, month, dayOfMonth, 0, 0, 0)
                onDateSelected(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchDataAndProcess(isExportPdf: Boolean) {
        binding.progressBar.visibility = View.VISIBLE
        binding.tvStatus.text = "Mengambil data..."

        val startTs = startDate!!.timeInMillis
        val endTs = endDate!!.timeInMillis + 86400000

        db.collection("penjualan")
            .whereGreaterThanOrEqualTo("tanggal", startTs)
            .whereLessThanOrEqualTo("tanggal", endTs)
            .get().addOnSuccessListener { docs ->
                val listPenjualan = mutableListOf<Pair<Penjualan, List<PenjualanItem>>>()
                val totalDocs = docs.size()
                var processedDocs = 0

                if (totalDocs == 0) {
                    updateUI(emptyList(), isExportPdf)
                    return@addOnSuccessListener
                }

                docs.forEach { doc ->
                    val penjualan = Penjualan(
                        id = doc.id,
                        tanggal = doc.getLong("tanggal") ?: 0L,
                        totalHarga = doc.getLong("total_harga")?.toInt() ?: 0,
                        totalBayar = doc.getLong("total_bayar")?.toInt() ?: 0,
                        kembalian = doc.getLong("kembalian")?.toInt() ?: 0
                    )
                    
                    loadPenjualanItems(doc.id) { items ->
                        listPenjualan.add(penjualan to items)
                        processedDocs++
                        if (processedDocs == totalDocs) {
                            updateUI(listPenjualan, isExportPdf)
                        }
                    }
                }
            }.addOnFailureListener { e ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadPenjualanItems(penjualanId: String, onComplete: (List<PenjualanItem>) -> Unit) {
        db.collection("penjualan_items")
            .whereEqualTo("penjualan_id", penjualanId)
            .get()
            .addOnSuccessListener { itemDocs ->
                val items = mutableListOf<PenjualanItem>()
                val totalItems = itemDocs.size()
                var processedItems = 0

                if (totalItems == 0) {
                    onComplete(emptyList())
                    return@addOnSuccessListener
                }

                itemDocs.documents.forEach { itemDoc ->
                    var bNama = itemDoc.getString("barang_nama") ?: ""
                    var kNama = itemDoc.getString("kemasan_nama") ?: ""
                    
                    val varianId = itemDoc.getString("varian_id") ?: ""
                    val subtotal = itemDoc.getLong("subtotal")?.toInt() ?: 0
                    val jumlah = itemDoc.getLong("jumlah")?.toInt() ?: 0
                    val hargaSatuan = itemDoc.getLong("harga_satuan")?.toInt() ?: 0

                    // Jika barang_nama kosong (transaksi lama), ambil dari master_barang via varian
                    if (bNama.isEmpty() && varianId.isNotEmpty()) {
                        db.collection("barang_varian").document(varianId).get().addOnSuccessListener { vDoc ->
                            val barangId = vDoc.getString("barang_id") ?: ""
                            val kemasanId = vDoc.getString("kemasan_id") ?: ""

                            loadNames(barangId, kemasanId) { namaB, namaK ->
                                items.add(PenjualanItem(itemDoc.id, penjualanId, varianId, namaB, namaK, hargaSatuan, jumlah, subtotal))
                                processedItems++
                                if (processedItems == totalItems) onComplete(items)
                            }
                        }.addOnFailureListener {
                            processedItems++
                            if (processedItems == totalItems) onComplete(items)
                        }
                    } else {
                        items.add(PenjualanItem(itemDoc.id, penjualanId, varianId, bNama, kNama, hargaSatuan, jumlah, subtotal))
                        processedItems++
                        if (processedItems == totalItems) onComplete(items)
                    }
                }
            }
            .addOnFailureListener { onComplete(emptyList()) }
    }

    private fun loadNames(bId: String, kId: String, callback: (String, String) -> Unit) {
        var bNama = ""
        var kNama = ""
        var done = 0
        db.collection("master_barang").document(bId).get().addOnSuccessListener { 
            bNama = it.getString("nama_barang") ?: "Unknown"
            if (++done == 2) callback(bNama, kNama)
        }.addOnFailureListener { if (++done == 2) callback(bNama, kNama) }
        
        db.collection("master_kemasan").document(kId).get().addOnSuccessListener { 
            kNama = it.getString("nama_kemasan") ?: ""
            if (++done == 2) callback(bNama, kNama)
        }.addOnFailureListener { if (++done == 2) callback(bNama, kNama) }
    }

    private fun updateUI(list: List<Pair<Penjualan, List<PenjualanItem>>>, isExportPdf: Boolean) {
        val sortedList = list.sortedByDescending { it.first.tanggal }
        rawData.clear()
        rawData.addAll(sortedList)
        
        listItems.clear()
        var totalPendapatan = 0
        
        sortedList.forEach { (penjualan, items) ->
            totalPendapatan += penjualan.totalHarga
            val info = items.joinToString { "${it.barangNama} x${it.jumlah}" }
            listItems.add(LaporanItem.PenjualanUI(
                "Total: ${formatter.format(penjualan.totalHarga)}",
                penjualan.tanggal,
                "Items: $info",
                penjualan.id
            ))
        }
        
        adapter.notifyDataSetChanged()
        binding.cardSummary.visibility = View.VISIBLE
        binding.tvSummaryProduksi.text = "Total Transaksi: ${sortedList.size}"
        binding.tvSummaryPenjualan.visibility = View.VISIBLE
        binding.tvSummaryPenjualan.text = "Total Pendapatan: ${formatter.format(totalPendapatan)}"
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.text = "Data periode ${sdf.format(startDate!!.time)} - ${sdf.format(endDate!!.time)}"
        
        if (isExportPdf && sortedList.isNotEmpty()) {
            generatePdf(sortedList, totalPendapatan)
        } else if (isExportPdf) {
            Toast.makeText(context, "Tidak ada data untuk di-export", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generatePdf(list: List<Pair<Penjualan, List<PenjualanItem>>>, totalPendapatan: Int) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        var y = 40f

        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("LAPORAN PENJUALAN SIMPRO", 150f, y, paint)
        y += 30f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Periode: ${sdf.format(startDate!!.time)} - ${sdf.format(endDate!!.time)}", 40f, y, paint)
        y += 40f

        paint.isFakeBoldText = true
        canvas.drawText("RINCIAN PENJUALAN", 40f, y, paint)
        y += 20f
        
        list.forEach { (penjualan, items) ->
            paint.isFakeBoldText = true
            paint.textSize = 12f
            canvas.drawText("${sdf.format(Date(penjualan.tanggal))} | TOTAL: ${formatter.format(penjualan.totalHarga)}", 40f, y, paint)
            y += 15f
            
            paint.isFakeBoldText = false
            paint.textSize = 10f
            items.forEach { item ->
                canvas.drawText("- ${item.barangNama} x${item.jumlah} @${formatter.format(item.hargaSatuan)} = ${formatter.format(item.subtotal)}", 50f, y, paint)
                y += 14f
            }
            y += 10f
            
            if (y > 780) { /* Simplified page break check */ }
        }
        
        y += 10f
        paint.textSize = 14f
        paint.isFakeBoldText = true
        canvas.drawText("TOTAL PENDAPATAN: ${formatter.format(totalPendapatan)}", 40f, y, paint)
        pdf.finishPage(page)

        val fileName = "Laporan_Penjualan_${System.currentTimeMillis()}.pdf"
        
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
                    resolver.openOutputStream(it)?.use { outputStream ->
                        pdf.writeTo(outputStream)
                    }
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
            e.printStackTrace()
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
            .setNegativeButton("Tutup", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
