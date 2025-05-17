package com.example.jobrec

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class AdminUsersAdapter(
    private var users: List<User>,
    private val onViewClick: (User) -> Unit,
    private val onEditClick: (User) -> Unit,
    private val onDeleteClick: (User) -> Unit
) : RecyclerView.Adapter<AdminUsersAdapter.UserViewHolder>() {

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.userNameText)
        val emailText: TextView = itemView.findViewById(R.id.userEmailText)
        val viewButton: MaterialButton = itemView.findViewById(R.id.viewUserButton)
        val editButton: MaterialButton = itemView.findViewById(R.id.editUserButton)
        val deleteButton: MaterialButton = itemView.findViewById(R.id.deleteUserButton)
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

        holder.viewButton.setOnClickListener { onViewClick(user) }
        holder.editButton.setOnClickListener { onEditClick(user) }
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