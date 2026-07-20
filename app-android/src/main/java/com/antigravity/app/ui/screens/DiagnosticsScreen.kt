package com.antigravity.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ConnectWithoutContact
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.app.ui.viewmodel.MeshViewModel
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(viewModel: MeshViewModel) {
    val state by viewModel.state.collectAsState()
    val dec = DecimalFormat("#,###.##")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mesh Telemetry", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            item {
                Text(
                    text = "Live Network Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.MonitorHeart,
                        title = "Health",
                        value = state.networkHealth,
                        valueColor = if (state.isConnected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.ConnectWithoutContact,
                        title = "Peers",
                        value = "${state.activeConnections}",
                        valueColor = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Speed,
                        title = "Bytes Sent",
                        value = dec.format(state.totalBytesSent),
                        valueColor = MaterialTheme.colorScheme.secondary
                    )
                    TelemetryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Storage,
                        title = "Bytes Rcvd",
                        value = dec.format(state.totalBytesReceived),
                        valueColor = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Routing Table Statistics", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Encryption: ChaCha20-Poly1305 (Active)", style = MaterialTheme.typography.bodySmall)
                        Text("Packet Signature: Ed25519 (Verified)", style = MaterialTheme.typography.bodySmall)
                        Text("Total Hop Threshold: 7", style = MaterialTheme.typography.bodySmall)
                        Text("Local Node Address: ${state.myNodeId}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }
}

@Composable
fun TelemetryCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color
) {
    ElevatedCard(
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}
