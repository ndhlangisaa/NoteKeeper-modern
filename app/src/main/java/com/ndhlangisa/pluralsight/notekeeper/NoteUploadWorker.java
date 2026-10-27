package com.ndhlangisa.pluralsight.notekeeper;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Runs NoteUploader when WorkManager decides the conditions (network connected) are met.
 * (Replaces the original NoteUploaderJobService, which used JobScheduler + AsyncTask.)
 */
public class NoteUploadWorker extends Worker {
    public static final String KEY_DATA_URI = "data_uri";
    public static final String UNIQUE_WORK_NAME = "note_upload";

    private volatile NoteUploader mNoteUploader;

    public NoteUploadWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String stringDataUri = getInputData().getString(KEY_DATA_URI);
        if (stringDataUri == null) return Result.failure();

        mNoteUploader = new NoteUploader(getApplicationContext());
        mNoteUploader.doUpload(Uri.parse(stringDataUri));

        // If we were stopped part-way (e.g. the network dropped), ask WorkManager to try again later
        return mNoteUploader.isCanceled() ? Result.retry() : Result.success();
    }

    /** WorkManager calls this when the constraints are no longer met, or the work is cancelled. */
    @Override
    public void onStopped() {
        super.onStopped();
        NoteUploader uploader = mNoteUploader;
        if (uploader != null) uploader.cancel();
    }
}