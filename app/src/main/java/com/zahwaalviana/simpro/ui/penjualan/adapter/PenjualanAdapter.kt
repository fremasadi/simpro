package com.zahwaalviana.simpro.ui.penjualan.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.zahwaalviana.simpro.data.model.PenjualanWithItems
import com.zahwaalviana.simpro.databinding.ItemPenjualanBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PenjualanAdapter(
    private val list: List<PenjualanWithItems>,
    private val onDelete: (PenjualanWithItems) -> Unit
) : RecyclerView.Adapter<PenjualanAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID"))

    inner class ViewHolder(private val binding: ItemPenjualanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PenjualanWithItems) = with(binding) {
            tvTanggal.text = dateFormat.format(Date(item.penjualan.tanggal))
            tvTotalHarga.text = currencyFormat.format(item.penjualan.totalHarga)
            tvJumlahItem.text = "${item.items.size} item"
            tvBayarKembalian.text = "Bayar: ${currencyFormat.format(item.penjualan.totalBayar)} | Kembali: ${currencyFormat.format(item.penjualan.kembalian)}"

            chipGroupItems.removeAllViews()
            item.items.take(3).forEach { penjualanItem ->
                val chip = Chip(binding.root.context).apply {
                    text = "${penjualanItem.barangNama} x${penjualanItem.jumlah}"
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#FFF0F3"))
                    setTextColor(Color.parseColor("#E59BA6"))
                    isClickable = false
                    isCheckable = false
                }
                chipGroupItems.addView(chip)
            }

            if (item.items.size > 3) {
                val moreChip = Chip(binding.root.context).apply {
                    text = "+${item.items.size - 3} lainnya"
                    chipBackgroundColor = ColorStateList.valueOf(Color.parseColor("#E59BA6"))
                    setTextColor(Color.WHITE)
                    isClickable = false
                    isCheckable = false
                }
                chipGroupItems.addView(moreChip)
            }

            ivDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPenjualanBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}
