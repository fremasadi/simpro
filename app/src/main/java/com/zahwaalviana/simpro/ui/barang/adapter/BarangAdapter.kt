package com.zahwaalviana.simpro.ui.barang.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
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

                val jumlahVarian = barangWithVarian.varianList.size
                tvJumlahVarian.text = "$jumlahVarian Varian"

                // Clear previous chips
                chipGroupVarian.removeAllViews()

                // Add chips for each varian
                val barangNama = barangWithVarian.barang.namaBarang
                barangWithVarian.varianList.take(3).forEach { varian ->
                    val chip = Chip(binding.root.context).apply {
                        text = "${varian.kemasanNama} - ${currencyFormat.format(varian.hargaJual)} (Stok: ${varian.stok})"
                        chipBackgroundColor = ColorStateList.valueOf(
                            Color.parseColor("#FFF0F3")
                        )
                        setTextColor(Color.parseColor("#E59BA6"))
                        isClickable = true
                        isCheckable = false
                        setOnClickListener { onVarianClick(varian, barangNama) }
                    }
                    chipGroupVarian.addView(chip)
                }

                // Add "more" chip if there are more than 3 variants
                if (jumlahVarian > 3) {
                    val moreChip = Chip(binding.root.context).apply {
                        text = "+${jumlahVarian - 3} lainnya"
                        chipBackgroundColor = ColorStateList.valueOf(
                            Color.parseColor("#E59BA6")
                        )
                        setTextColor(Color.WHITE)
                        isClickable = false
                        isCheckable = false
                    }
                    chipGroupVarian.addView(moreChip)
                }

                ivEdit.setOnClickListener {
                    onEditClick(barangWithVarian)
                }

                ivDelete.setOnClickListener {
                    onDeleteClick(barangWithVarian)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BarangViewHolder {
        val binding = ItemBarangBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BarangViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BarangViewHolder, position: Int) {
        holder.bind(barangList[position])
    }

    override fun getItemCount(): Int = barangList.size
}