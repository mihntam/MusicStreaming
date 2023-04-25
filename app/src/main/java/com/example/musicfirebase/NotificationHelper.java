package com.example.musicfirebase;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.ContextWrapper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.os.Build;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.media.session.MediaButtonReceiver;

import com.example.musicfirebase.R;

public class NotificationHelper extends ContextWrapper {

    private static final String CHANNEL_ID = "my_channel";
    private static final String CHANNEL_NAME = "My Channel";

    private NotificationManager manager;
    private MediaSession mediaSession;

    public NotificationHelper(Context context) {
        super(context);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        getManager().createNotificationChannel(channel);
    }

    public Notification getNotification(MediaController mediaController, MediaMetadata metadata, PlaybackState state) {
        if (mediaSession == null) {
            mediaSession = new MediaSession(this, "My Media Session");
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.svg_btn_play)
                .setContentTitle(metadata.getString(MediaMetadata.METADATA_KEY_TITLE))
                .setContentText(metadata.getString(MediaMetadata.METADATA_KEY_ARTIST))
                .setLargeIcon(getBitmap(metadata))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setMediaSession(MediaSessionCompat.Token.fromToken(mediaSession.getSessionToken()))
                        .setShowActionsInCompactView(0, 1, 2)
                        .setShowCancelButton(true)
                        .setCancelButtonIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                )
                .setDeleteIntent(MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_STOP))
                .addAction(new NotificationCompat.Action(
                        R.drawable.svg_btn_prev,
                        "Previous",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                ))
                .addAction(new NotificationCompat.Action(
                        state.getState() == PlaybackState.STATE_PLAYING ? R.drawable.svg_btn_pause : R.drawable.svg_btn_play,
                        state.getState() == PlaybackState.STATE_PLAYING ? "Pause" : "Play",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, state.getState() == PlaybackState.STATE_PLAYING ? PlaybackStateCompat.ACTION_PAUSE : PlaybackStateCompat.ACTION_PLAY)
                ))
                .addAction(new NotificationCompat.Action(
                        R.drawable.svg_btn_next,
                        "Next",
                        MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                ));

        return builder.build();
    }

    private NotificationManager getManager() {
        if (manager == null) {
            manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        }
        return manager;
    }

    private Bitmap getBitmap(MediaMetadata metadata) {
        byte[] bytes = metadata.getBitmap(MediaMetadata.METADATA_KEY_ART).getNinePatchChunk();
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
