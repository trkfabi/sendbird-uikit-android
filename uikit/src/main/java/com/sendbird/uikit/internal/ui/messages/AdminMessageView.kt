package com.sendbird.uikit.internal.ui.messages

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import com.sendbird.android.message.BaseMessage
import com.sendbird.uikit.R
import com.sendbird.uikit.databinding.SbViewAdminMessageComponentBinding
import com.sendbird.uikit.internal.extensions.setAppearance
import com.sendbird.uikit.utils.DateUtils
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
                android.R.color.white
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

            val nicknameAppearance = a.getResourceId(
                R.styleable.MessageView_sb_message_sender_name_text_appearance,
                R.style.SendbirdCaption1OnLight02
            )
            val sentAtAppearance = a.getResourceId(
                R.styleable.ParentMessageInfoView_sb_parent_message_info_sent_at_appearance,
                R.style.SendbirdCaption2OnLight03
            )
            binding.tvMessage.background =
                DrawableUtils.setTintList(context, messageBackground, messageBackgroundTint)

            binding.tvMessage.setAppearance(context, textAppearance)
            binding.tvNickname.setAppearance(context, nicknameAppearance)
            binding.tvSentAt.setAppearance(context, sentAtAppearance)

        } finally {
            a.recycle()
        }
    }

    fun drawMessage(message: BaseMessage) {
        binding.tvMessage.text = message.message
        binding.tvSentAt.text = DateUtils.formatDateTime(context, message.createdAt)
        binding.tvNickname.text = "Admin User"
        ViewUtils.drawProfile(
            binding.ivProfileView,
            "https://w7.pngwing.com/pngs/340/946/png-transparent-avatar-user-computer-icons-software-developer-avatar-child-face-heroes-thumbnail.png",
                    "https://w7.pngwing.com/pngs/340/946/png-transparent-avatar-user-computer-icons-software-developer-avatar-child-face-heroes-thumbnail.png"
        )
    }
}
