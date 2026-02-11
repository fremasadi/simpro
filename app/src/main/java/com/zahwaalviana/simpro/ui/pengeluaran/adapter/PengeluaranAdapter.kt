package com.zahwaalviana.simpro.ui.pengeluaran.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.data.model.Pengeluaran
import com.zahwaalviana.simpro.databinding.ItemPengeluaranBinding
import java.text.NumberFormat
import java.util.Locale

class PengeluaranAdapter(
    private val list: List<Pengeluaran>,
    private val masterMap: Map<String, Pair<String, String>>,
    // masterPengeluaranId -> (nama, kategori)
    private val onEdit: (Pengeluaran) -> Unit,
    private val onDelete: (Pengeluaran) -> Unit
) : RecyclerView.Adapter<PengeluaranAdapter.ViewHolder>() {

    inner class ViewHolder(
        private val binding: ItemPengeluaranBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Pengeluaran) = with(binding) {

            val master = masterMap[item.masterPengeluaranId]
            tvNamaPengeluaran.text = master?.first ?: "-"
            tvKategori.text = master?.second ?: "-"

            tvTanggal.text = item.tanggal
            tvKeterangan.text = item.keterangan

            val rupiah = NumberFormat
                .getCurrencyInstance(Locale("in", "ID"))
                .format(item.biaya)
            tvBiaya.text = rupiah

            ivEdit.setOnClickListener { onEdit(item) }
            ivDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPengeluaranBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int = list.size
}