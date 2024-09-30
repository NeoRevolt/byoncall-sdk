package com.dartmedia.brandedlibrary.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.dartmedia.brandedlibrary.R
import com.dartmedia.brandedlibrary.database.CallLog
import com.dartmedia.brandedlibrary.database.helper.CallLogDiffCallback
import com.dartmedia.brandedlibrary.databinding.ItemCallLogBinding
import com.dartmedia.brandedlibrary.ui.HistoryDetailActivity
import com.dartmedia.brandedlibrary.utils.image.WhiteBackgroundTransformation

class CallLogAdapter : RecyclerView.Adapter<CallLogAdapter.CallLogViewHolder>() {

    private val listLogs = ArrayList<CallLog>()

    fun setListLogs(listLogs: List<CallLog>) {
        val diffCallback = CallLogDiffCallback(this.listLogs, listLogs)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        this.listLogs.clear()
        this.listLogs.addAll(listLogs)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallLogViewHolder {
        val binding = ItemCallLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CallLogViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return listLogs.size
    }

    override fun onBindViewHolder(holder: CallLogViewHolder, position: Int) {
        holder.bind(listLogs[position])
    }

    inner class CallLogViewHolder(private val binding: ItemCallLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(log: CallLog) {
            with(binding) {
                if (log.image != null) {
                    Glide.with(binding.root.context)
                        .load(log.image)
                        .transform(WhiteBackgroundTransformation())
                        .apply(
                            RequestOptions()
                                .placeholder(R.drawable.rounded_message_gray)
                                .error(R.drawable.asset_person_ic)
                                .circleCrop()
                        )
                        .into(callIconIV)
                } else {
                    Glide.with(binding.root.context)
                        .load(R.drawable.asset_person_ic)
                        .apply(RequestOptions.circleCropTransform())
                        .into(callIconIV)
                }
                usernameTv.text = log.caller
                callerId.text = log.phone
                callMessageTv.text = log.message
                callDateTv.text = log.date
                callDurationTv.text = log.duration + " seconds"
                itemCard.setOnClickListener {
                    val intent = Intent(it.context, HistoryDetailActivity::class.java).apply {
                        putExtra(HistoryDetailActivity.CALLER_NAME, log.caller)
                        putExtra(HistoryDetailActivity.CALLER_NUMBER, log.phone)
                        putExtra(HistoryDetailActivity.CALLER_IMAGE, log.image)
                        putExtra(HistoryDetailActivity.MESSAGE, log.message)
                        putExtra(HistoryDetailActivity.CALL_DURATION, log.duration)
                        putExtra(HistoryDetailActivity.DATE, log.date)
                        putExtra(HistoryDetailActivity.CALL_TYPE, "Incoming Call") // TODO
                    }
                    it.context.startActivity(intent)
                }
            }
        }
    }
}