package com.sendbird.uikit.fragments;

import static android.app.Activity.RESULT_OK;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.sendbird.android.SendbirdChat;
import com.sendbird.android.channel.ChannelType;
import com.sendbird.android.channel.GroupChannel;
import com.sendbird.android.channel.Role;
import com.sendbird.android.exception.SendbirdException;
import com.sendbird.android.message.BaseMessage;
import com.sendbird.android.message.Emoji;
import com.sendbird.android.message.FileMessage;
import com.sendbird.android.message.MessageMetaArray;
import com.sendbird.android.message.Reaction;
import com.sendbird.android.message.SendingStatus;
import com.sendbird.android.message.UserMessage;
import com.sendbird.android.params.FileMessageCreateParams;
import com.sendbird.android.params.UserMessageCreateParams;
import com.sendbird.android.params.UserMessageUpdateParams;
import com.sendbird.android.user.Member;
import com.sendbird.android.user.MutedState;
import com.sendbird.android.user.Sender;
import com.sendbird.android.user.User;
import com.sendbird.uikit.R;
import com.sendbird.uikit.SendbirdUIKit;
import com.sendbird.uikit.activities.PhotoViewActivity;
import com.sendbird.uikit.activities.adapter.BaseMessageListAdapter;
import com.sendbird.uikit.activities.adapter.SuggestedMentionListAdapter;
import com.sendbird.uikit.activities.viewholder.MessageType;
import com.sendbird.uikit.activities.viewholder.MessageViewHolderFactory;
import com.sendbird.uikit.consts.ReplyType;
import com.sendbird.uikit.consts.StringSet;
import com.sendbird.uikit.interfaces.CustomParamsHandler;
import com.sendbird.uikit.interfaces.LoadingDialogHandler;
import com.sendbird.uikit.interfaces.OnItemClickListener;
import com.sendbird.uikit.interfaces.OnItemLongClickListener;
import com.sendbird.uikit.interfaces.OnResultHandler;
import com.sendbird.uikit.internal.tasks.JobResultTask;
import com.sendbird.uikit.internal.tasks.TaskQueue;
import com.sendbird.uikit.internal.ui.messages.VoiceMessageView;
import com.sendbird.uikit.internal.ui.reactions.EmojiListView;
import com.sendbird.uikit.internal.ui.reactions.EmojiReactionUserListView;
import com.sendbird.uikit.internal.ui.widgets.VoiceMessageInputView;
import com.sendbird.uikit.log.Logger;
import com.sendbird.uikit.model.DialogListItem;
import com.sendbird.uikit.model.EmojiManager;
import com.sendbird.uikit.model.FileInfo;
import com.sendbird.uikit.model.ReadyStatus;
import com.sendbird.uikit.model.VoiceMessageInfo;
import com.sendbird.uikit.modules.BaseMessageListModule;
import com.sendbird.uikit.modules.components.BaseMessageListComponent;
import com.sendbird.uikit.utils.ContextUtils;
import com.sendbird.uikit.utils.DialogUtils;
import com.sendbird.uikit.utils.FileUtils;
import com.sendbird.uikit.utils.IntentUtils;
import com.sendbird.uikit.utils.MessageUtils;
import com.sendbird.uikit.utils.PermissionUtils;
import com.sendbird.uikit.utils.SoftInputUtils;
import com.sendbird.uikit.vm.BaseMessageListViewModel;
import com.sendbird.uikit.vm.FileDownloader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract public class BaseMessageListFragment<
        LA extends BaseMessageListAdapter,
        LC extends BaseMessageListComponent<LA>,
        MT extends BaseMessageListModule<LC>,
        VM extends BaseMessageListViewModel> extends BaseModuleFragment<MT, VM> {
    @Nullable
    private OnItemClickListener<BaseMessage> messageClickListener;
    @Nullable
    private OnItemClickListener<BaseMessage> messageProfileClickListener;
    @Nullable
    private OnItemLongClickListener<BaseMessage> messageLongClickListener;
    @Nullable
    private OnItemLongClickListener<BaseMessage> messageProfileLongClickListener;
    @Nullable
    private LoadingDialogHandler loadingDialogHandler;
    @Nullable
    private LA adapter;
    @Nullable
    private SuggestedMentionListAdapter suggestedMentionListAdapter;

    @Nullable
    BaseMessage targetMessage;
    @Nullable
    private Uri mediaUri;

    private final ActivityResultLauncher<Intent> getContentLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        SendbirdChat.setAutoBackgroundDetection(true);
        final Intent intent = result.getData();
        int resultCode = result.getResultCode();

        if (resultCode != RESULT_OK || intent == null) return;
        final Uri mediaUri = intent.getData();
        if (mediaUri != null && isFragmentAlive()) {
            sendFileMessage(mediaUri);
        }
    });
    private final ActivityResultLauncher<Intent> takeCameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        SendbirdChat.setAutoBackgroundDetection(true);
        int resultCode = result.getResultCode();

        if (resultCode != RESULT_OK || getContext() == null) return;
        final Uri mediaUri = this.mediaUri;
        if (mediaUri != null && isFragmentAlive()) {
            sendFileMessage(mediaUri);
        }
    });
    private final ActivityResultLauncher<Intent> takeVideoLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        SendbirdChat.setAutoBackgroundDetection(true);
        int resultCode = result.getResultCode();

        if (resultCode != RESULT_OK) return;
        final Uri mediaUri = this.mediaUri;
        if (mediaUri != null && isFragmentAlive()) {
            sendFileMessage(mediaUri);
        }
    });

    @Override
    protected void onConfigureParams(@NonNull MT module, @NonNull Bundle args) {
        if (loadingDialogHandler != null) module.setOnLoadingDialogHandler(loadingDialogHandler);
    }

    @Override
    public void onDestroy() {
        Logger.i(">> BaseMessageListFragment::onDestroy()");
        super.onDestroy();
        SendbirdChat.setAutoBackgroundDetection(true);
    }

    @Override
    protected void onBeforeReady(@NonNull ReadyStatus status, @NonNull MT module, @NonNull VM viewModel) {
        Logger.d(">> BaseMessageListFragment::onBeforeReady()");
        module.getMessageListComponent().setPagedDataLoader(viewModel);
        if (this.adapter != null) {
            module.getMessageListComponent().setAdapter(adapter);
        }
        module.getMessageInputComponent().setSuggestedMentionListAdapter(suggestedMentionListAdapter == null ? new SuggestedMentionListAdapter() : suggestedMentionListAdapter);
    }

    /**
     * Make context menu items that are shown when the message is long clicked.
     *
     * @param message A clicked message.
     * @return Collection of {@link DialogListItem}
     * @since 2.2.3
     */
    @NonNull
    protected List<DialogListItem> makeMessageContextMenu(@NonNull BaseMessage message) {
        return new ArrayList<>();
    }

    void showMessageContextMenu(@NonNull View anchorView, @NonNull BaseMessage message, @NonNull List<DialogListItem> items) {}

    /**
     * It will be called when the message context menu was clicked.
     *
     * @param message  A clicked message.
     * @param view     The view that was clicked.
     * @param position The position that was clicked.
     * @param item     {@link DialogListItem} that was clicked.
     * @return <code>true</code> if long click event was handled, <code>false</code> otherwise.
     * @since 2.2.3
     */
    protected boolean onMessageContextMenuItemClicked(@NonNull BaseMessage message, @NonNull View view, int position, @NonNull DialogListItem item) {
        return false;
    }

    /**
     * Called when the item of the message list is clicked.
     *
     * @param view     The View clicked
     * @param position The position clicked
     * @param message  The message that the clicked item displays
     * @since 3.0.0
     */
    protected void onMessageClicked(@NonNull View view, int position, @NonNull BaseMessage message) {
        if (messageClickListener != null) {
            messageClickListener.onItemClick(view, position, message);
            return;
        }
        if (message.getSendingStatus() == SendingStatus.SUCCEEDED) {
            MessageType type = MessageViewHolderFactory.getMessageType(message);
            switch (type) {
                case VIEW_TYPE_FILE_MESSAGE_IMAGE_ME:
                case VIEW_TYPE_FILE_MESSAGE_IMAGE_OTHER:
                    startActivity(PhotoViewActivity.newIntent(requireContext(), ChannelType.GROUP, (FileMessage) message));
                    break;
                case VIEW_TYPE_FILE_MESSAGE_VIDEO_ME:
                case VIEW_TYPE_FILE_MESSAGE_VIDEO_OTHER:
                case VIEW_TYPE_FILE_MESSAGE_ME:
                case VIEW_TYPE_FILE_MESSAGE_OTHER:
                    final FileMessage fileMessage = (FileMessage) message;
                    FileDownloader.downloadFile(requireContext(), fileMessage, new OnResultHandler<File>() {
                        @Override
                        public void onResult(@NonNull File file) {
                            showFile(file, fileMessage.getType());
                        }

                        @Override
                        public void onError(@Nullable SendbirdException e) {
                            toastError(R.string.sb_text_error_download_file);
                        }
                    });
                    break;
                case VIEW_TYPE_VOICE_MESSAGE_ME:
                case VIEW_TYPE_VOICE_MESSAGE_OTHER:
                    if (view instanceof VoiceMessageView) {
                        ((VoiceMessageView) view).callOnPlayerButtonClick();
                    }
                    break;
                default:
            }
        } else {
            if (MessageUtils.isMine(message) && (message instanceof UserMessage || message instanceof FileMessage)) {
                resendMessage(message);
            }
        }
    }

    /**
     * Called when the profile view of the message is clicked.
     *
     * @param view     The View clicked
     * @param position The position clicked
     * @param message  The message that the clicked item displays
     * @since 3.0.0
     */
    protected void onMessageProfileClicked(@NonNull View view, int position, @NonNull BaseMessage message) {
        if (messageProfileClickListener != null) {
            messageProfileClickListener.onItemClick(view, position, message);
            return;
        }

        final Sender sender = message.getSender();
        if (sender != null) {
            showUserProfile(sender);
        }
    }

    /**
     * Called when the item of the message list is long-clicked.
     *
     * @param view     The View long-clicked
     * @param position The position long-clicked
     * @param message  The message that the long-clicked item displays
     * @since 3.0.0
     */
    protected void onMessageLongClicked(@NonNull View view, int position, @NonNull BaseMessage message) {
        if (messageLongClickListener != null) {
            messageLongClickListener.onItemLongClick(view, position, message);
            return;
        }
        final SendingStatus status = message.getSendingStatus();
        if (status == SendingStatus.PENDING) return;
        showMessageContextMenu(view, message, makeMessageContextMenu(message));
    }

    /**
     * Called when the profile view of the message is long-clicked.
     *
     * @param view     The View long-clicked
     * @param position The position long-clicked
     * @param message  The message that the long-clicked item displays
     * @since 3.0.0
     */
    protected void onMessageProfileLongClicked(@NonNull View view, int position, @NonNull BaseMessage message) {
        if (messageProfileLongClickListener != null) {
            messageProfileLongClickListener.onItemLongClick(view, position, message);
        }
    }

    @NonNull
    OnItemClickListener<DialogListItem> createMessageActionListener(@NonNull BaseMessage message) {
        return (view, position, item) -> onMessageContextMenuItemClicked(message, view, position, item);
    }

    private void download(@NonNull FileMessage fileMessage) {
        toastSuccess(R.string.sb_text_toast_success_start_download_file);
        TaskQueue.addTask(new JobResultTask<Boolean>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            @NonNull
            public Boolean call() throws Exception {
                if (getContext() == null) return false;
                FileDownloader.getInstance().saveFile(getContext(), fileMessage.getUrl(),
                        fileMessage.getType(), fileMessage.getName());
                return true;
            }

            @Override
            public void onResultForUiThread(@Nullable Boolean result, @Nullable SendbirdException e) {
                if (e != null) {
                    Logger.e(e);
                    toastError(R.string.sb_text_error_download_file);
                    return;
                }
                toastSuccess(R.string.sb_text_toast_success_download_file);
            }
        });
    }

    void copyTextToClipboard(@NonNull String text) {
        if (!isFragmentAlive()) return;
        ClipboardManager clipboardManager = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText(StringSet.LABEL_COPY_TEXT, text);
        if (clipboardManager != null) {
            clipboardManager.setPrimaryClip(clipData);
            toastSuccess(R.string.sb_text_toast_success_copy);
        } else {
            toastError(R.string.sb_text_error_copy_message);
        }
    }

    void showWarningDialog(@NonNull BaseMessage message) {
        if (getContext() == null) return;
        DialogUtils.showWarningDialog(
                requireContext(),
                getString(R.string.sb_text_dialog_delete_message),
                getString(R.string.sb_text_button_delete),
                delete -> {
                    Logger.dev("delete");
                    deleteMessage(message);
                },
                getString(R.string.sb_text_button_cancel),
                cancel -> Logger.dev("cancel"));
    }

    void showEmojiActionsDialog(@NonNull BaseMessage message, @NonNull DialogListItem[] actions) {
        if (getContext() == null) {
            return;
        }

        List<Emoji> emojiList = EmojiManager.getInstance().getAllEmojis();

        int shownEmojiSize = emojiList.size();
        boolean showMoreButton = false;
        if (emojiList.size() > 6) {
            showMoreButton = true;
            shownEmojiSize = 5;
        }
        emojiList = emojiList.subList(0, shownEmojiSize);

        final Context contextThemeWrapper = ContextUtils.extractModuleThemeContext(getContext(), getModule().getParams().getTheme(), R.attr.sb_component_list);
        final EmojiListView emojiListView = EmojiListView.create(contextThemeWrapper, emojiList, message.getReactions(), showMoreButton);
        hideKeyboard();
        if (actions.length > 0 || emojiList.size() > 0) {
            final AlertDialog dialog = DialogUtils.showContentViewAndListDialog(requireContext(), emojiListView, actions, createMessageActionListener(message));

            emojiListView.setEmojiClickListener((view, position, emojiKey) -> {
                dialog.dismiss();
                getViewModel().toggleReaction(view, message, emojiKey, e -> {
                    if (e != null)
                        toastError(view.isSelected() ? R.string.sb_text_error_delete_reaction : R.string.sb_text_error_add_reaction);
                });
            });

            emojiListView.setMoreButtonClickListener(v -> {
                dialog.dismiss();
                showEmojiListDialog(message);
            });
        }
    }

    private void showUserProfile(@NonNull User sender) {
        final Bundle args = getArguments();
        final boolean useUserProfile = args == null || args.getBoolean(StringSet.KEY_USE_USER_PROFILE, SendbirdUIKit.shouldUseDefaultUserProfile());
        if (getContext() == null || !useUserProfile) return;
        hideKeyboard();
        DialogUtils.showUserProfileDialog(getContext(), sender, true, null, null);
    }

    @NonNull
    private static Map<Reaction, List<User>> getReactionUserInfo(@NonNull GroupChannel channel, @NonNull List<Reaction> reactionList) {
        final Map<Reaction, List<User>> result = new HashMap<>();
        final Map<String, User> userMap = new HashMap<>();

        for (Member member : channel.getMembers()) {
            userMap.put(member.getUserId(), member);
        }

        for (Reaction reaction : reactionList) {
            final List<User> userList = new ArrayList<>();
            final List<String> userIds = reaction.getUserIds();
            for (String userId : userIds) {
                final User user = userMap.get(userId);
                userList.add(user);
            }
            result.put(reaction, userList);
        }

        return result;
    }

    private void hideKeyboard() {
        if (getView() != null) {
            SoftInputUtils.hideSoftKeyboard(getView());
        }
    }

    void toggleReaction(@NonNull View view, @NonNull BaseMessage message, @NonNull String reactionKey) {
        getViewModel().toggleReaction(view, message, reactionKey, e -> {
            if (e != null && isFragmentAlive()) {
                toastError(view.isSelected() ? R.string.sb_text_error_delete_reaction : R.string.sb_text_error_add_reaction);
            }
        });
    }

    void showEmojiReactionDialog(@NonNull BaseMessage message, int position) {
        if (getContext() == null) {
            return;
        }

        final Context contextThemeWrapper = ContextUtils.extractModuleThemeContext(getContext(), getModule().getParams().getTheme(), R.attr.sb_component_list);
        final EmojiReactionUserListView emojiReactionUserListView = new EmojiReactionUserListView(contextThemeWrapper);
        final GroupChannel channel = getViewModel().getChannel();
        if (channel != null) {
            emojiReactionUserListView.setEmojiReactionUserData(this,
                    position,
                    message.getReactions(),
                    getReactionUserInfo(channel, message.getReactions()));
        }
        hideKeyboard();
        DialogUtils.showContentDialog(requireContext(), emojiReactionUserListView);
    }

    void showEmojiListDialog(@NonNull BaseMessage message) {
        if (getContext() == null) {
            return;
        }

        final Context contextThemeWrapper = ContextUtils.extractModuleThemeContext(getContext(), getModule().getParams().getTheme(), R.attr.sb_component_list);
        final EmojiListView emojiListView = EmojiListView.create(contextThemeWrapper, EmojiManager.getInstance().getAllEmojis(), message.getReactions(), false);
        hideKeyboard();
        final AlertDialog dialog = DialogUtils.showContentDialog(requireContext(), emojiListView);

        emojiListView.setEmojiClickListener((view, position, emojiKey) -> {
            dialog.dismiss();
            getViewModel().toggleReaction(view, message, emojiKey, e -> {
                if (e != null)
                    toastError(view.isSelected() ? R.string.sb_text_error_delete_reaction : R.string.sb_text_error_add_reaction);
            });
        });
    }

    void sendFileMessageInternal(@NonNull FileInfo fileInfo, @NonNull FileMessageCreateParams params) {
        if (targetMessage != null && SendbirdUIKit.getReplyType() != ReplyType.NONE) {
            params.setParentMessageId(targetMessage.getMessageId());
            params.setReplyToChannel(true);
        }
        getViewModel().sendFileMessage(params, fileInfo);
    }

    private void showFile(@NonNull File file, @NonNull String mimeType) {
        TaskQueue.addTask(new JobResultTask<Intent>() {
            @Override
            @Nullable
            public Intent call() {
                if (!isFragmentAlive()) return null;
                Uri uri = FileUtils.fileToUri(requireContext(), file);
                return IntentUtils.getFileViewerIntent(uri, mimeType);
            }

            @Override
            public void onResultForUiThread(@Nullable Intent intent, @Nullable SendbirdException e) {
                if (!isFragmentAlive()) return;
                if (e != null) {
                    Logger.e(e);
                    toastError(R.string.sb_text_error_open_file);
                    return;
                }
                if (intent != null) {
                    startActivity(intent);
                }
            }
        });
    }

    /**
     * It will be called when the input message's left button is clicked.
     * The default behavior is showing the menu, like, taking camera, gallery, and file.
     *
     * @since 2.0.1
     */
    protected void showMediaSelectDialog() {
        if (getContext() == null) return;
        DialogListItem[] items = {
                new DialogListItem(R.string.sb_text_channel_input_camera, R.drawable.icon_camera),
                new DialogListItem(R.string.sb_text_channel_input_take_video, R.drawable.icon_camera),
                new DialogListItem(R.string.sb_text_channel_input_gallery, R.drawable.icon_photo),
                new DialogListItem(R.string.sb_text_channel_input_document, R.drawable.icon_document)
        };
        hideKeyboard();
        DialogUtils.showListBottomDialog(requireContext(), items, (view, position, item) -> {
            final int key = item.getKey();
            try {
                if (key == R.string.sb_text_channel_input_camera) {
                    takeCamera();
                } else if (key == R.string.sb_text_channel_input_take_video) {
                    takeVideo();
                } else if (key == R.string.sb_text_channel_input_gallery) {
                    takePhoto();
                } else {
                    takeFile();
                }
            } catch (Exception e) {
                Logger.e(e);
                if (key == R.string.sb_text_channel_input_camera) {
                    toastError(R.string.sb_text_error_open_camera);
                } else if (key == R.string.sb_text_channel_input_take_video) {
                    toastError(R.string.sb_text_error_open_camera);
                } else if (key == R.string.sb_text_channel_input_gallery) {
                    toastError(R.string.sb_text_error_open_gallery);
                } else {
                    toastError(R.string.sb_text_error_open_file);
                }
            }
        });
    }

    /**
     * Call taking camera application.
     *
     * @since 2.0.1
     */
    public void takeCamera() {
        SendbirdChat.setAutoBackgroundDetection(false);
        requestPermission(PermissionUtils.CAMERA_PERMISSION, () -> {
            if (getContext() == null) return;
            this.mediaUri = FileUtils.createImageFileUri(getContext());
            if (mediaUri == null) return;
            Intent intent = IntentUtils.getCameraIntent(requireContext(), mediaUri);
            if (IntentUtils.hasIntent(requireContext(), intent)) {
                takeCameraLauncher.launch(intent);
            }
        });
    }

    /**
     * Call taking camera application for video capture.
     *
     * @since 3.2.1
     */
    public void takeVideo() {
        SendbirdChat.setAutoBackgroundDetection(false);
        requestPermission(PermissionUtils.CAMERA_PERMISSION, () -> {
            if (getContext() == null) return;
            this.mediaUri = FileUtils.createVideoFileUri(getContext());
            if (mediaUri == null) return;

            Intent intent = IntentUtils.getVideoCaptureIntent(getContext(), mediaUri);
            if (IntentUtils.hasIntent(getContext(), intent)) {
                takeVideoLauncher.launch(intent);
            }
        });
    }

    /**
     * Call taking gallery application.
     *
     * @since 2.0.1
     */
    public void takePhoto() {
        SendbirdChat.setAutoBackgroundDetection(false);
        Logger.d("++ build sdk int=%s", Build.VERSION.SDK_INT);
        final String[] permissions = PermissionUtils.GET_CONTENT_PERMISSION;
        if (permissions.length > 0) {
            requestPermission(permissions, () -> {
                Intent intent = IntentUtils.getGalleryIntent();
                getContentLauncher.launch(intent);
            });
        } else {
            Intent intent = IntentUtils.getGalleryIntent();
            getContentLauncher.launch(intent);
        }
    }

    /**
     * Call taking file chooser application.
     *
     * @since 2.0.1
     */
    public void takeFile() {
        SendbirdChat.setAutoBackgroundDetection(false);
        final String[] permissions = PermissionUtils.GET_CONTENT_PERMISSION;
        if (permissions.length > 0) {
            requestPermission(permissions, () -> {
                Intent intent = IntentUtils.getFileChooserIntent();
                getContentLauncher.launch(intent);
            });
        } else {
            Intent intent = IntentUtils.getFileChooserIntent();
            getContentLauncher.launch(intent);
        }
    }

    /**
     * Call taking voice recorder.
     *
     * @since 3.4.0
     */
    public void takeVoiceRecorder() {
        requestPermission(PermissionUtils.RECORD_AUDIO_PERMISSION, () -> {
            if (getContext() == null) return;
            final Context contextThemeWrapper = ContextUtils.extractModuleThemeContext(getContext(), getModule().getParams().getTheme(), R.attr.sb_component_channel_message_input);
            final VoiceMessageInputView recorderView = new VoiceMessageInputView(contextThemeWrapper);
            hideKeyboard();
            final AlertDialog dialog = DialogUtils.showContentDialog(contextThemeWrapper, recorderView);
            dialog.setCanceledOnTouchOutside(false);
            recorderView.setOnSendButtonClickListener((sendButton, position, voiceMessageInfo) -> {
                sendVoiceFileMessage(voiceMessageInfo);
                dialog.dismiss();
            });
            recorderView.setOnCancelButtonClickListener(cancelButton -> {
                dialog.dismiss();
            });
        });
    }

    /**
     * It will be called when the loading dialog needs displaying.
     *
     * @return True if the callback has consumed the event, false otherwise.
     * @since 1.2.5
     */
    protected boolean shouldShowLoadingDialog() {
        return getModule().shouldShowLoadingDialog();
    }

    /**
     * It will be called when the loading dialog needs dismissing.
     *
     * @since 1.2.5
     */
    protected void shouldDismissLoadingDialog() {
        getModule().shouldDismissLoadingDialog();
    }

    /**
     * It will be called before sending message.
     * If you want add more data, you can override this and set the data.
     *
     * @param params Params of user message. Refer to {@link UserMessageCreateParams}.
     * @since 1.0.4
     */
    protected void onBeforeSendUserMessage(@NonNull UserMessageCreateParams params) {
    }

    /**
     * It will be called before sending message.
     * If you want add more data, you can override this and set the data.
     *
     * @param params Params of file message. Refer to {@link FileMessageCreateParams}.
     * @since 1.0.4
     */
    protected void onBeforeSendFileMessage(@NonNull FileMessageCreateParams params) {
    }

    /**
     * It will be called before updating message.
     * If you want add more data, you can override this and set the data.
     *
     * @param params Params of user message. Refer to {@link UserMessageUpdateParams}.
     * @since 1.0.4
     */
    protected void onBeforeUpdateUserMessage(@NonNull UserMessageUpdateParams params) {
    }

    /**
     * Sends a user message.
     *
     * @param params Params of user message. Refer to {@link UserMessageCreateParams}.
     * @since 1.0.4
     */
    protected void sendUserMessage(@NonNull UserMessageCreateParams params) {
        final CustomParamsHandler customHandler = SendbirdUIKit.getCustomParamsHandler();
        if (customHandler != null) {
            customHandler.onBeforeSendUserMessage(params);
        }
        onBeforeSendUserMessage(params);
        getViewModel().sendUserMessage(params);
    }

    /**
     * Sends a file with given file information.
     *
     * @param uri A file Uri
     * @since 1.0.4
     */
    protected void sendFileMessage(@NonNull Uri uri) {
        if (getContext() != null) {
            FileInfo.fromUri(getContext(), uri, SendbirdUIKit.shouldUseImageCompression(), new OnResultHandler<FileInfo>() {
                @Override
                public void onResult(@NonNull FileInfo info) {
                    BaseMessageListFragment.this.mediaUri = null;
                    sendFileMessage(info, info.toFileParams());
                }

                @Override
                public void onError(@Nullable SendbirdException e) {
                    Logger.w(e);
                    toastError(R.string.sb_text_error_send_message);
                    BaseMessageListFragment.this.mediaUri = null;
                }
            });
        }
    }

    /**
     * Sends a voice message with given file information.
     *
     * @param info A voice file information
     * @since 3.4.0
     */
    protected void sendVoiceFileMessage(@NonNull VoiceMessageInfo info) {
        final GroupChannel channel = getViewModel().getChannel();
        if (channel == null) return;
        boolean isOperator = channel.getMyRole() == Role.OPERATOR;
        boolean isMuted = channel.getMyMutedState() == MutedState.MUTED;
        boolean isFrozen = channel.isFrozen() && !isOperator;
        if (isMuted || isFrozen) {
            if (isMuted) {
                toastError(R.string.sb_text_error_user_muted);
            } else {
                toastError(R.string.sb_text_error_channel_frozen);
            }
            final File voiceFile = new File(info.getPath());
            voiceFile.delete();
            return;
        }

        if (getContext() != null) {
            final FileInfo fileInfo = FileInfo.fromVoiceFileInfo(info,
                    FileUtils.getChannelFileCacheDir(getContext(), getViewModel().getChannelUrl()));
            final FileMessageCreateParams params = fileInfo.toFileParams();
            final List<MessageMetaArray> metaArrays =  new ArrayList<>();
            final List<String> duration = new ArrayList<>();
            duration.add(String.valueOf(info.getDuration()));
            metaArrays.add(new MessageMetaArray(StringSet.KEY_VOICE_MESSAGE_DURATION, duration));
            final List<String> type = new ArrayList<>();
            type.add(StringSet.voice + "/" + StringSet.m4a);
            metaArrays.add(new MessageMetaArray(StringSet.KEY_INTERNAL_MESSAGE_TYPE, type));
            params.setMetaArrays(metaArrays);
            params.setFileName(StringSet.Voice_message + "." + StringSet.m4a);
            sendFileMessage(fileInfo, params);
        }
    }

    private void sendFileMessage(@NonNull FileInfo info, @NonNull FileMessageCreateParams params) {
        final CustomParamsHandler customHandler = SendbirdUIKit.getCustomParamsHandler();
        if (customHandler != null) {
            customHandler.onBeforeSendFileMessage(params);
        }
        onBeforeSendFileMessage(params);
        sendFileMessageInternal(info, params);
    }

    /**
     * Updates a <code>UserMessage</code> that was previously sent in the channel.
     *
     * @param messageId The ID of the message. This must be a message that exists in the channel's history,
     *                  or an error will be returned.
     * @param params    Params of a message. Refer to {@link UserMessageUpdateParams}.
     * @since 1.0.4
     */
    protected void updateUserMessage(long messageId, @NonNull UserMessageUpdateParams params) {
        CustomParamsHandler customHandler = SendbirdUIKit.getCustomParamsHandler();
        if (customHandler != null) {
            customHandler.onBeforeUpdateUserMessage(params);
        }
        onBeforeUpdateUserMessage(params);
        getViewModel().updateUserMessage(messageId, params, e -> {
            if (e != null) toastError(R.string.sb_text_error_update_user_message);
        });
    }

    /**
     * Delete a message
     *
     * @param message Message to delete.
     * @since 1.0.4
     */
    protected void deleteMessage(@NonNull BaseMessage message) {
        getViewModel().deleteMessage(message, e -> {
            if (e != null) toastError(R.string.sb_text_error_delete_message);
        });
    }

    /**
     * Resends a failed message.
     *
     * @param message Failed message to resend.
     */
    protected void resendMessage(@NonNull BaseMessage message) {
        if (message.isResendable()) {
            getViewModel().resendMessage(message, e -> {
                if (e != null) toastError(R.string.sb_text_error_resend_message);
            });
        } else {
            toastError(R.string.sb_text_error_not_possible_resend_message);
        }
    }

    /**
     * Download {@link FileMessage} into external storage.
     * It needs to have a permission.
     * If current application needs permission, the request of permission will call automatically.
     * After permission is granted, the download will be also called automatically.
     *
     * @param message A file message to download contents.
     * @since 2.2.3
     */
    protected void saveFileMessage(@NonNull FileMessage message) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
            download(message);
        } else {
            requestPermission(PermissionUtils.GET_CONTENT_PERMISSION, () -> download(message));
        }
    }

    /**
     * Returns the URL of the channel with the required data to use this fragment.
     *
     * @return The URL of a channel this fragment is currently associated with
     * @since 3.0.0
     */
    @NonNull
    protected String getChannelUrl() {
        final Bundle args = getArguments() == null ? new Bundle() : getArguments();
        return args.getString(StringSet.KEY_CHANNEL_URL, "");
    }

    /**
     * Sets the click listener on the item of message list.
     *
     * @param messageClickListener The callback that will run.
     * @since 3.3.0
     */
    void setOnMessageClickListener(@Nullable OnItemClickListener<BaseMessage> messageClickListener) {
        this.messageClickListener = messageClickListener;
    }

    /**
     * Sets the click listener on the profile of message.
     *
     * @param messageProfileClickListener The callback that will run.
     * @since 3.3.0
     */
    void setOnMessageProfileClickListener(@Nullable OnItemClickListener<BaseMessage> messageProfileClickListener) {
        this.messageProfileClickListener = messageProfileClickListener;
    }

    /**
     * Sets the long click listener on the item of message list.
     *
     * @param messageLongClickListener The callback that will run.
     * @since 3.3.0
     */
    void setOnMessageLongClickListener(@Nullable OnItemLongClickListener<BaseMessage> messageLongClickListener) {
        this.messageLongClickListener = messageLongClickListener;
    }

    /**
     * Sets the long click listener on the item of message list.
     *
     * @param messageProfileLongClickListener The callback that will run.
     * @since 3.3.0
     */
    void setOnMessageProfileLongClickListener(@Nullable OnItemLongClickListener<BaseMessage> messageProfileLongClickListener) {
        this.messageProfileLongClickListener = messageProfileLongClickListener;
    }

    /**
     * Sets the custom loading dialog handler
     *
     * @param loadingDialogHandler Interface definition for a callback to be invoked before when the loading dialog is called.
     * @since 3.3.0
     */
    void setOnLoadingDialogHandler(@Nullable LoadingDialogHandler loadingDialogHandler) {
        this.loadingDialogHandler = loadingDialogHandler;
    }

    /**
     * Sets the message list adapter.
     *
     * @param adapter the adapter for the message list.
     * @since 3.3.0
     */
    void setAdapter(@Nullable LA adapter) {
        this.adapter = adapter;
    }

    /**
     * Sets the suggested mention list adapter.
     *
     * @param suggestedMentionListAdapter the adapter for the mentionable user list.
     * @since 3.3.0
     */
    void setSuggestedMentionListAdapter(@Nullable SuggestedMentionListAdapter suggestedMentionListAdapter) {
        this.suggestedMentionListAdapter = suggestedMentionListAdapter;
    }
}
