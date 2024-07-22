package com.tonymen.locatteme.view.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.tonymen.locatteme.databinding.ItemChatBinding
import com.tonymen.locatteme.model.Chat
import com.google.firebase.auth.FirebaseAuth

class ActiveChatsAdapter(
    private val context: Context,
    private val onChatClick: (String, String) -> Unit
) : RecyclerView.Adapter<ActiveChatsAdapter.ChatViewHolder>() {

    private val chats = mutableListOf<Chat>()
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    fun submitList(chatList: List<Chat>) {
        chats.clear()
        chats.addAll(chatList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(context), parent, false)
        return ChatViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(chats[position])
    }

    override fun getItemCount(): Int = chats.size

    inner class ChatViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Chat) {
            binding.chat = chat
            binding.currentUserId = currentUserId
            binding.executePendingBindings()

            binding.root.setOnClickListener {
                val otherUserId = chat.participants.first { it != currentUserId }
                onChatClick(chat.id, otherUserId)
            }
        }
    }
}
