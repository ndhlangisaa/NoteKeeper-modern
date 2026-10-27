package com.ndhlangisa.pluralsight.notekeeper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Receives the "Backup notes" button press from the reminder notification and queues a backup.
 * (Notifications can no longer start background services directly, so we hop through a receiver.)
 */
public class NoteBackupReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        NoteBackupWorker.enqueue(context, NoteBackup.ALL_COURSES);
    }
}