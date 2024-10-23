package com.dartmedia.brandedlibraryclient.adapter

import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.dartmedia.brandedlibraryclient.databinding.ItemChatOtherBinding
import com.dartmedia.brandedlibraryclient.databinding.ItemChatSelfBinding
import com.dartmedia.brandedlibraryclient.model.ChatModel

class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val ITEM_SELF = 1
    private val ITEM_OTHER = 2

    private val diffCallback = object : DiffUtil.ItemCallback<ChatModel>() {
        override fun areItemsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: ChatModel, newItem: ChatModel): Boolean {
            return oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, diffCallback)

    fun submitChat(chatModels: List<ChatModel>) {
        differ.submitList(chatModels)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == ITEM_SELF) {
            val binding =
                ItemChatSelfBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            SelfChatItemViewHolder(binding)
        } else {
            val binding =
                ItemChatOtherBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            OtherChatItemViewHolder(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val chat = differ.currentList[position]
        if (chat.isSelf) {
            (holder as SelfChatItemViewHolder).bind(chat)
        } else {
            (holder as OtherChatItemViewHolder).bind(chat)
        }
    }


    inner class OtherChatItemViewHolder(private val binding: ItemChatOtherBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chatModel: ChatModel) {
            binding.apply {
                name.text = chatModel.senderId
                msg.text = chatModel.message
                tvTimestamp.text =
                    chatModel.createAt.let { DateUtils.getRelativeTimeSpanString(it) }
            }
        }
    }

    inner class SelfChatItemViewHolder(private val binding: ItemChatSelfBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chatModel: ChatModel) {
            binding.apply {
                name.isVisible = false
                msg.text = chatModel.message
                tvTimestamp.text =
                    chatModel.createAt.let { DateUtils.getRelativeTimeSpanString(it) }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        val chat = differ.currentList[position]
        return if (chat.isSelf) ITEM_SELF else ITEM_OTHER
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }
}