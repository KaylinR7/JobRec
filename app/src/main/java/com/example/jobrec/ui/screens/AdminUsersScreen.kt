package com.example.jobrec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jobrec.User
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavHostController

@Composable
fun AdminUsersScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<User?>(null) }
    
    LaunchedEffect(Unit) {
        db.collection("users")
            .get()
            .addOnSuccessListener { documents ->
                users = documents.map { doc ->
                    doc.toObject(User::class.java).copy(id = doc.id)
                }
            }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(users) { user ->
                UserCard(
                    user = user,
                    onEdit = {
                        selectedUser = user
                        showDialog = true
                    },
                    onDelete = {
                        db.collection("users")
                            .document(user.id)
                            .delete()
                            .addOnSuccessListener {
                                users = users.filter { it.id != user.id }
                            }
                    }
                )
            }
        }
        
        Button(
            onClick = {
                selectedUser = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New User")
        }
    }
}

@Composable
fun UserCard(
    user: User,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = user.name,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = user.role,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }
        }
    }
} 