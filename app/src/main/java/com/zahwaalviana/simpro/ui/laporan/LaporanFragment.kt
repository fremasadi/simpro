package com.zahwaalviana.simpro.ui.laporan

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
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.zahwaalviana.simpro.data.model.Produksi
import com.zahwaalviana.simpro.data.model.ProduksiItem
import com.zahwaalviana.simpro.databinding.FragmentLaporanBinding
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class LaporanFragment : Fragment() {

    private var _binding: FragmentLaporanBinding? = null
    private val binding get() = _binding!!

    private var startDate: Calendar? = null
    private var endDate: Calendar? = null
    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    private var userRole: String = ""
    private var userId: String = ""

    private val listItems = mutableListOf<LaporanItem>()
    private lateinit var adapter: LaporanAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLaporanBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Hapus header Aksi untuk laporan produksi
        binding.tvHeaderAksi.visibility = View.GONE

        setupRecyclerView()
        checkUserRole()

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
        if (startDate != null && endDate != null && userRole.isNotEmpty()) {
            fetchDataAndProcess(false)
        }
    }

    private fun setupRecyclerView() {
        adapter = LaporanAdapter(listItems) { }
        binding.rvLaporan.layoutManager = LinearLayoutManager(context)
        binding.rvLaporan.adapter = adapter
    }

    private fun checkUserRole() {
        val uid = auth.currentUser?.uid ?: return
        userId = uid
        db.collection("users").document(uid).get().addOnSuccessListener { 
            userRole = it.getString("role") ?: ""
            tryFetch()
        }
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

        var query: Query = db.collection("produksi")
            .whereGreaterThanOrEqualTo("tanggal_produksi", startTs)
            .whereLessThanOrEqualTo("tanggal_produksi", endTs)

        if (userRole == "mandor") {
            query = query.whereEqualTo("mandor_id", userId)
        }

        query.get().addOnSuccessListener { docs ->
            val listProduksi = mutableListOf<Produksi>()
            val totalDocs = docs.size()
            var processedDocs = 0

            if (totalDocs == 0) {
                updateUI(emptyList(), isExportPdf)
                return@addOnSuccessListener
            }

            docs.forEach { doc ->
                val mandorId = doc.getString("mandor_id") ?: ""
                val prodId = doc.id
                val tglProduksi = doc.getLong("tanggal_produksi") ?: 0L

                db.collection("users").document(mandorId).get().addOnSuccessListener { userDoc ->
                    val mandorName = userDoc.getString("name") ?: "Unknown"
                    
                    db.collection("produksi_items").whereEqualTo("produksi_id", prodId).get().addOnSuccessListener { itemDocs ->
                        val items = mutableListOf<ProduksiItem>()
                        val totalItems = itemDocs.size()
                        var processedItems = 0

                        if (totalItems == 0) {
                            listProduksi.add(Produksi(prodId, tglProduksi, mandorId, mandorName, emptyList()))
                            processedDocs++
                            if (processedDocs == totalDocs) updateUI(listProduksi, isExportPdf)
                            return@addOnSuccessListener
                        }

                        itemDocs.forEach { itemDoc ->
                            val varianId = itemDoc.getString("varian_id") ?: ""
                            val qty = itemDoc.getLong("jumlah_produksi")?.toInt() ?: 0
                            val exp = itemDoc.getLong("expired_at") ?: 0L

                            db.collection("barang_varian").document(varianId).get().addOnSuccessListener { varianDoc ->
                                val bId = varianDoc.getString("barang_id") ?: ""
                                val kId = varianDoc.getString("kemasan_id") ?: ""
                                val shelfLife = varianDoc.getLong("shelf_life_hari")?.toInt() ?: 0

                                loadNames(bId, kId) { bNama, kNama, kSatuan ->
                                    items.add(ProduksiItem(itemDoc.id, prodId, varianId, bNama, kNama, kSatuan, qty, exp, shelfLife))
                                    processedItems++
                                    if (processedItems == totalItems) {
                                        listProduksi.add(Produksi(prodId, tglProduksi, mandorId, mandorName, items.toList()))
                                        processedDocs++
                                        if (processedDocs == totalDocs) updateUI(listProduksi, isExportPdf)
                                    }
                                }
                            }.addOnFailureListener {
                                processedItems++
                                if (processedItems == totalItems) {
                                    listProduksi.add(Produksi(prodId, tglProduksi, mandorId, mandorName, items.toList()))
                                    processedDocs++
                                    if (processedDocs == totalDocs) updateUI(listProduksi, isExportPdf)
                                }
                            }
                        }
                    }
                }
            }
        }.addOnFailureListener { e ->
            binding.progressBar.visibility = View.GONE
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadNames(bId: String, kId: String, callback: (String, String, String) -> Unit) {
        var bNama = ""
        var kNama = ""
        var kSatuan = ""
        var done = 0
        db.collection("master_barang").document(bId).get().addOnSuccessListener { 
            bNama = it.getString("nama_barang") ?: ""
            if (++done == 2) callback(bNama, kNama, kSatuan)
        }.addOnFailureListener { if (++done == 2) callback(bNama, kNama, kSatuan) }
        
        db.collection("master_kemasan").document(kId).get().addOnSuccessListener { 
            kNama = it.getString("nama_kemasan") ?: ""
            kSatuan = it.getString("satuan") ?: ""
            if (++done == 2) callback(bNama, kNama, kSatuan)
        }.addOnFailureListener { if (++done == 2) callback(bNama, kNama, kSatuan) }
    }

    private fun updateUI(list: List<Produksi>, isExportPdf: Boolean) {
        val sortedList = list.sortedByDescending { it.tanggalProduksi }
        listItems.clear()
        var totalQty = 0
        sortedList.forEach { prod ->
            var sum = ""
            prod.items.forEach { 
                totalQty += it.jumlahProduksi
                sum += "${it.barangNama} (${it.jumlahProduksi}), "
            }
            listItems.add(LaporanItem.ProduksiUI("Produksi: ${prod.mandorName}", prod.tanggalProduksi, "Items: ${sum.trimEnd(',', ' ')}", prod.id))
        }
        adapter.notifyDataSetChanged()
        binding.cardSummary.visibility = View.VISIBLE
        binding.tvSummaryProduksi.text = "Total Batch: ${sortedList.size} | Total Produk: $totalQty"
        binding.tvSummaryPenjualan.visibility = View.GONE
        binding.progressBar.visibility = View.GONE
        binding.tvStatus.text = "Data periode ${sdf.format(startDate!!.time)} - ${sdf.format(endDate!!.time)}"
        
        if (isExportPdf && sortedList.isNotEmpty()) {
            generatePdf(sortedList)
        } else if (isExportPdf) {
            Toast.makeText(context, "Tidak ada data untuk di-export", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generatePdf(list: List<Produksi>) {
        val pdf = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdf.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()
        var y = 40f

        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("LAPORAN PRODUKSI SIMPRO", 150f, y, paint)
        y += 30f
        paint.textSize = 12f
        paint.isFakeBoldText = false
        canvas.drawText("Periode: ${sdf.format(startDate!!.time)} - ${sdf.format(endDate!!.time)}", 40f, y, paint)
        y += 40f

        paint.isFakeBoldText = true
        canvas.drawText("RINCIAN PRODUKSI", 40f, y, paint)
        y += 20f
        paint.isFakeBoldText = false
        var total = 0
        list.forEach { prod ->
            var sub = 0
            prod.items.forEach { sub += it.jumlahProduksi }
            total += sub
            canvas.drawText("- ${sdf.format(Date(prod.tanggalProduksi))} | Mandor: ${prod.mandorName}", 40f, y, paint)
            y += 15f
            paint.textSize = 10f
            prod.items.forEach { item ->
                canvas.drawText("  * ${item.barangNama} - ${item.kemasanNama}: ${item.jumlahProduksi} ${item.kemasanSatuan}", 50f, y, paint)
                y += 14f
            }
            y += 10f
            paint.textSize = 12f
        }
        
        y += 10f
        paint.isFakeBoldText = true
        canvas.drawText("Total Seluruh Produk: $total", 40f, y, paint)
        pdf.finishPage(page)

        val fileName = "Laporan_Produksi_${System.currentTimeMillis()}.pdf"
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
