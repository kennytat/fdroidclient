package org.fdroid.fdroid.views;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;

import org.fdroid.fdroid.R;
import org.fdroid.fdroid.UpdateService;
import org.fdroid.fdroid.net.ConnectivityMonitorService;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

/**
 * Banner widget which reflects current status related to repository updates.
 * It will display whether repositories area actively being updated, or
 * whether there is no Internet connection, so repositories cannot be updated
 * from the Internet.
 * <p>
 * It shows a "No Internet" message when it identifies the device is not
 * connected. Will only monitor the wifi state when attached to the window.
 * Note that this does a pretty poor job of responding to network changes in
 * real time. It only knows how to respond to the <em>enabling</em> of WiFi
 * (not disabling of WiFi, nor enabling/disabling of mobile data). However it
 * will always query the network state when it is shown to the user. This way
 * if they change between tabs, hide and then open F-Droid, or do other things
 * which require the view to attach to the window again then it will update the
 * network state. In practice this works pretty well.
 *
 * @see <a href="https://gitlab.com/fdroid/fdroidclient/-/merge_requests/724">"No internet" banner on main, categories, and updates screen</a>
 */
public class BannerUpdatingRepos extends androidx.appcompat.widget.AppCompatTextView {

    private int updateServiceStatus = UpdateService.STATUS_COMPLETE_WITH_CHANGES;
    private int networkState = ConnectivityMonitorService.FLAG_NET_NO_LIMIT;

    public BannerUpdatingRepos(Context context) {
        this(context, null);
    }

    public BannerUpdatingRepos(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public BannerUpdatingRepos(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        int padding = (int) getResources().getDimension(R.dimen.banner__padding);
        setPadding(padding, padding, padding, padding);
        setBackgroundColor(0xFF4A4A4A);
        setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL);
        setTextColor(0xFFFFFFFF);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Context context = getContext();
        networkState = ConnectivityMonitorService.getNetworkState(context);
        context.registerReceiver(onNetworkStateChanged,
                new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        if (UpdateService.isUpdating()) {
            updateServiceStatus = UpdateService.STATUS_INFO;
        }
        LocalBroadcastManager.getInstance(context).registerReceiver(onRepoFeedback,
                new IntentFilter(UpdateService.LOCAL_ACTION_STATUS));

        setBannerTextAndVisibility();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        Context context = getContext();
        LocalBroadcastManager.getInstance(context).unregisterReceiver(onRepoFeedback);
        context.unregisterReceiver(onNetworkStateChanged);
    }

    private void setBannerTextAndVisibility() {
        if (updateServiceStatus == UpdateService.STATUS_INFO) {
            setText(R.string.banner_updating_repositories);
            setVisibility(View.VISIBLE);
        } else if (networkState == ConnectivityMonitorService.FLAG_NET_UNAVAILABLE
                || networkState == ConnectivityMonitorService.FLAG_NET_DEVICE_AP_WITHOUT_INTERNET) {
            setText(R.string.banner_no_internet);
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    /**
     * Anything other than a {@link UpdateService#STATUS_INFO} broadcast
     * signifies that it was complete (and out banner should be removed).
     */
    private final BroadcastReceiver onRepoFeedback = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateServiceStatus = intent.getIntExtra(UpdateService.EXTRA_STATUS_CODE,
                    UpdateService.STATUS_COMPLETE_WITH_CHANGES);
            setBannerTextAndVisibility();
        }
    };

    private final BroadcastReceiver onNetworkStateChanged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            networkState = ConnectivityMonitorService.getNetworkState(context);
            setBannerTextAndVisibility();
        }
    };
}
