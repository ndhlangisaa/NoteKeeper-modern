package com.ndhlangisa.pluralsight.notekeeper;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Builds and shows the "Review note" notification.
 */
public class NoteReminderNotification {
    public static final String CHANNEL_ID = "note_reminders";
    private static final String NOTIFICATION_TAG = "NoteReminder";

    /**
     * Since Android 8 every notification must belong to a channel. The user controls sound,
     * vibration and importance per channel in system Settings. Creating it again is harmless.
     */
    public static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription(context.getString(R.string.reminder_channel_description));

        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public static void notify(Context context, String noteTitle, String noteText, int noteId) {
        // Android 13+: if the user hasn't allowed notifications, quietly do nothing
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        createChannel(context);

        // Tapping the notification opens this note
        Intent noteActivityIntent = new Intent(context, NoteActivity.class);
        noteActivityIntent.putExtra(NoteActivity.NOTE_ID, noteId);
        PendingIntent openNoteIntent = PendingIntent.getActivity(context, noteId, noteActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // Action button 1: open the list of notes
        PendingIntent viewAllIntent = PendingIntent.getActivity(context, 0,
                new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);

        // Action button 2: start a backup (via a receiver, which then queues WorkManager)
        PendingIntent backupIntent = PendingIntent.getBroadcast(context, 0,
                new Intent(context, NoteBackupReceiver.class), PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_note_reminder)
                .setContentTitle(context.getString(R.string.reminder_title))
                .setContentText(noteText)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(noteText)
                        .setBigContentTitle(noteTitle)
                        .setSummaryText(context.getString(R.string.reminder_title)))
                .setContentIntent(openNoteIntent)
                .addAction(0, context.getString(R.string.reminder_action_view_all), viewAllIntent)
                .addAction(0, context.getString(R.string.reminder_action_backup), backupIntent)
                .setAutoCancel(true); // dismiss when tapped

        NotificationManagerCompat.from(context).notify(NOTIFICATION_TAG, noteId, builder.build());
    }
}