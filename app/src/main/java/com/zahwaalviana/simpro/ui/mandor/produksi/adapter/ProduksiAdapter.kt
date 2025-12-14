package com.zahwaalviana.simpro.ui.mandor.produksi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.data.model.Produksi
import com.zahwaalviana.simpro.databinding.ItemProduksiBinding
import java.text.SimpleDateFormat
import java.util.*

class ProduksiAdapter(
    private val onEdit: (Produksi) -> Unit,
    private val onDelete: (Produksi) -> Unit
) : RecyclerView.Adapter<ProduksiAdapter.ViewHolder>() {

    private var list = listOf<Produksi>()

    fun submitData(data: List<Produksi>) {
        list = data
        notifyDataSetChanged()
    }

    inner class ViewHolder(val b: ItemProduksiBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(item: Produksi, position: Int) {

            val df = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
            val tanggal = item.tanggal?.toDate()?.let { df.format(it) } ?: "-"

            b.tvNo.text = "${position + 1}"
            b.tvTanggal.text = tanggal
            b.tvBarang.text = item.namaBarangJadi
            b.tvProduksi.text = item.jumlah_produksi.toString()
            b.tvKeluar.text = item.jumlah_keluar.toString()

            b.btnEdit.setOnClickListener { onEdit(item) }
//            b.btnDelete.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProduksiBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int = list.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(list[position], position)
    }
}
