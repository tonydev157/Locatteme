//package com.tonymen.locatteme.view.adapters
//
//import android.content.Context
//import android.media.MediaMetadataRetriever
//import android.media.MediaPlayer
//import android.net.Uri
//import android.os.Environment
//import android.os.Handler
//import android.os.Looper
//import android.util.Log
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.SeekBar
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.bumptech.glide.Glide
//import com.google.firebase.auth.FirebaseAuth
//import com.google.firebase.storage.FirebaseStorage
//import com.tonymen.locatteme.R
//import com.tonymen.locatteme.databinding.ItemMessageBinding
//import com.tonymen.locatteme.model.Message
//import com.tonymen.locatteme.model.MessageType
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import java.io.File
//import java.util.concurrent.TimeUnit
//
//class MessagesAdapter(
//    var messages: List<Message>,
//    private val clickHandler: MessageClickHandler
//
//) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {
//
//    private var mediaPlayer: MediaPlayer? = null
//    private var currentPlayingMessageId: String? = null
//    private var currentViewHolder: MessageViewHolder? = null
//    private var handler: Handler = Handler(Looper.getMainLooper())
//
//    // Método para obtener la lista de mensajes
//
//
//    // Método para reproducir audio desde el adapter
//    fun playAudio(message: Message, isSentByCurrentUser: Boolean, viewHolder: MessageViewHolder) {
//        viewHolder.playAudio(message, isSentByCurrentUser)
//    }
//
//    inner class MessageViewHolder(val binding: ItemMessageBinding) :
//        RecyclerView.ViewHolder(binding.root) {
//
//        fun bind(message: Message) {
//            val isSentByCurrentUser = isSentByCurrentUser(message.senderId)
//            binding.message = message
//            binding.handler = clickHandler
//
//            if (isSentByCurrentUser) {
//                binding.sentMessageLayout.visibility = View.VISIBLE
//                binding.receivedMessageLayout.visibility = View.GONE
//                binding.textViewMessageSent.text = message.messageText
//                binding.textViewTimestampSent.text = clickHandler.formatTimestamp(message.timestamp)
//
//                binding.statusImageView.visibility = View.VISIBLE
//                binding.statusImageView.setImageResource(
//                    if (message.readBy.isNotEmpty()) R.drawable.ic_double_tick_blue else R.drawable.ic_single_tick
//                )
//
//                handleMediaContent(message, true)
//
//            } else {
//                binding.sentMessageLayout.visibility = View.GONE
//                binding.receivedMessageLayout.visibility = View.VISIBLE
//                binding.textViewMessageReceived.text = message.messageText
//                binding.textViewTimestampReceived.text = clickHandler.formatTimestamp(message.timestamp)
//
//                binding.statusImageView.visibility = View.GONE
//
//                handleMediaContent(message, false)
//            }
//        }
//
//        private fun isSentByCurrentUser(senderId: String): Boolean {
//            return senderId == FirebaseAuth.getInstance().currentUser?.uid
//        }
//
//        private fun handleMediaContent(message: Message, isSent: Boolean) {
//            when (message.messageType) {
//                MessageType.AUDIO -> {
//                    val audioLayout = if (isSent) binding.audioLayoutSent else binding.audioLayoutReceived
//                    val playPauseButton = if (isSent) binding.playPauseButtonSent else binding.playPauseButtonReceived
//                    val seekBar = if (isSent) binding.audioSeekBarSent else binding.audioSeekBarReceived
//                    val durationText = if (isSent) binding.audioDurationSent else binding.audioDurationReceived
//                    val downloadButton = if (isSent) binding.downloadButtonSent else binding.downloadButtonReceived
//                    val progressBar = if (isSent) binding.downloadProgressBarSent else binding.downloadProgressBarReceived
//
//                    audioLayout.visibility = View.VISIBLE
//
//                    val localPath = getLocalMediaPath(message, MessageType.AUDIO)
//                    if (localPath != null && File(localPath).exists()) {
//                        updateAudioDuration(localPath, durationText, seekBar)
//                        playPauseButton.visibility = View.VISIBLE
//                        playPauseButton.isEnabled = true
//                        downloadButton.visibility = View.GONE
//                    } else {
//                        // Lógica adicional para descarga automática si es un archivo enviado
//                        if (isSent) {
//                            // Iniciar descarga automática para audios enviados
//                            downloadMediaAndRetrieveDuration(
//                                binding.root.context, message.audioUrl ?: "", message,
//                                durationText, seekBar, playPauseButton, progressBar, downloadButton
//                            )
//                        } else {
//                            playPauseButton.visibility = View.GONE
//                            downloadButton.visibility = View.VISIBLE
//                            downloadButton.setOnClickListener {
//                                progressBar.visibility = View.VISIBLE
//                                downloadMediaAndRetrieveDuration(
//                                    binding.root.context, message.audioUrl ?: "", message,
//                                    durationText, seekBar, playPauseButton, progressBar, downloadButton
//                                )
//                            }
//                        }
//                    }
//
//                    playPauseButton.setOnClickListener {
//                        currentViewHolder = this
//                        playAudio(message, isSent)
//                    }
//                }
//
//                MessageType.IMAGE -> {
//                    val imageView = if (isSent) binding.imageViewSent else binding.imageViewReceived
//                    val localPath = getLocalMediaPath(message, MessageType.IMAGE)
//
//                    if (localPath != null && File(localPath).exists()) {
//                        // Cargar desde el almacenamiento local
//                        Glide.with(binding.root.context).load(File(localPath)).into(imageView)
//                    } else {
//                        // Cargar desde la URL remota
//                        Glide.with(binding.root.context).load(message.imageUrl).into(imageView)
//                    }
//                    imageView.visibility = View.VISIBLE
//                    imageView.setOnClickListener { clickHandler.onImageClick(message) }
//                }
//
//                MessageType.VIDEO -> {
//                    val videoView = if (isSent) binding.videoViewSent else binding.videoViewReceived
//                    val localPath = getLocalMediaPath(message, MessageType.VIDEO)
//
//                    if (localPath != null && File(localPath).exists()) {
//                        videoView.setVideoURI(Uri.fromFile(File(localPath)))
//                    } else {
//                        videoView.setVideoURI(Uri.parse(message.videoUrl))
//                    }
//                    videoView.visibility = View.VISIBLE
//                    videoView.setOnClickListener { clickHandler.onVideoClick(message) }
//                }
//
//                MessageType.TEXT -> {
//                    val textView = if (isSent) binding.textViewMessageSent else binding.textViewMessageReceived
//                    textView.visibility = View.VISIBLE
//                    textView.text = message.messageText
//                }
//            }
//        }
//
//        private fun downloadMediaAndRetrieveDuration(
//            context: Context, audioUrl: String, message: Message,
//            durationText: TextView, seekBar: SeekBar, playPauseButton: View,
//            progressBar: View, downloadButton: View
//        ) {
//            val storageReference = FirebaseStorage.getInstance().getReferenceFromUrl(audioUrl)
//            val localFile = File(context.getExternalFilesDir(null), "locatteme/${message.senderId}/Audios/${message.id}.3gp")
//
//            // Muestra el ProgressBar y oculta los otros botones
//            progressBar.visibility = View.VISIBLE
//            playPauseButton.visibility = View.GONE
//            downloadButton.visibility = View.GONE
//
//            storageReference.getFile(localFile).addOnSuccessListener {
//                // Actualiza la duración del audio una vez descargado
//                updateAudioDuration(localFile.absolutePath, durationText, seekBar)
//
//                // Guarda el archivo localmente para usos futuros
//                saveDownloadedMediaLocally(context, localFile, message.messageType, message.senderId == FirebaseAuth.getInstance().currentUser?.uid)
//
//                // Oculta el ProgressBar y muestra el botón de reproducción
//                progressBar.visibility = View.GONE
//                playPauseButton.visibility = View.VISIBLE
//                playPauseButton.isEnabled = true
//                downloadButton.visibility = View.GONE
//            }.addOnFailureListener { exception ->
//                Log.e("MessagesAdapter", "Error al descargar el archivo de audio", exception)
//
//                // Si falla la descarga, vuelve a mostrar el botón de descarga
//                progressBar.visibility = View.GONE
//                playPauseButton.visibility = View.GONE
//                downloadButton.visibility = View.VISIBLE
//            }
//        }
//
//
//
//        private fun updateAudioDuration(localPath: String, durationText: TextView, seekBar: SeekBar) {
//            val retriever = MediaMetadataRetriever()
//            retriever.setDataSource(localPath)
//            val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
//            val duration = durationStr?.toIntOrNull() ?: 0
//            retriever.release()
//
//            val formattedDuration = formatTime(duration)
//            durationText.text = formattedDuration
//            seekBar.max = duration
//        }
//
//        // Corregido dentro de tu adaptador:
//        fun getLocalMediaPath(context: Context, message: Message, messageType: MessageType): String? {
//            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return null
//
//            val baseDir = when (messageType) {
//                MessageType.IMAGE -> {
//                    if (message.senderId == userId) {
//                        File(context.getExternalFilesDir(null), "locatteme/$userId/Imagenes/sent")
//                    } else {
//                        File(context.getExternalFilesDir(null), "locatteme/$userId/Imagenes")
//                    }
//                }
//                MessageType.VIDEO -> {
//                    if (message.senderId == userId) {
//                        File(context.getExternalFilesDir(null), "locatteme/$userId/Videos/sent")
//                    } else {
//                        File(context.getExternalFilesDir(null), "locatteme/$userId/Videos")
//                    }
//                }
//                MessageType.AUDIO -> {
//                    if (message.senderId == userId) {
//                        File(context.getExternalFilesDir(null), "locatteme/$userId/Audios/sent")
//                    } else {
//                        File(context.getExternalFilesDir(null), "locatteme/$userId/Audios")
//                    }
//                }
//                else -> return null
//            }
//
//            val localFile = File(baseDir, "${message.id}.${getFileExtension(messageType)}")
//            return if (localFile.exists()) localFile.absolutePath else null
//        }
//
//
//
//        private fun getFileExtension(messageType: MessageType): String {
//            return when (messageType) {
//                MessageType.IMAGE -> "jpg"
//                MessageType.VIDEO -> "mp4"
//                MessageType.AUDIO -> "3gp"
//                else -> ""
//            }
//        }
//
//        private fun saveDownloadedMediaLocally(context: Context, file: File, messageType: MessageType, isSent: Boolean) {
//            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
//
//            val userBaseDir = File(context.getExternalFilesDir(null), "locatteme/$currentUserId")
//            if (!userBaseDir.exists()) {
//                userBaseDir.mkdirs()
//            }
//
//            val baseDir = when (messageType) {
//                MessageType.IMAGE -> File(userBaseDir, "Imagenes")
//                MessageType.VIDEO -> File(userBaseDir, "Videos")
//                MessageType.AUDIO -> File(userBaseDir, "Audios")
//                else -> return
//            }
//
//            if (!baseDir.exists()) {
//                baseDir.mkdirs()
//            }
//
//            val finalDir = if (isSent) {
//                val sentDir = File(baseDir, "sent")
//                if (!sentDir.exists()) {
//                    sentDir.mkdirs()
//                }
//                sentDir
//            } else {
//                baseDir
//            }
//
//            // Usa el message.id como nombre del archivo para garantizar la consistencia
//            val finalFile = File(finalDir, "${file.nameWithoutExtension}.${getFileExtension(messageType)}")
//
//            file.copyTo(finalFile, overwrite = true)
//            file.delete()
//
//            Log.d("MessagesAdapter", "Archivo guardado en: ${finalFile.absolutePath}")
//        }
//
//
//        fun setPlayButtonIcon(iconRes: Int) {
//            binding.playPauseButtonSent.setImageResource(iconRes)
//            binding.playPauseButtonReceived.setImageResource(iconRes)
//        }
//
//        fun updateSeekBar(position: Int) {
//            binding.audioSeekBarSent.progress = position
//            binding.audioSeekBarReceived.progress = position
//        }
//
//        fun resetAudioLayout() {
//            setPlayButtonIcon(R.drawable.ic_play_arrow)
//            updateSeekBar(0)
//            binding.audioDurationSent.text = ""
//            binding.audioDurationReceived.text = ""
//        }
//
//        fun updateAudioTimeText(currentPosition: Int, duration: Int, textView: TextView) {
//            val currentTime = formatTime(currentPosition)
//            val totalTime = formatTime(duration)
//            textView.text = "$currentTime/$totalTime"
//        }
//
//        private fun formatTime(time: Int): String {
//            return String.format(
//                "%02d:%02d",
//                TimeUnit.MILLISECONDS.toMinutes(time.toLong()) % TimeUnit.HOURS.toMinutes(1),
//                TimeUnit.MILLISECONDS.toSeconds(time.toLong()) % TimeUnit.MINUTES.toSeconds(1)
//            )
//        }
//
//        fun getDurationTextView(isSentByCurrentUser: Boolean): TextView {
//            return if (isSentByCurrentUser) {
//                binding.audioDurationSent
//            } else {
//                binding.audioDurationReceived
//            }
//        }
//
//        fun playAudio(message: Message, isSentByCurrentUser: Boolean) {
//            currentViewHolder?.resetAudioLayout()
//
//            val localPath = getLocalMediaPath(message, MessageType.AUDIO)
//            val audioPath = localPath ?: message.audioUrl
//
//            if (audioPath != null) {
//                if (mediaPlayer != null && currentPlayingMessageId == message.id) {
//                    if (mediaPlayer!!.isPlaying) {
//                        mediaPlayer!!.pause()
//                        setPlayButtonIcon(R.drawable.ic_play_arrow)
//                    } else {
//                        mediaPlayer!!.start()
//                        setPlayButtonIcon(R.drawable.ic_pause)
//                        updateSeekBar(this)
//                    }
//                } else {
//                    mediaPlayer?.release()
//                    mediaPlayer = MediaPlayer().apply {
//                        if (localPath != null) {
//                            setDataSource(localPath)
//                        } else {
//                            setDataSource(binding.root.context, Uri.parse(audioPath))
//                        }
//                        setOnPreparedListener { mp ->
//                            val duration = mp.duration
//                            updateAudioTimeText(0, duration, getDurationTextView(isSentByCurrentUser))
//                            updateSeekBar(0)
//                            mp.start()
//                            updateSeekBar(this@MessageViewHolder)
//                        }
//                        setOnCompletionListener {
//                            resetAudioLayout()
//                            currentPlayingMessageId = null
//                            handler.removeCallbacksAndMessages(null)
//                        }
//                        prepareAsync()
//                    }
//                    currentPlayingMessageId = message.id
//                    setPlayButtonIcon(R.drawable.ic_pause)
//                }
//            } else {
//                Log.e("MessagesAdapter", "No se puede reproducir el audio. Ruta nula.")
//            }
//        }
//
//
//        private fun updateSeekBar(viewHolder: MessageViewHolder) {
//            handler.post(object : Runnable {
//                override fun run() {
//                    if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
//                        viewHolder.updateSeekBar(mediaPlayer!!.currentPosition)
//                        viewHolder.updateAudioTimeText(
//                            mediaPlayer!!.currentPosition,
//                            mediaPlayer!!.duration,
//                            viewHolder.getDurationTextView(currentPlayingMessageId == mediaPlayer!!.audioSessionId.toString())
//                        )
//                        handler.postDelayed(this, 500)
//                    } else {
//                        handler.removeCallbacks(this)
//                    }
//                }
//            })
//        }
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
//        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
//        return MessageViewHolder(binding)
//    }
//
//    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
//        holder.bind(messages[position])
//    }
//
//    override fun getItemCount(): Int = messages.size
//
//    fun updateMessages(newMessages: List<Message>) {
//        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
//            override fun getOldListSize(): Int = messages.size
//            override fun getNewListSize(): Int = newMessages.size
//
//            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//                return messages[oldItemPosition].id == newMessages[newItemPosition].id
//            }
//
//            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
//                return messages[oldItemPosition] == newMessages[newItemPosition]
//            }
//        })
//        messages = newMessages
//        diffResult.dispatchUpdatesTo(this)
//    }
//}
