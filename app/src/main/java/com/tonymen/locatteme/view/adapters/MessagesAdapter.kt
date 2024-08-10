package com.tonymen.locatteme.view.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ItemMessageBinding
import com.tonymen.locatteme.model.Message
import java.text.SimpleDateFormat
import java.util.Locale

class MessagesAdapter(private var messages: List<Message>) :
    RecyclerView.Adapter<MessagesAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(val binding: ItemMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message, currentUserId: String?) {
            val isSentByCurrentUser = message.senderId == currentUserId

            if (isSentByCurrentUser) {
                // Mostrar layout de mensaje enviado
                binding.sentMessageLayout.visibility = View.VISIBLE
                binding.receivedMessageLayout.visibility = View.GONE
                binding.textViewMessageSent.text = message.messageText
                binding.textViewTimestampSent.text = formatTimestamp(message.timestamp)

                // Configurar el ícono de estado del mensaje
                binding.statusImageView.visibility = View.VISIBLE
                when {
                    message.readTimestamp != null -> binding.statusImageView.setImageResource(R.drawable.ic_double_tick_blue)
                    message.readBy.isNotEmpty() -> binding.statusImageView.setImageResource(R.drawable.ic_double_tick)
                    else -> binding.statusImageView.setImageResource(R.drawable.ic_single_tick)
                }
            } else {
                // Mostrar layout de mensaje recibido
                binding.sentMessageLayout.visibility = View.GONE
                binding.receivedMessageLayout.visibility = View.VISIBLE
                binding.textViewMessageReceived.text = message.messageText
                binding.textViewTimestampReceived.text = formatTimestamp(message.timestamp)

                // Ocultar el ícono de estado en los mensajes recibidos
                binding.statusImageView.visibility = View.GONE
            }
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
        messages = newMessages
        notifyDataSetChanged()
    }

    private fun formatTimestamp(timestamp: Timestamp): String {
        val dateFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return dateFormat.format(timestamp.toDate())
    }
}
