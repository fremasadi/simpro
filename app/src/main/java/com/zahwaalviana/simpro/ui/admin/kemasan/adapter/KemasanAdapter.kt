package com.zahwaalviana.simpro.ui.admin.kemasan.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.Kemasan

class KemasanAdapter(
    private val onEditClick: (Kemasan) -> Unit,
    private val onDeleteClick: (Kemasan) -> Unit
) : RecyclerView.Adapter<KemasanAdapter.KemasanViewHolder>() {

    private var kemasanList = listOf<Kemasan>()

    fun submitList(newList: List<Kemasan>) {
        kemasanList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KemasanViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_kemasan, parent, false)
        return KemasanViewHolder(view)
    }

    override fun onBindViewHolder(holder: KemasanViewHolder, position: Int) {
        holder.bind(kemasanList[position], position + 1)
    }

    override fun getItemCount() = kemasanList.size

    inner class KemasanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberText = itemView.findViewById<TextView>(R.id.tvKemasanNumber)
        private val namaKemasanText = itemView.findViewById<TextView>(R.id.tvNamaKemasan)
        private val namaBarangText = itemView.findViewById<TextView>(R.id.tvNamaBarangJadi)
        private val editButton = itemView.findViewById<ImageButton>(R.id.btnEdit)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.btnDelete)

        fun bind(kemasan: Kemasan, number: Int) {
            numberText.text = number.toString()
            namaKemasanText.text = kemasan.namaKemasan
            namaBarangText.text = kemasan.namaBarangJadi

            editButton.setOnClickListener {
                onEditClick(kemasan)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(kemasan)
            }

            itemView.setOnClickListener {
                onEditClick(kemasan)
            }
        }
    }
}