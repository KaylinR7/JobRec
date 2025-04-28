package com.example.jobrec.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.jobrec.Company
import com.example.jobrec.ui.components.CompanyDialog
import com.google.firebase.firestore.FirebaseFirestore
import androidx.navigation.NavHostController

@Composable
fun AdminCompaniesScreen(navController: NavHostController) {
    val db = FirebaseFirestore.getInstance()
    var companies by remember { mutableStateOf<List<Company>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedCompany by remember { mutableStateOf<Company?>(null) }
    
    LaunchedEffect(Unit) {
        db.collection("companies")
            .get()
            .addOnSuccessListener { documents ->
                companies = documents.map { doc ->
                    doc.toObject(Company::class.java).copy(id = doc.id)
                }
            }
    }
    
    if (showDialog) {
        CompanyDialog(
            company = selectedCompany,
            onDismiss = {
                showDialog = false
                selectedCompany = null
            },
            onSave = { company ->
                if (company.id.isEmpty()) {
                    // Add new company
                    db.collection("companies")
                        .add(company)
                        .addOnSuccessListener {
                            companies = companies + company.copy(id = it.id)
                        }
                } else {
                    // Update existing company
                    db.collection("companies")
                        .document(company.id)
                        .set(company)
                        .addOnSuccessListener {
                            companies = companies.map { c ->
                                if (c.id == company.id) company else c
                            }
                        }
                }
                showDialog = false
                selectedCompany = null
            }
        )
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
            items(companies) { company ->
                CompanyCard(
                    company = company,
                    onEdit = {
                        selectedCompany = company
                        showDialog = true
                    },
                    onDelete = {
                        db.collection("companies")
                            .document(company.id)
                            .delete()
                            .addOnSuccessListener {
                                companies = companies.filter { it.id != company.id }
                            }
                    }
                )
            }
        }
        
        Button(
            onClick = {
                selectedCompany = null
                showDialog = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add New Company")
        }
    }
}

@Composable
fun CompanyCard(
    company: Company,
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
                text = company.companyName,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = company.industry,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = company.location,
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