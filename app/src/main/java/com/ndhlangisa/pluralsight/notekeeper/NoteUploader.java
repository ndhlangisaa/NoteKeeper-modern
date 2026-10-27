package com.ndhlangisa.pluralsight.notekeeper;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Notes;

/**
 * Pretends to upload notes to a server (3 seconds per note) and can be cancelled part-way through.
 */
public class NoteUploader {
    private static final String TAG = NoteUploader.class.getSimpleName();

    private final Context mContext;
    // volatile: cancel() is called from a different thread than doUpload() runs on
    private volatile boolean mCanceled;

    public NoteUploader(Context context) {
        mContext = context;
    }

    public boolean isCanceled() {
        return mCanceled;
    }

    public void cancel() {
        mCanceled = true;
    }

    @WorkerThread
    public void doUpload(Uri dataUri) {
        String[] columns = {
                Notes.COLUMN_COURSE_ID,
                Notes.COLUMN_NOTE_TITLE,
                Notes.COLUMN_NOTE_TEXT,
        };

        try (Cursor cursor = mContext.getContentResolver().query(dataUri, columns, null, null, null)) {
            if (cursor == null) return;
            int courseIdPos = cursor.getColumnIndexOrThrow(Notes.COLUMN_COURSE_ID);
            int noteTitlePos = cursor.getColumnIndexOrThrow(Notes.COLUMN_NOTE_TITLE);
            int noteTextPos = cursor.getColumnIndexOrThrow(Notes.COLUMN_NOTE_TEXT);

            Log.i(TAG, ">>>*** UPLOAD START - " + dataUri + " ***<<<");
            mCanceled = false;
            while (!mCanceled && cursor.moveToNext()) {
                String courseId = cursor.getString(courseIdPos);
                String noteTitle = cursor.getString(noteTitlePos);
                String noteText = cursor.getString(noteTextPos);

                if (!noteTitle.isEmpty()) {
                    Log.i(TAG, ">>>Uploading Note<<< " + courseId + "|" + noteTitle + "|" + noteText);
                    simulateLongRunningWork();
                }
            }
            if (mCanceled)
                Log.i(TAG, ">>>*** UPLOAD !!CANCELED!! - " + dataUri + " ***<<<");
            else
                Log.i(TAG, ">>>*** UPLOAD COMPLETE - " + dataUri + " ***<<<");
        }
    }

    private static void simulateLongRunningWork() {
        try {
            Thread.sleep(3000);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}