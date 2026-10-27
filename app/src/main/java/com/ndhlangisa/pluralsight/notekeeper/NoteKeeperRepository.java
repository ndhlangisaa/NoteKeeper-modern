package com.ndhlangisa.pluralsight.notekeeper;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Courses;
import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Notes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The app's single place for reading and writing data (replaces the original in-memory DataManager).
 * It talks to our ContentProvider and turns Cursors into plain Java objects.
 *
 * Every method marked @WorkerThread does disk work, so it must NOT run on the main (UI) thread.
 * Use runInBackground(...) to call them.
 */
public class NoteKeeperRepository {

    // ONE background thread for the whole app. Because there is only one, database jobs run
    // one at a time in the order they were submitted - so a save always finishes before a later load.
    private static final ExecutorService sBackgroundThread = Executors.newSingleThreadExecutor();

    private final ContentResolver mResolver;

    public NoteKeeperRepository(Context context) {
        mResolver = context.getApplicationContext().getContentResolver();
    }

    /** Runs a piece of work on the shared background thread. */
    public static void runInBackground(Runnable work) {
        sBackgroundThread.execute(work);
    }

    /** All courses, sorted by title. */
    @WorkerThread
    public List<CourseInfo> getCourses() {
        String[] columns = {Courses.COLUMN_COURSE_ID, Courses.COLUMN_COURSE_TITLE};
        List<CourseInfo> courses = new ArrayList<>();

        try (Cursor cursor = mResolver.query(Courses.CONTENT_URI, columns,
                null, null, Courses.COLUMN_COURSE_TITLE)) {
            if (cursor == null) return courses;
            int courseIdPos = cursor.getColumnIndexOrThrow(Courses.COLUMN_COURSE_ID);
            int courseTitlePos = cursor.getColumnIndexOrThrow(Courses.COLUMN_COURSE_TITLE);
            while (cursor.moveToNext()) {
                courses.add(new CourseInfo(cursor.getString(courseIdPos), cursor.getString(courseTitlePos)));
            }
        }
        return courses;
    }

    /** All notes with their course titles, sorted by course then note title. */
    @WorkerThread
    public List<NoteInfo> getNotes() {
        String sortOrder = Notes.COLUMN_COURSE_TITLE + "," + Notes.COLUMN_NOTE_TITLE;
        List<NoteInfo> notes = new ArrayList<>();

        try (Cursor cursor = mResolver.query(Notes.CONTENT_EXPANDED_URI, EXPANDED_NOTE_COLUMNS,
                null, null, sortOrder)) {
            if (cursor == null) return notes;
            while (cursor.moveToNext()) {
                notes.add(readNote(cursor));
            }
        }
        return notes;
    }

    /** One note (with its course title), or null if it doesn't exist. */
    @WorkerThread
    @Nullable
    public NoteInfo getNote(long noteId) {
        // content://com.ndhlangisa.pluralsight.notekeeper.provider/notes_expanded/3
        Uri noteUri = ContentUris.withAppendedId(Notes.CONTENT_EXPANDED_URI, noteId);
        try (Cursor cursor = mResolver.query(noteUri, EXPANDED_NOTE_COLUMNS, null, null, null)) {
            if (cursor == null || !cursor.moveToFirst()) return null;
            return readNote(cursor);
        }
    }

    /** Creates an empty note row and returns its URI, e.g. content://.../notes/9 */
    @WorkerThread
    public Uri insertEmptyNote() {
        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, "");
        values.put(Notes.COLUMN_NOTE_TITLE, "");
        values.put(Notes.COLUMN_NOTE_TEXT, "");
        return mResolver.insert(Notes.CONTENT_URI, values);
    }

    @WorkerThread
    public void updateNote(Uri noteUri, String courseId, String title, String text) {
        ContentValues values = new ContentValues();
        values.put(Notes.COLUMN_COURSE_ID, courseId);
        values.put(Notes.COLUMN_NOTE_TITLE, title);
        values.put(Notes.COLUMN_NOTE_TEXT, text);
        mResolver.update(noteUri, values, null, null);
    }

    @WorkerThread
    public void deleteNote(Uri noteUri) {
        mResolver.delete(noteUri, null, null);
    }

    // ---- helpers ----

    private static final String[] EXPANDED_NOTE_COLUMNS = {
            Notes._ID,
            Notes.COLUMN_COURSE_ID,
            Notes.COLUMN_COURSE_TITLE,
            Notes.COLUMN_NOTE_TITLE,
            Notes.COLUMN_NOTE_TEXT
    };

    /** Converts the cursor's CURRENT row into a NoteInfo. */
    private static NoteInfo readNote(Cursor cursor) {
        int id = cursor.getInt(cursor.getColumnIndexOrThrow(Notes._ID));
        String courseId = cursor.getString(cursor.getColumnIndexOrThrow(Notes.COLUMN_COURSE_ID));
        String courseTitle = cursor.getString(cursor.getColumnIndexOrThrow(Notes.COLUMN_COURSE_TITLE));
        String noteTitle = cursor.getString(cursor.getColumnIndexOrThrow(Notes.COLUMN_NOTE_TITLE));
        String noteText = cursor.getString(cursor.getColumnIndexOrThrow(Notes.COLUMN_NOTE_TEXT));
        return new NoteInfo(id, new CourseInfo(courseId, courseTitle), noteTitle, noteText);
    }
}