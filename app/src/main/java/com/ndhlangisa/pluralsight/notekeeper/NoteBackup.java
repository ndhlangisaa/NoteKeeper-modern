package com.ndhlangisa.pluralsight.notekeeper;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Notes;

/**
 * Pretends to back up notes: it reads them and logs each one, sleeping a second per note
 * to simulate slow work. Watch the output in Logcat (filter by "NoteBackup").
 */
public class NoteBackup {
    public static final String ALL_COURSES = "ALL_COURSES";
    private static final String TAG = NoteBackup.class.getSimpleName();

    @WorkerThread
    public static void doBackup(Context context, String backupCourseId) {
        String[] columns = {
                Notes.COLUMN_COURSE_ID,
                Notes.COLUMN_NOTE_TITLE,
                Notes.COLUMN_NOTE_TEXT,
        };

        String selection = null;
        String[] selectionArgs = null;
        if (!backupCourseId.equals(ALL_COURSES)) {
            selection = Notes.COLUMN_COURSE_ID + " = ?";
            selectionArgs = new String[]{backupCourseId};
        }

        try (Cursor cursor = context.getContentResolver()
                .query(Notes.CONTENT_URI, columns, selection, selectionArgs, null)) {
            if (cursor == null) return;
            int courseIdPos = cursor.getColumnIndexOrThrow(Notes.COLUMN_COURSE_ID);
            int noteTitlePos = cursor.getColumnIndexOrThrow(Notes.COLUMN_NOTE_TITLE);
            int noteTextPos = cursor.getColumnIndexOrThrow(Notes.COLUMN_NOTE_TEXT);

            Log.i(TAG, ">>>***   BACKUP START - Thread: " + Thread.currentThread().getName() + "   ***<<<");
            while (cursor.moveToNext()) {
                String courseId = cursor.getString(courseIdPos);
                String noteTitle = cursor.getString(noteTitlePos);
                String noteText = cursor.getString(noteTextPos);

                if (!noteTitle.isEmpty()) {
                    Log.i(TAG, ">>>Backing Up Note<<< " + courseId + "|" + noteTitle + "|" + noteText);
                    simulateLongRunningWork();
                }
            }
            Log.i(TAG, ">>>***   BACKUP COMPLETE   ***<<<");
        }
    }

    private static void simulateLongRunningWork() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}