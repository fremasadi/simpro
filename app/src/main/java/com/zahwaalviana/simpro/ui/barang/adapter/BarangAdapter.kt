package com.zahwaalviana.simpro.ui.barang.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.data.model.BarangWithVarian
import com.zahwaalviana.simpro.databinding.ItemBarangBinding
import java.text.NumberFormat
import java.util.Locale

class BarangAdapter(
    private val barangList: List<BarangWithVarian>,
    private val onEditClick: (BarangWithVarian) -> Unit,
    private val onDeleteClick: (BarangWithVarian) -> Unit,
    private val onVarianClick: (BarangVarian, String) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<BarangAdapter.BarangViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    inner class BarangViewHolder(private val binding: ItemBarangBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barangWithVarian: BarangWithVarian) {
            binding.apply {
                tvNamaBarang.text = barangWithVarian.barang.namaBarang
                
                if (barangWithVarian.varianList.isEmpty()) {
                    tvVarian.text = "Belum ada varian"
                    tvStok.text = "-"
                    tvHarga.text = "-"
                } else {
                    tvVarian.text = barangWithVarian.varianList.joinToString("\n") { it.kemasanNama }
                    tvStok.text = barangWithVarian.varianList.joinToString("\n") { "${it.stok} ${it.kemasanSatuan}" }
                    tvHarga.text = barangWithVarian.varianList.joinToString("\n") { currencyFormat.format(it.hargaJual) }
                }

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
