package com.tonymen.locatteme.view.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tonymen.locatteme.R
import com.tonymen.locatteme.databinding.ItemFollowingUserBinding
import com.tonymen.locatteme.model.User
import com.tonymen.locatteme.view.HomeFragments.UserProfileFragment

class FollowingNAdapter(
    private var following: List<User>,
    private val context: Context
) : RecyclerView.Adapter<FollowingNAdapter.FollowingViewHolder>() {

    class FollowingViewHolder(val binding: ItemFollowingUserBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FollowingViewHolder {
        val binding = ItemFollowingUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return FollowingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FollowingViewHolder, position: Int) {
        val user = following[position]

        holder.binding.username.text = user.username
        holder.binding.fullName.text = "${user.nombre} ${user.apellido}" // Verificar estos campos
        Glide.with(holder.binding.profileImage.context).load(user.profileImageUrl).circleCrop().into(holder.binding.profileImage)

        holder.itemView.setOnClickListener {
            val fragment = UserProfileFragment.newInstance(user.id)
            val transaction = (context as AppCompatActivity).supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragmentContainer, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
        }
    }

    override fun getItemCount(): Int = following.size

    fun updateFollowing(newFollowing: List<User>) {
        following = newFollowing
        notifyDataSetChanged()
    }
}
