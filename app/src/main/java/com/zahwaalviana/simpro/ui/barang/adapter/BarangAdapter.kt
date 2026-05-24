package com.zahwaalviana.simpro.ui.barang.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.data.model.BarangWithVarian
import com.zahwaalviana.simpro.databinding.ItemBarangBinding

class BarangAdapter(
    private val barangList: List<BarangWithVarian>,
    private val onEditClick: (BarangWithVarian) -> Unit,
    private val onDeleteClick: (BarangWithVarian) -> Unit
) : RecyclerView.Adapter<BarangAdapter.BarangViewHolder>() {

    inner class BarangViewHolder(private val binding: ItemBarangBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barangWithVarian: BarangWithVarian) {
            binding.apply {
                tvNamaBarang.text = barangWithVarian.barang.namaBarang
                
                ivEdit.setOnClickListener { onEditClick(barangWithVarian) }
                ivDelete.setOnClickListener { onDeleteClick(barangWithVarian) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarangViewHolder {
        val binding = ItemBarangBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BarangViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarangViewHolder, position: Int) {
        holder.bind(barangList[position])
    }

    override fun getItemCount(): Int = barangList.size
}
