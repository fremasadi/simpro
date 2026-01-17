package com.zahwaalviana.simpro.ui.user.adapter

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.zahwaalviana.simpro.R
import com.zahwaalviana.simpro.data.model.User
import com.zahwaalviana.simpro.databinding.ItemUserBinding

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
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(users[position], position + 1)
    }

    override fun getItemCount() = users.size

    inner class UserViewHolder(private val binding: ItemUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User, number: Int) {
            binding.apply {
                // Set user data
                tvUserName.text = user.name
                tvUserEmail.text = user.email
                tvUserRole.text = user.role.uppercase()

                // Set background color based on role
                val backgroundColor: Int
                val textColor: Int

                when (user.role.lowercase()) {
                    "admin" -> {
                        backgroundColor = ContextCompat.getColor(root.context, android.R.color.holo_red_light)
                        textColor = ContextCompat.getColor(root.context, android.R.color.white)
                    }
                    "mandor" -> {
                        backgroundColor = ContextCompat.getColor(root.context, android.R.color.holo_green_light)
                        textColor = ContextCompat.getColor(root.context, android.R.color.white)
                    }
                    else -> {
                        backgroundColor = ContextCompat.getColor(root.context, android.R.color.darker_gray)
                        textColor = ContextCompat.getColor(root.context, android.R.color.white)
                    }
                }

                // Create rounded background programmatically
                val drawable = GradientDrawable()
                drawable.shape = GradientDrawable.RECTANGLE
                drawable.setColor(backgroundColor)
                drawable.cornerRadius = 12f * root.resources.displayMetrics.density

                tvUserRole.background = drawable
                tvUserRole.setTextColor(textColor)

                // Set click listeners
                ivEdit.setOnClickListener {
                    onEditClick(user)
                }

                root.setOnClickListener {
                    onEditClick(user)
                }
            }
        }
    }
}