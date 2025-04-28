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
import com.example.jobrec.Job
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavHostController

@Composable
fun AdminJobsScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    var jobs by remember { mutableStateOf<List<Job>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedJob by remember { mutableStateOf<Job?>(null) }
    
    LaunchedEffect(Unit) {
        db.collection("jobs")
            .get()
            .addOnSuccessListener { documents ->
                jobs = documents.map { doc ->
                    doc.toObject(Job::class.java).copy(id = doc.id)
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
            items(jobs) { job ->
                JobCard(
                    job = job,
                    onEdit = {
                        selectedJob = job
                        showDialog = true
                    },
                    onDelete = {
                        db.collection("jobs")
                            .document(job.id)
                            .delete()
                            .addOnSuccessListener {
                                jobs = jobs.filter { it.id != job.id }
                            }
                    }
                )
            }
        }
        
        Button(
            onClick = {
                selectedJob = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Job")
        }
    }
}

@Composable
fun JobCard(
    job: Job,
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
                text = job.title,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.companyName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = job.location,
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