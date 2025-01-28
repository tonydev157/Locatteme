package com.tonymen.locatteme.view.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.*
import com.tonymen.locatteme.viewmodel.ChatViewModel
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tonymen.locatteme.model.chatmodels.Message
import com.tonymen.locatteme.view.ui.ui.theme.LocattemeTheme
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val chatId = intent.getStringExtra("chatId") ?: return
        val currentUserId = intent.getStringExtra("currentUserId") ?: return

        setContent {
            LocattemeTheme {
                val chatViewModel: ChatViewModel = viewModel()
                chatViewModel.listenToMessages(chatId) // Escuchar mensajes en tiempo real

                ChatScreen(
                    chatViewModel = chatViewModel,
                    chatId = chatId,
                    currentUserId = currentUserId
                )
            }
        }
    }
}

@Composable
fun ChatScreen(
    chatViewModel: ChatViewModel,
    chatId: String,
    currentUserId: String
) {
    val messages by chatViewModel.messages.collectAsState()
    val inputMessage by chatViewModel.inputMessage.collectAsState()
    val listState = rememberLazyListState() // Define el LazyListState

    Scaffold(
        topBar = { ChatTopBar() },
        bottomBar = {
            ChatInputField(
                messageText = inputMessage,
                onMessageTextChanged = { chatViewModel.onInputMessageChanged(it) },
                onSendClicked = { chatViewModel.sendMessage(chatId, currentUserId) },
                onAttachClicked = {}
            )
        }
    ) { paddingValues ->
        ChatMessageList(
            messages = messages,
            currentUserId = currentUserId,
            onLoadOlderMessages = { chatViewModel.loadOlderMessages(chatId) },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            listState = listState // Pasa el listState aquí
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatTopBar() {
    TopAppBar(title = { Text("Chat") })
}

@Composable
fun ChatInputField(
    messageText: String,
    onMessageTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onAttachClicked: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAttachClicked) {
            Icon(Icons.Default.AttachFile, contentDescription = "Adjuntar")
        }

        TextField(
            value = messageText,
            onValueChange = onMessageTextChanged,
            placeholder = { Text("Escribe un mensaje...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(50.dp),
            colors = TextFieldDefaults.colors(
                unfocusedContainerColor = Color(0xFFECECEC),
                focusedContainerColor = Color.White,
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent
            )
        )

        IconButton(onClick = onSendClicked) {
            Icon(Icons.Default.Send, contentDescription = "Enviar", tint = Color(0xFF128C7E))
        }
    }
}

@Composable
fun ChatMessageList(
    messages: List<Message>,
    currentUserId: String,
    onLoadOlderMessages: () -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState // Acepta el listState como parámetro
) {
    var isFirstLoad by remember { mutableStateOf(true) } // Controla si es la primera vez que carga

    // Desplazar automáticamente al último mensaje solo la primera vez
    LaunchedEffect(messages) {
        if (isFirstLoad && messages.isNotEmpty()) {
            listState.scrollToItem(messages.size - 1) // Ir al último mensaje
            isFirstLoad = false // Evitar que se ejecute nuevamente
        }
    }

    LazyColumn(
        state = listState, // Usa el listState pasado como parámetro
        reverseLayout = false, // Mensajes en orden cronológico ascendente
        modifier = modifier
    ) {
        // Cargar mensajes más antiguos al llegar al inicio
        if (messages.isNotEmpty()) {
            item {
                LaunchedEffect(listState.firstVisibleItemIndex) {
                    if (listState.firstVisibleItemIndex == 0) {
                        onLoadOlderMessages()
                    }
                }
            }
        }

        // Mostrar mensajes
        items(messages) { message ->
            ChatBubble(
                message = message,
                isSentByCurrentUser = message.senderId == currentUserId
            )
        }
    }
}


@Composable
fun ChatBubble(message: Message, isSentByCurrentUser: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isSentByCurrentUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isSentByCurrentUser) AvatarIcon()

        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isSentByCurrentUser) Color(0xFFD9FDD3) else Color(0xFFECECEC)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = message.messageText, color = Color.Black)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(message.timestamp.toDate()),
                    color = Color.Gray,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
fun AvatarIcon() {
    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = "Avatar",
        modifier = Modifier
            .size(36.dp)
            .padding(end = 4.dp),
        tint = Color.LightGray
    )
}
