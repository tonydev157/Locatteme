package com.tonymen.locatteme.view.HomeFragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.FragmentChatBinding
import com.tonymen.locatteme.model.Chat
import com.tonymen.locatteme.model.Message
import com.tonymen.locatteme.model.MessageType
import com.tonymen.locatteme.view.MediaPreviewActivity
import com.tonymen.locatteme.view.adapters.MessageClickHandler
import com.tonymen.locatteme.view.adapters.MessagesAdapter
import com.tonymen.locatteme.viewmodel.ChatViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.IOException

class ChatFragment : Fragment(), MessageClickHandler {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: ChatViewModel
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var adapter: MessagesAdapter

    private var mediaRecorder: MediaRecorder? = null
    private var chatId: String? = null
    private var currentUserId: String? = null
    private var otherUserId: String? = null

    private val job = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + job)

    companion object {
        private const val REQUEST_CODE_PICK_MEDIA = 1001
        private const val REQUEST_CODE_MEDIA_PREVIEW = 1002
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200

        fun newInstance(chatId: String, currentUserId: String, otherUserId: String) = ChatFragment().apply {
            arguments = Bundle().apply {
                putString("chatId", chatId)
                putString("currentUserId", currentUserId)
                putString("otherUserId", otherUserId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this).get(ChatViewModel::class.java)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        chatId = arguments?.getString("chatId")
        currentUserId = arguments?.getString("currentUserId") ?: auth.currentUser?.uid
        otherUserId = arguments?.getString("otherUserId")

        setupRecyclerView()
        setupSendButton()
        setupAttachmentButton()
        setupMicButton()
        setupInputListeners()

        chatId?.let {
            coroutineScope.launch {
                checkAndCreateChat(it)
                observeMessages(it)
            }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.RECORD_AUDIO), REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    private fun setupRecyclerView() {
        adapter = MessagesAdapter(emptyList(), this)
        binding.recyclerViewMessages.layoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true
        }
        binding.recyclerViewMessages.adapter = adapter
    }

    private fun setupSendButton() {
        binding.sendButton.setOnClickListener {
            val messageText = binding.editTextMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(
                    messageText = messageText,
                    messageType = MessageType.TEXT,
                    mediaUri = null
                )
                binding.editTextMessage.text.clear()
            }
        }
    }

    private fun setupAttachmentButton() {
        binding.attachmentButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "video/*", "audio/*"))
            }
            startActivityForResult(intent, REQUEST_CODE_PICK_MEDIA)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_PICK_MEDIA && resultCode == AppCompatActivity.RESULT_OK) {
            val uri = data?.data
            uri?.let {
                if (!isValidFileSize(it)) {
                    Toast.makeText(context, "El archivo excede el tamaño máximo de 10 MB.", Toast.LENGTH_SHORT).show()
                    return
                }
                val intent = Intent(requireContext(), MediaPreviewActivity::class.java).apply {
                    putExtra("MEDIA_URI", it.toString())
                    putExtra("MEDIA_TYPE", getMessageTypeFromUri(it))
                }
                startActivityForResult(intent, REQUEST_CODE_MEDIA_PREVIEW)
            }
        } else if (requestCode == REQUEST_CODE_MEDIA_PREVIEW && resultCode == AppCompatActivity.RESULT_OK) {
            val mediaUri = data?.getStringExtra("MEDIA_URI")?.let { Uri.parse(it) }
            val mediaType = data?.getStringExtra("MEDIA_TYPE")
            if (mediaUri != null && mediaType != null) {
                showLoading()
                uploadMediaAndSendMessage(mediaUri, MessageType.valueOf(mediaType))
            }
        }
    }

    private fun isValidFileSize(uri: Uri): Boolean {
        val fileSize: Long = try {
            requireContext().contentResolver.openFileDescriptor(uri, "r")?.use {
                it.statSize
            } ?: return false
        } catch (e: Exception) {
            Log.e("ChatFragment", "Error al obtener el tamaño del archivo", e)
            return false
        }
        return fileSize <= 10 * 1024 * 1024 // 10 MB
    }

    private fun getMessageTypeFromUri(uri: Uri): String {
        val mimeType = requireContext().contentResolver.getType(uri)
        return when {
            mimeType?.startsWith("image/") == true -> "IMAGE"
            mimeType?.startsWith("video/") == true -> "VIDEO"
            mimeType?.startsWith("audio/") == true -> "AUDIO"
            else -> "UNKNOWN"
        }
    }

    private fun setupMicButton() {
        binding.micButton.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    startRecording()
                    true
                }
                MotionEvent.ACTION_UP -> {
                    stopRecording()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupInputListeners() {
        binding.editTextMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.isNullOrEmpty() || s.toString().trim().isEmpty()) {
                    binding.sendButton.visibility = View.GONE
                    binding.micButton.visibility = View.VISIBLE
                } else {
                    binding.sendButton.visibility = View.VISIBLE
                    binding.micButton.visibility = View.GONE
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.editTextMessage.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEND ||
                (event != null && event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_ENTER)) {
                val messageText = binding.editTextMessage.text.toString().trim()
                if (messageText.isNotEmpty()) {
                    sendMessage(
                        messageText = messageText,
                        messageType = MessageType.TEXT,
                        mediaUri = null
                    )
                    binding.editTextMessage.text.clear()
                }
                true
            } else {
                false
            }
        }
    }

    private fun startRecording() {
        val audioFilePath = "${requireContext().getExternalFilesDir(Environment.DIRECTORY_MUSIC)}/audio_${System.currentTimeMillis()}.3gp"
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            setOutputFile(audioFilePath)
            try {
                prepare()
                start()
                Toast.makeText(context, "Grabando audio...", Toast.LENGTH_SHORT).show()
            } catch (e: IOException) {
                Log.e("ChatFragment", "Error al iniciar la grabación", e)
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun uploadMediaAndSendMessage(uri: Uri, messageType: MessageType) {
        coroutineScope.launch {
            try {
                val storageRef = storage.reference.child("chat_media/${chatId}/${System.currentTimeMillis()}")
                val uploadTask = storageRef.putFile(uri)
                showUploadProgress(uploadTask)
                val downloadUrl = uploadTask.await().storage.downloadUrl.await()

                sendMessage(
                    messageText = "",
                    messageType = messageType,
                    mediaUri = downloadUrl
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Error al subir archivo: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("ChatFragment", "Error al subir archivo", e)
                }
            } finally {
                hideLoading()
            }
        }
    }

    private fun showUploadProgress(uploadTask: UploadTask) {
        binding.progressBar.visibility = View.VISIBLE
        binding.root.isEnabled = false
        binding.sendButton.isEnabled = false
        uploadTask.addOnProgressListener { taskSnapshot ->
            val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
            binding.progressBar.progress = progress
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.root.isEnabled = false
    }

    private fun hideLoading() {
        binding.progressBar.visibility = View.GONE
        binding.root.isEnabled = true
    }

    private fun sendMessage(messageText: String, messageType: MessageType, mediaUri: Uri?) {
        coroutineScope.launch {
            val message = Message(
                id = "",
                senderId = currentUserId.orEmpty(),
                messageText = messageText,
                messageType = messageType,
                timestamp = Timestamp.now(),
                imageUrl = if (messageType == MessageType.IMAGE) mediaUri.toString() else null,
                videoUrl = if (messageType == MessageType.VIDEO) mediaUri.toString() else null,
                audioUrl = if (messageType == MessageType.AUDIO) mediaUri.toString() else null
            )
            sendMessageToFirestore(message)
        }
    }

    private suspend fun sendMessageToFirestore(message: Message) {
        try {
            withContext(Dispatchers.IO) {
                val chatRef = db.collection("chats").document(chatId.orEmpty())

                val chatDoc = chatRef.get().await()

                if (!chatDoc.exists()) {
                    val participants = listOf(currentUserId.orEmpty(), otherUserId.orEmpty())
                    val chat = Chat(
                        id = chatId.orEmpty(),
                        participants = participants,
                        lastMessageText = message.messageText,
                        lastMessageTimestamp = message.timestamp,
                        lastMessageSentByCurrentUser = true,
                        isLastMessageRead = false
                    )
                    chatRef.set(chat).await()
                    Log.d("FirestoreOperation", "Chat creado: ${chat.id}")
                }

                val messagesRef = chatRef.collection("messages")
                val newMessageRef = messagesRef.document()
                val messageWithId = message.copy(id = newMessageRef.id)
                newMessageRef.set(messageWithId).await()

                chatRef.update(
                    mapOf(
                        "lastMessageText" to message.messageText,
                        "lastMessageTimestamp" to message.timestamp,
                        "lastMessageSentByCurrentUser" to (message.senderId == currentUserId),
                        "isLastMessageRead" to false
                    )
                ).await()

                withContext(Dispatchers.Main) {
                    Log.d("FirestoreOperation", "Mensaje enviado: ${messageWithId.id}")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (isActive) {
                    Toast.makeText(context, "Error al enviar el mensaje: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("FirestoreOperation", "Error al enviar el mensaje", e)
            }
        }
    }

    private suspend fun observeMessages(chatId: String) {
        withContext(Dispatchers.IO) {
            viewModel.loadMessages(chatId)
            withContext(Dispatchers.Main) {
                viewModel.messages.observe(viewLifecycleOwner) { messages ->
                    adapter.updateMessages(messages)
                    binding.recyclerViewMessages.scrollToPosition(messages.size - 1)
                    coroutineScope.launch {
                        markMessagesAsRead(chatId, messages)
                    }
                }
            }
        }
    }

    private suspend fun markMessagesAsRead(chatId: String, messages: List<Message>) {
        withContext(Dispatchers.IO) {
            messages.forEach { message ->
                if (!message.readBy.contains(currentUserId.orEmpty()) && message.id.isNotEmpty()) {
                    Log.d("MarkMessageAsRead", "Marking message ${message.id} as read")
                    viewModel.markMessageAsRead(chatId, message.id)
                } else {
                    Log.w("MarkMessageAsRead", "Message ID is empty or already read: ${message.id}")
                }
            }
        }
    }

    private suspend fun checkAndCreateChat(chatId: String) {
        try {
            withContext(Dispatchers.IO) {
                val chatDoc = db.collection("chats").document(chatId).get().await()
                if (!chatDoc.exists()) {
                    val participants = listOf(currentUserId.orEmpty(), otherUserId.orEmpty())
                    val chat = Chat(
                        id = chatId,
                        participants = participants,
                        lastMessageText = "",
                        lastMessageTimestamp = Timestamp.now(),
                        lastMessageSentByCurrentUser = false,
                        isLastMessageRead = true
                    )
                    db.collection("chats").document(chatId).set(chat).await()
                    Log.d("FirestoreOperation", "Chat created: $chatId")
                } else {
                    Log.d("FirestoreOperation", "Chat already exists: $chatId")
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                if (isActive) {
                    Toast.makeText(context, "Error al crear el chat: ${e.message}", Toast.LENGTH_SHORT).show()
                }
                Log.e("FirestoreOperation", "Error creating chat", e)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        job.cancel()
    }

    override fun onImageClick(message: Message) {
        val intent = Intent(requireContext(), MediaPreviewActivity::class.java).apply {
            putExtra("MEDIA_URI", message.imageUrl)
            putExtra("MEDIA_TYPE", "IMAGE")
            putExtra("IS_SENT", true) // Indica que la imagen ya fue enviada
        }
        startActivity(intent)
    }

    override fun onVideoClick(message: Message) {
        val intent = Intent(requireContext(), MediaPreviewActivity::class.java).apply {
            putExtra("MEDIA_URI", message.videoUrl)
            putExtra("MEDIA_TYPE", "VIDEO")
            putExtra("IS_SENT", true) // Indica que el video ya fue enviado
        }
        startActivity(intent)
    }


    override fun onAudioClick(message: Message) {
        // No hacer nada aquí para que el audio se reproduzca directamente en la interfaz del chat.
    }
}
