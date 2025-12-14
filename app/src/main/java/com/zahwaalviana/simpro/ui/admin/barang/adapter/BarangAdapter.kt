package com.zahwaalviana.simpro.ui.admin.barang.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.Barang

class BarangAdapter(
    private val onEditClick: (Barang) -> Unit,
    private val onDeleteClick: (Barang) -> Unit
) : RecyclerView.Adapter<BarangAdapter.BarangViewHolder>() {

    private var barangList = listOf<Barang>()

    fun submitList(newList: List<Barang>) {
        barangList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarangViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_barang, parent, false)
        return BarangViewHolder(view)
    }

    override fun onBindViewHolder(holder: BarangViewHolder, position: Int) {
        holder.bind(barangList[position], position + 1)
    }

    override fun getItemCount() = barangList.size

    inner class BarangViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberText = itemView.findViewById<TextView>(R.id.tvBarangNumber)
        private val bahanBakuText = itemView.findViewById<TextView>(R.id.tvNamaBahanBaku)
        private val barangJadiText = itemView.findViewById<TextView>(R.id.tvNamaBarangJadi)

        private val totalStokText = itemView.findViewById<TextView>(R.id.tvTotalStok)

        private val editButton = itemView.findViewById<ImageButton>(R.id.btnEdit)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.btnDelete)

        fun bind(barang: Barang, number: Int) {
            numberText.text = number.toString()
            bahanBakuText.text = barang.namaBahanBaku
            barangJadiText.text = barang.namaBarangJadi
            totalStokText.text = barang.total_stok.toString()


            editButton.setOnClickListener {
                onEditClick(barang)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(barang)
            }

            itemView.setOnClickListener {
                onEditClick(barang)
            }
        }
    }
}