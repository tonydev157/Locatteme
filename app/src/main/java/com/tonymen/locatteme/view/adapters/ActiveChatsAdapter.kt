package com.tonymen.locatteme.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.databinding.ItemChatBinding
import com.tonymen.locatteme.model.chatmodels.Chat

data class ChatDisplay(
    val chat: Chat,
    val userName: String,
    val userProfileImageUrl: String,
    val unreadCount: Int = 0  // Contador de mensajes no leídos
)

class ActiveChatsAdapter(
    private var chats: List<ChatDisplay>,
    private val onClick: (Chat) -> Unit
) : RecyclerView.Adapter<ActiveChatsAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chatDisplay: ChatDisplay) {
            // Otros bindings...
            binding.usernameTextView.text = chatDisplay.userName
            binding.lastMessageTextView.text = chatDisplay.chat.lastMessageText
            Glide.with(binding.profileImageView.context)
                .load(chatDisplay.userProfileImageUrl)
                .circleCrop()
                .into(binding.profileImageView)

            if (chatDisplay.unreadCount > 0) {
                binding.unreadCountTextView.text = chatDisplay.unreadCount.toString()
                binding.unreadCountTextView.visibility = View.VISIBLE
            } else {
                binding.unreadCountTextView.visibility = View.GONE
            }

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

    override fun getItemCount(): Int = chats.size

    fun updateData(newChats: List<ChatDisplay>) {
        chats = newChats
        notifyDataSetChanged()
    }
}
