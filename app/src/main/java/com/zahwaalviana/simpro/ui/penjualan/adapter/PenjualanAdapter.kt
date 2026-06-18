package com.zahwaalviana.simpro.ui.penjualan.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.data.model.PenjualanWithItems
import com.zahwaalviana.simpro.databinding.ItemPenjualanBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PenjualanAdapter(
    private val list: List<PenjualanWithItems>,
    private val onDetail: (PenjualanWithItems) -> Unit
) : RecyclerView.Adapter<PenjualanAdapter.ViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd/MM/yy HH:mm", Locale("id", "ID"))

    inner class ViewHolder(private val binding: ItemPenjualanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: PenjualanWithItems) = with(binding) {
            tvTanggal.text = dateFormat.format(Date(item.penjualan.tanggal))
            tvTotalHarga.text = currencyFormat.format(item.penjualan.totalHarga)
            
            btnDetail.setOnClickListener { onDetail(item) }
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
