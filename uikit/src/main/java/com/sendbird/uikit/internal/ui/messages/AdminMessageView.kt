package com.sendbird.uikit.internal.ui.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.sendbird.android.message.BaseMessage
import com.sendbird.uikit.R
import com.sendbird.uikit.databinding.SbViewAdminMessageComponentBinding
import com.sendbird.uikit.internal.extensions.setAppearance
import com.sendbird.uikit.utils.DrawableUtils
import com.sendbird.uikit.utils.ViewUtils

internal class AdminMessageView @JvmOverloads internal constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = R.attr.sb_widget_admin_message
) : BaseMessageView(context, attrs, defStyle) {
    override val binding: SbViewAdminMessageComponentBinding
    override val layout: View
        get() = binding.root

    init {
        val a =
            context.theme.obtainStyledAttributes(attrs, R.styleable.MessageView_User, defStyle, 0)
        try {
            binding = SbViewAdminMessageComponentBinding.inflate(
                LayoutInflater.from(context),
                this,
                true
            )
            val textAppearance = a.getResourceId(
                R.styleable.MessageView_User_sb_message_me_text_appearance,
                R.style.SendbirdCaption2OnLight02
            )
            val backgroundResourceId = a.getResourceId(
                R.styleable.MessageView_User_sb_message_me_background,
                android.R.color.holo_blue_light
            )
            val messageBackground = a.getResourceId(
                R.styleable.MessageView_User_sb_message_me_background,
                R.drawable.sb_shape_chat_bubble
            )
            val messageBackgroundTint = android.R.color.holo_blue_light

            binding.contentPanel.background =
                DrawableUtils.setTintList(context, messageBackground, messageBackgroundTint)

            binding.tvMessage.setAppearance(context, textAppearance)
            binding.tvMessage.setBackgroundResource(backgroundResourceId)
            binding.tvNickname.setAppearance(context, textAppearance)
            binding.tvNickname.setBackgroundResource(backgroundResourceId)
            binding.tvSentAt.setAppearance(context, textAppearance)
            binding.tvSentAt.setBackgroundResource(backgroundResourceId)


        } finally {
            a.recycle()
        }
    }

    fun drawMessage(message: BaseMessage) {
        binding.tvMessage.text = message.message + " TEST"
        binding.tvSentAt.text = message.createdAt.toString()
        binding.tvNickname.text = message.sender?.nickname
    }
}
