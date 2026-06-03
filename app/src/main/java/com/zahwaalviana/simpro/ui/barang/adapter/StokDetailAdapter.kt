package com.zahwaalviana.simpro.ui.barang.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.StokDetailItem

class StokDetailAdapter(
    private val list: List<StokDetailItem>
) : RecyclerView.Adapter<StokDetailAdapter.ViewHolder>() {

    inner class ViewHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_stok_detail, parent, false)
    ) {
        private val tvMandorName: TextView = itemView.findViewById(R.id.tvMandorName)
        private val tvTanggalProduksi: TextView = itemView.findViewById(R.id.tvTanggalProduksi)
        private val tvJumlah: TextView = itemView.findViewById(R.id.tvJumlah)

        fun bind(item: StokDetailItem) {
            tvMandorName.text = "Mandor: ${item.mandorName}"
            tvTanggalProduksi.text = item.tanggal
            tvJumlah.text = "+${item.jumlah}"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}
