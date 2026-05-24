package com.zahwaalviana.simpro.ui.varian.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.data.model.BarangVarian
import com.zahwaalviana.simpro.databinding.ItemVarianBinding
import java.text.NumberFormat
import java.util.Locale

class VarianAdapter(
    private var varianList: List<Pair<String, BarangVarian>>,
    private val onItemClick: (String, BarangVarian) -> Unit
) : RecyclerView.Adapter<VarianAdapter.VarianViewHolder>() {

    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

    inner class VarianViewHolder(private val binding: ItemVarianBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(barangNama: String, varian: BarangVarian) {
            binding.apply {
                tvNamaBarang.text = barangNama
                tvNamaVarian.text = "${varian.kemasanNama} (${varian.kemasanSatuan})"
                tvStok.text = varian.stok.toString()
                tvHarga.text = currencyFormat.format(varian.hargaJual)
                
                root.setOnClickListener { onItemClick(barangNama, varian) }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VarianViewHolder {
        val binding = ItemVarianBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VarianViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VarianViewHolder, position: Int) {
        val (barangNama, varian) = varianList[position]
        holder.bind(barangNama, varian)
    }

    override fun getItemCount(): Int = varianList.size
}
