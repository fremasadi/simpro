package com.zahwaalviana.simpro.ui.laporan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.databinding.ItemLaporanBinding
import java.text.SimpleDateFormat
import java.util.*

sealed class LaporanItem {
    data class ProduksiUI(
        val title: String,
        val date: Long,
        val info: String
    ) : LaporanItem()

    data class PenjualanUI(
        val title: String,
        val date: Long,
        val info: String
    ) : LaporanItem()
}

class LaporanAdapter(private val items: List<LaporanItem>) :
    RecyclerView.Adapter<LaporanAdapter.LaporanViewHolder>() {

    private val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    class LaporanViewHolder(val binding: ItemLaporanBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LaporanViewHolder {
        val binding = ItemLaporanBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return LaporanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LaporanViewHolder, position: Int) {
        val item = items[position]
        holder.binding.apply {
            when (item) {
                is LaporanItem.ProduksiUI -> {
                    tvItemType.text = "PRODUKSI"
                    tvItemTitle.text = item.title
                    tvItemDate.text = "Tanggal: ${sdf.format(Date(item.date))}"
                    tvItemInfo.text = item.info
                }
                is LaporanItem.PenjualanUI -> {
                    tvItemType.text = "PENJUALAN"
                    tvItemTitle.text = item.title
                    tvItemDate.text = "Tanggal: ${sdf.format(Date(item.date))}"
                    tvItemInfo.text = item.info
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size
}