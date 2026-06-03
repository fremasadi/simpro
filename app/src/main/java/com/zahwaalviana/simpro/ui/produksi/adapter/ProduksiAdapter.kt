package com.zahwaalviana.simpro.ui.produksi.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.data.model.ProduksiWithItems
import com.zahwaalviana.simpro.databinding.ItemProduksiBinding
import java.text.SimpleDateFormat
import java.util.*

class ProduksiAdapter(
    private val produksiList: List<ProduksiWithItems>,
    private val userRole: String,
    private val onEditClick: (ProduksiWithItems) -> Unit,
    private val onDeleteClick: (ProduksiWithItems) -> Unit
) : RecyclerView.Adapter<ProduksiAdapter.ProduksiViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale("id", "ID"))

    inner class ProduksiViewHolder(private val binding: ItemProduksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(produksiWithItems: ProduksiWithItems) {
            binding.apply {
                val produksi = produksiWithItems.produksi
                val items = produksiWithItems.items

                tvTanggalProduksi.text = dateFormat.format(Date(produksi.tanggalProduksi))
                
                // Tampilkan mandor jika bukan login sebagai mandor
                if (userRole == "mandor") {
                    tvMandor.visibility = View.GONE
                } else {
                    tvMandor.visibility = View.VISIBLE
                    tvMandor.text = produksi.mandorName
                }

                // Ringkasan item barang
                val summary = items.joinToString(", ") { "${it.barangNama} x${it.jumlahProduksi}" }
                tvBarangSummary.visibility = View.VISIBLE
                tvBarangSummary.text = if (summary.isNotEmpty()) summary else "Tidak ada item"

                // Logika tombol Edit (Admin tidak bisa edit, Mandor bisa)
                ivEdit.visibility = if (userRole == "admin") View.GONE else View.VISIBLE
                
                // Tombol Delete selalu muncul (atau sesuaikan)
                ivDelete.visibility = View.VISIBLE

                ivEdit.setOnClickListener { onEditClick(produksiWithItems) }
                ivDelete.setOnClickListener { onDeleteClick(produksiWithItems) }
                
                // Jika ada tombol detail
                if (root.findViewById<View>(binding.btnDetail.id) != null) {
                    btnDetail.visibility = View.GONE // Kita pakai icon edit/delete saja agar rapi
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProduksiViewHolder {
        val binding = ItemProduksiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProduksiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProduksiViewHolder, position: Int) {
        holder.bind(produksiList[position])
    }

    override fun getItemCount(): Int = produksiList.size
}
