package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AdminUsersAdapter(
    private var users: List<User>,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.userNameText)
        val emailText: TextView = itemView.findViewById(R.id.userEmailText)
        val deleteButton: Button = itemView.findViewById(R.id.deleteUserButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.nameText.text = "${user.name} ${user.surname}"
        holder.emailText.text = user.email
        holder.deleteButton.setOnClickListener { onDeleteClick(user) }
    }

    override fun getItemCount(): Int = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers
        notifyDataSetChanged()
    }

    val usersList: List<User>
        get() = users
} 