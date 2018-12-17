package com.example.exoplayerdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.session.MediaButtonReceiver;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

public class AudioBackService extends MediaBrowserServiceCompat {
    private static MediaSessionCompat mMediaSession;

    public static final String ACTION_START_FOREGROUND_SERVICE = "ACTION_START_FOREGROUND_SERVICE";

    public static final String ACTION_STOP_FOREGROUND_SERVICE = "ACTION_STOP_FOREGROUND_SERVICE";

    private static final String CHANNEL_ID = "media_playback_channel";

    // Binder given to clients
    private final IBinder binder = new LocalBinder();
    // Registered callbacks
    private ServiceCallbacks serviceCallbacks;

    private PlaybackStateCompat.Builder mStateBuilder;

    private MediaSessionCompat.Token token;


    // Class used for the client Binder.
    public class LocalBinder extends Binder {
        AudioBackService getService() {
            // Return this instance of MyService so clients can call public methods
            return AudioBackService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setCallbacks(ServiceCallbacks callbacks) {
        serviceCallbacks = callbacks;
    }
    public AudioBackService(){

    }

public void initMediaSession(){
    mMediaSession = new MediaSessionCompat(this, this.getClass().getSimpleName());
    mMediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

    // Do not let MediaButtons restart the player when the app is not visible.
    mMediaSession.setMediaButtonReceiver(null);

    mStateBuilder = new PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY |
                    PlaybackStateCompat.ACTION_PAUSE |
                    PlaybackStateCompat.ACTION_FAST_FORWARD |
                    PlaybackStateCompat.ACTION_REWIND |
                    PlaybackStateCompat.ACTION_PLAY_PAUSE |
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS);

    mMediaSession.setPlaybackState(mStateBuilder.build());
    mMediaSession.setCallback(new MySessionCallback());
    mMediaSession.setActive(true);
}


    private class MySessionCallback extends MediaSessionCompat.Callback {
        @Override
        public void onPlay() {
            serviceCallbacks.play();
        }

        @Override
        public void onPause() {
            serviceCallbacks.pause();
        }

        @Override
        public void onRewind() {
            serviceCallbacks.rewind();
        }

        @Override
        public void onFastForward() {
            serviceCallbacks.fast();
        }

        @Override
        public void onSkipToPrevious() {
            serviceCallbacks.seekTo();
        }
    }
    @Override
    public void onCreate() {
        super.onCreate();
        initMediaSession();

    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName, int clientUid, @Nullable Bundle rootHints) {
        if(TextUtils.equals(clientPackageName, getPackageName())) {
            return new BrowserRoot(getString(R.string.app_name), null);
        }

        return null;
    }

    @Override
    public void onLoadChildren(@NonNull String parentId, @NonNull Result<List<MediaBrowserCompat.MediaItem>> result) {
        result.sendResult(null);
    }


    /**
     * Broadcast Receiver registered to receive the MEDIA_BUTTON intent coming from clients
     */

    public static class MediaReceiver extends BroadcastReceiver {

        public MediaReceiver() {
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            MediaButtonReceiver.handleIntent(mMediaSession, intent);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null)
        {
            String action = intent.getAction();

            switch (action)
            {
                case ACTION_START_FOREGROUND_SERVICE:
                    mStateBuilder.setState(intent.getIntExtra("playback",PlaybackStateCompat.STATE_PLAYING),
                            intent.getLongExtra("cur_pos",0), 1f);
                    mMediaSession.setPlaybackState(mStateBuilder.build());
                    showNotification(mStateBuilder.build());
                    break;
                case ACTION_STOP_FOREGROUND_SERVICE:
                    mMediaSession.setActive(false);
                     break;

            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
    private void showNotification(PlaybackStateCompat state) {

        // You only need to create the channel on API 26+ devices
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel();
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);

        int icon;
        String play_pause;
        if (state.getState() == PlaybackStateCompat.STATE_PLAYING) {
            icon = R.drawable.exo_controls_pause;
            play_pause = "pause";
        } else {
            icon = R.drawable.exo_controls_play;
            play_pause = "play";
        }

       NotificationCompat.Action playPauseAction = new NotificationCompat.Action(
                icon, play_pause,
                MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                        PlaybackStateCompat.ACTION_PLAY_PAUSE));

        NotificationCompat.Action restartAction = new NotificationCompat.Action(
                R.drawable.ic_replay, "restart",
                MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS));

        NotificationCompat.Action fastForwardAction = new NotificationCompat.Action(
                R.drawable.exo_icon_fastforward, "fastforward",
                MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                        PlaybackStateCompat.ACTION_FAST_FORWARD));

        NotificationCompat.Action rewindAction = new NotificationCompat.Action(
                R.drawable.exo_icon_rewind, "rewind",
                MediaButtonReceiver.buildMediaButtonPendingIntent(getApplicationContext(),
                        PlaybackStateCompat.ACTION_REWIND));

        PendingIntent contentPendingIntent = PendingIntent.getActivity
                (this, 0, new Intent(this, MainActivity.class), 0);

        token = mMediaSession.getSessionToken();

        builder.setContentTitle("Audio of Western Meadowlark")
                .setContentText("Courtesy of Cornell Lab of Ornithology 0:55")
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.ic_notification_large)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.splash_logo))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .addAction(restartAction)
                .addAction(rewindAction)
                .addAction(playPauseAction)
                .addAction(fastForwardAction)
                .setStyle(new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(token)
                        .setShowActionsInCompactView(0, 1));

         startForeground(1, builder.build());
    }

    /**
     * The NotificationCompat class does not create a channel for you. You still have to create a channel yourself
     */

    @RequiresApi(Build.VERSION_CODES.O)
    private void createChannel() {
        NotificationManager mNotificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
        // The id of the channel.
        String id = CHANNEL_ID;
        // The user-visible name of the channel.
        CharSequence name = "Media playback";
        // The user-visible description of the channel.
        String description = "Media playback controls";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel mChannel = new NotificationChannel(id, name, importance);
        // Configure the notification channel.
        mChannel.setDescription(description);
        mChannel.setShowBadge(false);
        mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        mNotificationManager.createNotificationChannel(mChannel);
    }
    public void stopForegroundService()
    {
        Log.d("", "Stop foreground service.");

        // Stop foreground service and remove the notification.
        stopForeground(true);

        // Stop the foreground service.
        stopSelf();
    }

    public void updateNotification(int state,long current_pos){
        mStateBuilder.setState(state,
               current_pos, 1f);

        mMediaSession.setPlaybackState(mStateBuilder.build());
        showNotification(mStateBuilder.build());
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        super.onTaskRemoved(rootIntent);
        Log.i("---","=======> onTaskRemoved called");
        mMediaSession.setActive(false);
        stopForegroundService();
    }
}
