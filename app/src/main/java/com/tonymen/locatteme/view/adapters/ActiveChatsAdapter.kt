package com.tonymen.locatteme.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ItemChatBinding
import com.tonymen.locatteme.model.Chat
import com.tonymen.locatteme.utils.ChatTimestampUtil


data class ChatDisplay(
    val chat: Chat,
    val userName: String,
    val userProfileImageUrl: String
)

class ActiveChatsAdapter(
    private var chats: List<ChatDisplay>,
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ActiveChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chatDisplay: ChatDisplay) {
            // Carga de la imagen de perfil
            Glide.with(binding.root.context)
                .load(chatDisplay.userProfileImageUrl.ifEmpty { R.drawable.ic_profile_placeholder })
                .circleCrop()
                .into(binding.profileImageView)

            binding.usernameTextView.text = chatDisplay.userName // Mostramos el nombre del usuario
            binding.lastMessageTextView.text = chatDisplay.chat.lastMessageText

            // Formatear y mostrar la fecha/hora correcta
            val formattedTimestamp = ChatTimestampUtil.formatChatTimestamp(chatDisplay.chat.lastMessageTimestamp)
            binding.timestampTextView.text = formattedTimestamp

            binding.root.setOnClickListener {
                onClick(chatDisplay.chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount() = chats.size

    fun updateData(newChats: List<ChatDisplay>) {
        chats = newChats
        notifyDataSetChanged()
    }
}
