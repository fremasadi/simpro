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
    private val onDetailClick: (ProduksiWithItems) -> Unit
) : RecyclerView.Adapter<ProduksiAdapter.ProduksiViewHolder>() {

    private val dateFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("id", "ID"))

    inner class ProduksiViewHolder(private val binding: ItemProduksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(produksiWithItems: ProduksiWithItems) {
            binding.apply {
                val produksi = produksiWithItems.produksi
                tvTanggalProduksi.text = dateFormat.format(Date(produksi.tanggalProduksi))
                
                // Fitur Edit & Delete dihapus sesuai permintaan
                ivEdit.visibility = View.GONE
                ivDelete.visibility = View.GONE
                
                // Menampilkan tombol Detail
                btnDetail.setOnClickListener {
                    onDetailClick(produksiWithItems)
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
