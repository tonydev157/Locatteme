package com.tonymen.locatteme.view.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ItemFollowerUserBinding
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.view.HomeFragments.UserProfileFragment

class FollowersNAdapter(
    private var followers: List<User>,
    private val context: Context
) : RecyclerView.Adapter<FollowersNAdapter.FollowerViewHolder>() {

    class FollowerViewHolder(val binding: ItemFollowerUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowerViewHolder {
        val binding = ItemFollowerUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FollowerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowerViewHolder, position: Int) {
        val user = followers[position]

        Log.d("FollowersNAdapter", "User: $user")

        holder.binding.user = user
        holder.binding.executePendingBindings()

        Glide.with(holder.binding.profileImage.context)
            .load(user.profileImageUrl)
            .circleCrop()
            .into(holder.binding.profileImage)

        holder.itemView.setOnClickListener {
            Log.d("FollowersNAdapter", "Clicked on user: $user")
            val fragment = UserProfileFragment.newInstance(user.id)
            val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    override fun getItemCount(): Int = followers.size

    fun updateFollowers(newFollowers: List<User>) {
        followers = newFollowers
        notifyDataSetChanged()
    }
}
