package com.antigravity.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ConnectWithoutContact
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.app.ui.viewmodel.MeshViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeshRadarScreen(
    viewModel: MeshViewModel,
    onNodeSelected: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var showNameDialog by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(state.myName) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Radar", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { 
                        tempName = state.myName
                        showNameDialog = true 
                    }) {
                        Icon(Icons.Filled.Settings, contentDescription = "Profile Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (!state.isConnected) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.startMesh() },
                    icon = { Icon(Icons.Filled.ConnectWithoutContact, contentDescription = null) },
                    text = { Text("Start Discovery") },
                    containerColor = MaterialTheme.colorScheme.primary, // Blurple fill
                    contentColor = MaterialTheme.colorScheme.onPrimary, // White label
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                )
            } else {
                FloatingActionButton(
                    onClick = { viewModel.stopMesh() },
                    containerColor = MaterialTheme.colorScheme.error,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.Close, contentDescription = "Stop Discovery", tint = MaterialTheme.colorScheme.onError)
                }
            }
        }
    ) { padding ->
        if (showNameDialog) {
            AlertDialog(
                onDismissRequest = { showNameDialog = false },
                title = { Text("Set Display Name") },
                text = {
                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text("Name") },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        viewModel.updateName(tempName)
                        showNameDialog = false
                    }, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showNameDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "My Profile",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Name: ${state.myName}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Node ID: ${state.myNodeId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Status: ${state.networkHealth}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.isConnected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error
                    )
                }
            }

            if (state.isConnected && state.activeNodes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.activeNodes) { node ->
                        ElevatedCard(
                            onClick = { onNodeSelected(node.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CellTower,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = node.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Signal: ${node.rssi} dBm  •  Hop: ${node.hopCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
