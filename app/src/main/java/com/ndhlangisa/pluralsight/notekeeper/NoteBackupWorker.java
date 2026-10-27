package com.ndhlangisa.pluralsight.notekeeper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * Runs NoteBackup on a background thread managed by WorkManager.
 * (Replaces the original NoteBackupService, which used the now-deprecated IntentService.)
 */
public class NoteBackupWorker extends Worker {
    public static final String KEY_COURSE_ID = "course_id";

    public NoteBackupWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    /** WorkManager calls this on a background thread. */
    @NonNull
    @Override
    public Result doWork() {
        String backupCourseId = getInputData().getString(KEY_COURSE_ID);
        if (backupCourseId == null) backupCourseId = NoteBackup.ALL_COURSES;

        NoteBackup.doBackup(getApplicationContext(), backupCourseId);
        return Result.success();
    }

    /** Convenience method: queue a one-off backup to run as soon as possible. */
    public static void enqueue(Context context, String courseId) {
        Data inputData = new Data.Builder()
                .putString(KEY_COURSE_ID, courseId)
                .build();

        OneTimeWorkRequest backupRequest = new OneTimeWorkRequest.Builder(NoteBackupWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(context).enqueue(backupRequest);
    }
}