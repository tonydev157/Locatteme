package com.tonymen.locatteme.view.adapters

import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ItemMessageBinding
import com.tonymen.locatteme.model.Message
import com.tonymen.locatteme.model.MessageType

class MessagesAdapter(
    private var messages: List<Message>,
    private val clickHandler: MessageClickHandler
) : RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentPlayingPosition: Int = -1
    private var currentPlayingViewHolder: MessageViewHolder? = null

    inner class MessageViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val handler = Handler(Looper.getMainLooper())
        private val updateSeekBar = object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    if (bindingAdapterPosition == currentPlayingPosition) {
                        if (isSentByCurrentUser()) {
                            binding.audioSeekBarSent.progress = it.currentPosition
                        } else {
                            binding.audioSeekBarReceived.progress = it.currentPosition
                        }
                        handler.postDelayed(this, 1000)
                    }
                }
            }
        }

        fun bind(message: Message, currentUserId: String?) {
            val isSentByCurrentUser = isSentByCurrentUser()

            binding.message = message
            binding.handler = clickHandler

            if (isSentByCurrentUser) {
                binding.sentMessageLayout.visibility = View.VISIBLE
                binding.receivedMessageLayout.visibility = View.GONE
                binding.textViewMessageSent.text = message.messageText
                binding.textViewTimestampSent.text = clickHandler.formatTimestamp(message.timestamp)

                binding.statusImageView.visibility = View.VISIBLE
                binding.statusImageView.setImageResource(
                    if (message.readBy.isNotEmpty()) R.drawable.ic_double_tick_blue else R.drawable.ic_single_tick
                )

                handleMediaContent(message, true)

            } else {
                binding.sentMessageLayout.visibility = View.GONE
                binding.receivedMessageLayout.visibility = View.VISIBLE
                binding.textViewMessageReceived.text = message.messageText
                binding.textViewTimestampReceived.text = clickHandler.formatTimestamp(message.timestamp)

                binding.statusImageView.visibility = View.GONE

                handleMediaContent(message, false)
            }
        }

        private fun isSentByCurrentUser() = binding.message?.senderId == FirebaseAuth.getInstance().currentUser?.uid

        private fun handleMediaContent(message: Message, isSent: Boolean) {
            when (message.messageType) {
                MessageType.IMAGE -> {
                    val imageView = if (isSent) binding.imageViewSent else binding.imageViewReceived
                    imageView.visibility = View.VISIBLE
                    Glide.with(binding.root.context).load(message.imageUrl).into(imageView)
                    imageView.setOnClickListener { clickHandler.onImageClick(message) }
                }
                MessageType.VIDEO -> {
                    val videoView = if (isSent) binding.videoViewSent else binding.videoViewReceived
                    videoView.visibility = View.VISIBLE
                    videoView.setOnClickListener { clickHandler.onVideoClick(message) }
                }
                MessageType.AUDIO -> {
                    val audioLayout = if (isSent) binding.audioLayoutSent else binding.audioLayoutReceived
                    val playPauseButton = if (isSent) binding.playPauseButtonSent else binding.playPauseButtonReceived
                    val seekBar = if (isSent) binding.audioSeekBarSent else binding.audioSeekBarReceived

                    audioLayout.visibility = View.VISIBLE

                    playPauseButton.setOnClickListener {
                        Log.d("AudioPlayback", "Botón play/pause clicado para posición $bindingAdapterPosition")
                        if (currentPlayingPosition == bindingAdapterPosition) {
                            // Toggle Play/Pause
                            mediaPlayer?.let {
                                if (it.isPlaying) {
                                    Log.d("AudioPlayback", "Pausando reproducción")
                                    it.pause()
                                    playPauseButton.setImageResource(R.drawable.ic_play_arrow)
                                    handler.removeCallbacks(updateSeekBar)
                                } else {
                                    Log.d("AudioPlayback", "Reanudando reproducción")
                                    it.start()
                                    playPauseButton.setImageResource(R.drawable.ic_pause)
                                    handler.postDelayed(updateSeekBar, 1000)
                                }
                            }
                        } else {
                            Log.d("AudioPlayback", "Iniciando nueva reproducción para mensaje ${message.id}")
                            playNewAudio(message, seekBar, playPauseButton)
                        }
                    }

                    seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                            if (fromUser && mediaPlayer != null) {
                                mediaPlayer?.seekTo(progress)
                            }
                        }

                        override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                        override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                    })
                }
                else -> {
                    if (isSent) {
                        binding.imageViewSent.visibility = View.GONE
                        binding.videoViewSent.visibility = View.GONE
                        binding.audioLayoutSent.visibility = View.GONE
                    } else {
                        binding.imageViewReceived.visibility = View.GONE
                        binding.videoViewReceived.visibility = View.GONE
                        binding.audioLayoutReceived.visibility = View.GONE
                    }
                }
            }
        }

        private fun playNewAudio(message: Message, seekBar: SeekBar, playPauseButton: ImageButton) {
            if (mediaPlayer != null) {
                mediaPlayer?.stop()
                mediaPlayer?.release()
                mediaPlayer = null
                currentPlayingViewHolder?.resetAudioLayout()
            }

            val audioUrl = message.audioUrl
            if (audioUrl.isNullOrEmpty()) {
                Log.e("AudioError", "URI no válida o vacía: $audioUrl")
                resetAudioLayout()
                return
            }

            val audioUri = Uri.parse(audioUrl)

            try {
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(binding.root.context, audioUri)
                    prepare()
                    start()
                    currentPlayingPosition = bindingAdapterPosition
                    currentPlayingViewHolder = this@MessageViewHolder

                    playPauseButton.setImageResource(R.drawable.ic_pause)
                    seekBar.max = duration

                    handler.postDelayed(updateSeekBar, 1000)

                    setOnCompletionListener {
                        resetAudioLayout()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Log.e("AudioError", "Error reproduciendo audio: ${e.message}")
                resetAudioLayout()
            }
        }

        private fun resetAudioLayout() {
            binding.playPauseButtonSent.setImageResource(R.drawable.ic_play_arrow)
            binding.playPauseButtonReceived.setImageResource(R.drawable.ic_play_arrow)
            binding.audioSeekBarSent.progress = 0
            binding.audioSeekBarReceived.progress = 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        holder.bind(messages[position], currentUserId)
    }

    override fun getItemCount(): Int = messages.size

    fun updateMessages(newMessages: List<Message>) {
        val diffResult = androidx.recyclerview.widget.DiffUtil.calculateDiff(object : androidx.recyclerview.widget.DiffUtil.Callback() {
            override fun getOldListSize(): Int = messages.size
            override fun getNewListSize(): Int = newMessages.size

            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition].id == newMessages[newItemPosition].id
            }

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
                return messages[oldItemPosition] == newMessages[newItemPosition]
            }
        })
        messages = newMessages
        diffResult.dispatchUpdatesTo(this)
    }
}
