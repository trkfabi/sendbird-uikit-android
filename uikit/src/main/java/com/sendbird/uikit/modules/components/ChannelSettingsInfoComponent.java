package com.sendbird.uikit.modules.components;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.sendbird.android.channel.GroupChannel;
import com.sendbird.uikit.R;
import com.sendbird.uikit.internal.ui.components.ChannelSettingsInfoView;

/**
 * This class creates and performs a view corresponding the channel settings information area in Sendbird UIKit.
 *
 * @since 3.0.0
 */
public class ChannelSettingsInfoComponent {
    @NonNull
    private final Params params;
    @Nullable
    private ChannelSettingsInfoView infoView;

    /**
     * Constructor
     *
     * @since 3.0.0
     */
    public ChannelSettingsInfoComponent() {
        this.params = new Params();
    }

    /**
     * Returns the view created by {@link #onCreateView(Context, LayoutInflater, ViewGroup, Bundle)}.
     *
     * @return the topmost view containing this view
     * @since 3.0.0
     */
    @Nullable
    public View getRootView() {
        return this.infoView;
    }

    /**
     * Returns a collection of parameters applied to this component.
     *
     * @return {@code Params} applied to this component
     * @since 3.0.0
     */
    @NonNull
    public Params getParams() {
        return params;
    }

    /**
     * Called after the component was created to make views.
     * <p><b>If this function is used override, {@link #getRootView()} must also be override.</b></p>
     *
     * @param context  The {@code Context} this component is currently associated with
     * @param inflater The LayoutInflater object that can be used to inflate any views in the component
     * @param parent   The ViewGroup into which the new View will be added
     * @param args     The arguments supplied when the component was instantiated, if any
     * @return Return the View for the UI.
     * @since 3.0.0
     */
    @NonNull
    public View onCreateView(@NonNull Context context, @NonNull LayoutInflater inflater, @NonNull ViewGroup parent, @Nullable Bundle args) {
        if (args != null) params.apply(context, args);
        final ChannelSettingsInfoView infoView = new ChannelSettingsInfoView(context, null, R.attr.sb_component_channel_settings_info);
        this.infoView = infoView;
        return infoView;
    }

    /**
     * Notifies this component that the channel data has changed.
     *
     * @param channel The latest group channel
     * @since 3.0.0
     */
    public void notifyChannelChanged(@NonNull GroupChannel channel) {
        if (this.infoView == null) return;
        this.infoView.drawChannelSettingsInfoView(channel);
    }

    /**
     * A collection of parameters, which can be applied to a default View. The values of params are not dynamically applied at runtime.
     * Params cannot be created directly, and it is automatically created together when components are created.
     * <p>Since the onCreateView configuring View uses the values of the set Params, we recommend that you set up for Params before the onCreateView is called.</p>
     *
     * @see #getParams()
     * @since 3.0.0
     */
    public static class Params {
        protected Params() {
        }

        /**
         * Apply data that matches keys mapped to Params' properties.
         *
         * @param context The {@code Context} this component is currently associated with
         * @param args    The sets of arguments to apply at Params.
         * @return This Params object that applied with given data.
         * @since 3.0.0
         */
        @NonNull
        protected Params apply(@NonNull Context context, @NonNull Bundle args) {
            return this;
        }
    }
}
