package com.sendbird.uikit.activities.adapter;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sendbird.android.channel.GroupChannel;
import com.sendbird.android.message.BaseMessage;
import com.sendbird.android.message.FileMessage;
import com.sendbird.android.message.UserMessage;
import com.sendbird.uikit.activities.viewholder.MessageType;
import com.sendbird.uikit.activities.viewholder.MessageViewHolder;
import com.sendbird.uikit.model.MessageListUIParams;
import com.sendbird.uikit.utils.MessageUtils;

/**
 * ThreadListAdapter provides a binding from a thread message type data set to views that are displayed within a RecyclerView.
 *
 * @since 3.3.0
 */
public class ThreadListAdapter extends BaseMessageListAdapter {

    public ThreadListAdapter(@Nullable GroupChannel channel, @NonNull MessageListUIParams messageListUIParams) {
        super(channel, messageListUIParams);
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return super.onCreateViewHolder(parent, viewType);
    }

    @Override
    public int getItemViewType(int position) {
        final BaseMessage message = getItem(position);
        if (position == getItemCount()-1 &&
                !MessageUtils.hasParentMessage(message) &&
                (message instanceof UserMessage || message instanceof FileMessage)) {
            return MessageType.VIEW_TYPE_PARENT_MESSAGE_INFO.getValue();
        }
        return super.getItemViewType(position);
    }
}
