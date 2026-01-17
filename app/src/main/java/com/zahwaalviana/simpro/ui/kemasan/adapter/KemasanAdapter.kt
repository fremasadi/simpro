package com.zahwaalviana.simpro.ui.kemasan.adapter

import Kemasan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.databinding.ItemKemasanBinding
import java.text.DecimalFormat

class KemasanAdapter(
    private val kemasanList: List<Kemasan>,
    private val onEditClick: (Kemasan) -> Unit,
    private val onDeleteClick: (Kemasan) -> Unit
) : RecyclerView.Adapter<KemasanAdapter.KemasanViewHolder>() {

    private val decimalFormat = DecimalFormat("#,##0.##")

    inner class KemasanViewHolder(private val binding: ItemKemasanBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(kemasan: Kemasan) {
            binding.apply {
                tvNamaKemasan.text = kemasan.namaKemasan
                tvSatuan.text = "Satuan: ${kemasan.satuan}"
                tvStok.text = "Stok: ${decimalFormat.format(kemasan.stok)} ${kemasan.satuan}"

                ivEdit.setOnClickListener {
                    onEditClick(kemasan)
                }

                ivDelete.setOnClickListener {
                    onDeleteClick(kemasan)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KemasanViewHolder {
        val binding = ItemKemasanBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return KemasanViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KemasanViewHolder, position: Int) {
        holder.bind(kemasanList[position])
    }

    override fun getItemCount(): Int = kemasanList.size
}
