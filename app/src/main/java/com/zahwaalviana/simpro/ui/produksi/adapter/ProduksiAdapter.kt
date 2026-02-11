package com.zahwaalviana.simpro.ui.produksi.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.zahwaalviana.simpro.data.model.ProduksiWithItems
import com.zahwaalviana.simpro.databinding.ItemProduksiBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class ProduksiAdapter(
    private val produksiList: List<ProduksiWithItems>,
    private val onEditClick: (ProduksiWithItems) -> Unit,
    private val onDeleteClick: (ProduksiWithItems) -> Unit
) : RecyclerView.Adapter<ProduksiAdapter.ProduksiViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

    inner class ProduksiViewHolder(private val binding: ItemProduksiBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(produksiWithItems: ProduksiWithItems) {
            binding.apply {
                val produksi = produksiWithItems.produksi
                val items = produksiWithItems.items

                tvTanggalProduksi.text = dateFormat.format(Date(produksi.tanggalProduksi))
                tvMandor.text = "Mandor: ${produksi.mandorName}"
                tvJumlahItem.text = "${items.size} Item Produksi"

                // Clear previous chips
                chipGroupItems.removeAllViews()

                // Add chips for each item
                items.take(3).forEach { item ->
                    val chip = Chip(binding.root.context).apply {
                        text = "${item.barangNama} - ${item.kemasanNama}: ${item.jumlahProduksi}"
                        chipBackgroundColor = ColorStateList.valueOf(
                            Color.parseColor("#FFF0F3")
                        )
                        setTextColor(Color.parseColor("#E59BA6"))
                        isClickable = false
                        isCheckable = false
                    }
                    chipGroupItems.addView(chip)
                }

                // Add "more" chip if there are more than 3 items
                if (items.size > 3) {
                    val moreChip = Chip(binding.root.context).apply {
                        text = "+${items.size - 3} lainnya"
                        chipBackgroundColor = ColorStateList.valueOf(
                            Color.parseColor("#E59BA6")
                        )
                        setTextColor(Color.WHITE)
                        isClickable = false
                        isCheckable = false
                    }
                    chipGroupItems.addView(moreChip)
                }

                ivEdit.setOnClickListener {
                    onEditClick(produksiWithItems)
                }

                ivDelete.setOnClickListener {
                    onDeleteClick(produksiWithItems)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProduksiViewHolder {
        val binding = ItemProduksiBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProduksiViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProduksiViewHolder, position: Int) {
        holder.bind(produksiList[position])
    }

    override fun getItemCount(): Int = produksiList.size
}