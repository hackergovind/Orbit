package com.antigravity.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.app.ui.components.GlassBox
import com.antigravity.app.ui.theme.DeepBlack
import com.antigravity.app.ui.theme.NeonCyan
import com.antigravity.app.ui.theme.NeonPurple
import com.antigravity.app.ui.theme.SurfaceDark
import com.antigravity.app.ui.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    targetNodeId: String,
    viewModel: ChatViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = targetNodeId,
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "E2EE Secured Tunnel",
                        color = NeonCyan,
                        fontSize = 12.sp
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = SurfaceDark
            ),
            navigationIcon = {
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = NeonCyan)
                ) {
                    Text("< Back")
                }
            }
        )

        // Chat History
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            reverseLayout = true
        ) {
            items(state.messages.reversed()) { msg ->
                val alignment = if (msg.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
                val bgColor = if (msg.isFromMe) NeonPurple.copy(alpha = 0.2f) else GlassBoxColor

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    contentAlignment = alignment
                ) {
                    GlassBox(
                        modifier = Modifier
                            .widthIn(max = 280.dp)
                            .background(bgColor)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = Color.White,
                            fontSize = 16.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }
        }

        // Input Area
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = state.currentInput,
                onValueChange = { viewModel.updateInput(it) },
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                placeholder = { Text("Encrypted message...", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = Color.DarkGray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = NeonCyan
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { viewModel.sendMessage() })
            )
            
            Button(
                onClick = { viewModel.sendMessage() },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan, contentColor = DeepBlack),
                shape = RoundedCornerShape(50)
            ) {
                Text("Send", fontWeight = FontWeight.Bold)
            }
        }
    }
}

private val GlassBoxColor = Color(0x1AFFFFFF)
