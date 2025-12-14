package com.zahwaalviana.simpro.ui.admin.user.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.User

// UserAdapter.kt
class UserAdapter(
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private var users = listOf<User>()

    fun submitList(newList: List<User>) {
        users = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], position + 1)
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val numberText = itemView.findViewById<TextView>(R.id.tvUserNumber)
        private val nameText = itemView.findViewById<TextView>(R.id.tvUserName)
        private val emailText = itemView.findViewById<TextView>(R.id.tvUserEmail)
        private val roleText = itemView.findViewById<TextView>(R.id.tvUserRole)
        private val editButton = itemView.findViewById<ImageButton>(R.id.btnEdit)
        private val deleteButton = itemView.findViewById<ImageButton>(R.id.btnDelete)

        fun bind(user: User, number: Int) {
            numberText.text = number.toString()
            nameText.text = user.name
            emailText.text = user.email
            roleText.text = user.role.uppercase()

            // Set background color based on role (like HTML/CSS)
            val backgroundColor: Int
            val textColor: Int

            when (user.role.lowercase()) {
                "admin" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, android.R.color.holo_blue_light)
                    textColor = ContextCompat.getColor(itemView.context, android.R.color.white)
                }
                "mandor" -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, android.R.color.holo_green_light)
                    textColor = ContextCompat.getColor(itemView.context, android.R.color.white)
                }
                else -> {
                    backgroundColor = ContextCompat.getColor(itemView.context, android.R.color.darker_gray)
                    textColor = ContextCompat.getColor(itemView.context, android.R.color.white)
                }
            }

            // Create rounded background programmatically
            val drawable = GradientDrawable()
            drawable.shape = GradientDrawable.RECTANGLE
            drawable.setColor(backgroundColor)
            drawable.cornerRadius = 12f * itemView.resources.displayMetrics.density

            roleText.background = drawable
            roleText.setTextColor(textColor)

            // Set click listeners
            editButton.setOnClickListener {
                onEditClick(user)
            }

//            deleteButton.setOnClickListener {
//                onDeleteClick(user)
//            }

            itemView.setOnClickListener {
                onEditClick(user)
            }
        }
    }
}