package com.zahwaalviana.simpro.ui.pengeluaran.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.RelatedItem

class RelatedItemSelectAdapter(
    private val allItems: List<RelatedItem>,
    private val selectedItems: MutableList<RelatedItem>
) : RecyclerView.Adapter<RelatedItemSelectAdapter.ViewHolder>() {

    private var displayList = allItems.toList()

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val checkBox: CheckBox = view.findViewById(R.id.checkBox)
        val tvName: TextView = view.findViewById(R.id.tvItemName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_related, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = displayList[position]
        holder.tvName.text = item.name
        
        // Check if item is selected
        holder.checkBox.isChecked = selectedItems.any { it.id == item.id }

        holder.itemView.setOnClickListener {
            if (holder.checkBox.isChecked) {
                selectedItems.removeAll { it.id == item.id }
            } else {
                if (!selectedItems.any { it.id == item.id }) {
                    selectedItems.add(item)
                }
            }
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = displayList.size

    fun filter(query: String) {
        displayList = if (query.isEmpty()) {
            allItems
        } else {
            allItems.filter { it.name.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }
}
