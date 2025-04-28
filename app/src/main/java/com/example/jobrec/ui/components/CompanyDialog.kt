package com.example.jobrec.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.jobrec.Company

@Composable
fun CompanyDialog(
    company: Company? = null,
    onDismiss: () -> Unit,
    onSave: (Company) -> Unit
) {
    var companyName by remember { mutableStateOf(company?.companyName ?: "") }
    var registrationNumber by remember { mutableStateOf(company?.registrationNumber ?: "") }
    var industry by remember { mutableStateOf(company?.industry ?: "") }
    var companySize by remember { mutableStateOf(company?.companySize ?: "") }
    var location by remember { mutableStateOf(company?.location ?: "") }
    var website by remember { mutableStateOf(company?.website ?: "") }
    var description by remember { mutableStateOf(company?.description ?: "") }
    var contactPersonName by remember { mutableStateOf(company?.contactPersonName ?: "") }
    var contactPersonEmail by remember { mutableStateOf(company?.contactPersonEmail ?: "") }
    var contactPersonPhone by remember { mutableStateOf(company?.contactPersonPhone ?: "") }
    var email by remember { mutableStateOf(company?.email ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (company == null) "Add Company" else "Edit Company",
                    style = MaterialTheme.typography.titleLarge
                )

                OutlinedTextField(
                    value = companyName,
                    onValueChange = { companyName = it },
                    label = { Text("Company Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = registrationNumber,
                    onValueChange = { registrationNumber = it },
                    label = { Text("Registration Number") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = industry,
                    onValueChange = { industry = it },
                    label = { Text("Industry") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = companySize,
                    onValueChange = { companySize = it },
                    label = { Text("Company Size") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = website,
                    onValueChange = { website = it },
                    label = { Text("Website") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPersonName,
                    onValueChange = { contactPersonName = it },
                    label = { Text("Contact Person Name") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPersonEmail,
                    onValueChange = { contactPersonEmail = it },
                    label = { Text("Contact Person Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = contactPersonPhone,
                    onValueChange = { contactPersonPhone = it },
                    label = { Text("Contact Person Phone") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                Company(
                                    id = company?.id ?: "",
                                    companyName = companyName,
                                    registrationNumber = registrationNumber,
                                    industry = industry,
                                    companySize = companySize,
                                    location = location,
                                    website = website,
                                    description = description,
                                    contactPersonName = contactPersonName,
                                    contactPersonEmail = contactPersonEmail,
                                    contactPersonPhone = contactPersonPhone,
                                    email = email,
                                    profileImageUrl = company?.profileImageUrl ?: ""
                                )
                            )
                        }
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
} 