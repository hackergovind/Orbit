package com.antigravity.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.app.ui.components.GlassBox
import com.antigravity.app.ui.theme.DeepBlack
import com.antigravity.app.ui.theme.NeonCyan
import com.antigravity.app.ui.viewmodel.MeshViewModel

@Composable
fun DiscoveryScreen(
    viewModel: MeshViewModel,
    onNodeSelected: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Antigravity Mesh",
            color = NeonCyan,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp, bottom = 16.dp)
        )
        
        Text(
            text = "My Node ID: ${state.myNodeId}",
            color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        if (!state.isConnected) {
            Button(
                onClick = { viewModel.startMesh() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack)
            ) {
                Text("Start Mesh Discovery")
            }
        } else {
            Text(
                text = "Scanning for peers...",
                color = NeonCyan,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (state.activeNodes.isEmpty()) {
                CircularProgressIndicator(color = NeonCyan)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.activeNodes) { node ->
                        GlassBox(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNodeSelected(node) }
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = node,
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Connect",
                                    color = NeonCyan,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
