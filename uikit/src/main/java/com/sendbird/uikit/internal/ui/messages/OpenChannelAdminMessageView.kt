package com.sendbird.uikit.internal.ui.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.sendbird.android.message.BaseMessage
import com.sendbird.uikit.R
import com.sendbird.uikit.databinding.SbViewOpenChannelAdminMessageComponentBinding
import com.sendbird.uikit.internal.extensions.setAppearance
import com.sendbird.uikit.utils.DrawableUtils
import com.sendbird.uikit.utils.ViewUtils

internal class OpenChannelAdminMessageView @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.sb_widget_admin_message
) : BaseMessageView(context, attrs, defStyle) {
    override val binding: SbViewOpenChannelAdminMessageComponentBinding
    override val layout: View
        get() = binding.root

    init {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.MessageView, defStyle, 0)
        try {
            binding = SbViewOpenChannelAdminMessageComponentBinding.inflate(
                LayoutInflater.from(getContext()),
                this,
                true
            )
            val avatar = a.getResourceId(
                R.styleable.MessageView_File_sb_quoted_message_me_file_icon_tint,
                R.drawable.icon_members
            )
            val messageBackground = a.getResourceId(
                R.styleable.MessageView_User_sb_message_other_background,
                R.drawable.sb_shape_chat_bubble
            )
            val messageBackgroundTint =
                a.getColorStateList(R.styleable.MessageView_User_sb_message_other_background_tint)
            val textAppearance = a.getResourceId(
                R.styleable.MessageView_Admin_sb_admin_message_text_appearance,
                R.style.SendbirdCaption2OnLight02
            )
            val backgroundResourceId = a.getResourceId(
                R.styleable.MessageView_Admin_sb_admin_message_background,
                R.drawable.sb_shape_admin_message_background_light
            )
            binding.contentPanel.background =
                DrawableUtils.setTintList(context, messageBackground, messageBackgroundTint)
            binding.tvMessage.setAppearance(context, textAppearance)
            binding.tvNickname.setAppearance(context, textAppearance)
            binding.tvSentAt.setAppearance(context, textAppearance)

            //binding.tvMessage.setBackgroundResource(backgroundResourceId)
        } finally {
            a.recycle()
        }
    }

    fun drawMessage(message: BaseMessage) {
        binding.tvMessage.text = message.message
        binding.tvNickname.text = message.sender?.nickname ?: ""
        binding.tvSentAt.text = message.createdAt.toString()

    }
}
