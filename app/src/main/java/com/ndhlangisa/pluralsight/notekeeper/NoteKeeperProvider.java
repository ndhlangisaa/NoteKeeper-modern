package com.ndhlangisa.pluralsight.notekeeper;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.provider.BaseColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperDatabaseContract.CourseInfoEntry;
import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperDatabaseContract.NoteInfoEntry;
import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Courses;
import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.CoursesIdColumns;
import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Notes;

/**
 * The single "front door" to NoteKeeper's data. Everything else asks for data with a content:// URI
 * and this class turns the URI into the right SQLite query.
 */
public class NoteKeeperProvider extends ContentProvider {

    private static final String MIME_VENDOR_TYPE = "vnd." + NoteKeeperProviderContract.AUTHORITY + ".";

    // One number per kind of URI we understand
    private static final int COURSES = 0;
    private static final int NOTES = 1;
    private static final int NOTES_EXPANDED = 2;
    private static final int NOTES_ROW = 3;
    private static final int COURSES_ROW = 4;
    private static final int NOTES_EXPANDED_ROW = 5;

    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Courses.PATH, COURSES);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH, NOTES);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH_EXPANDED, NOTES_EXPANDED);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Courses.PATH + "/#", COURSES_ROW);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH + "/#", NOTES_ROW);
        sUriMatcher.addURI(NoteKeeperProviderContract.AUTHORITY, Notes.PATH_EXPANDED + "/#", NOTES_EXPANDED_ROW);
    }

    private NoteKeeperOpenHelper mDbOpenHelper;

    @Override
    public boolean onCreate() {
        // Keep this fast: just create the helper. The database file is opened later, on first use.
        mDbOpenHelper = new NoteKeeperOpenHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase db = mDbOpenHelper.getReadableDatabase();
        long rowId;
        String rowSelection;
        String[] rowSelectionArgs;

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                return db.query(CourseInfoEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
            case NOTES:
                return db.query(NoteInfoEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
            case NOTES_EXPANDED:
                return notesExpandedQuery(db, projection, selection, selectionArgs, sortOrder);
            case COURSES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = CourseInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.query(CourseInfoEntry.TABLE_NAME, projection, rowSelection,
                        rowSelectionArgs, null, null, null);
            case NOTES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.query(NoteInfoEntry.TABLE_NAME, projection, rowSelection,
                        rowSelectionArgs, null, null, null);
            case NOTES_EXPANDED_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry.getQName(NoteInfoEntry._ID) + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                return notesExpandedQuery(db, projection, rowSelection, rowSelectionArgs, null);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    /** Notes JOINed with courses, so each row also carries the course title. */
    private Cursor notesExpandedQuery(SQLiteDatabase db, String[] projection, String selection,
                                      String[] selectionArgs, String sortOrder) {
        if (projection == null) {
            throw new IllegalArgumentException("A projection (list of columns) is required for expanded notes");
        }

        // _id and course_id exist in BOTH tables, so we must say which table we mean
        String[] columns = new String[projection.length];
        for (int idx = 0; idx < projection.length; idx++) {
            columns[idx] = projection[idx].equals(BaseColumns._ID) ||
                    projection[idx].equals(CoursesIdColumns.COLUMN_COURSE_ID) ?
                    NoteInfoEntry.getQName(projection[idx]) : projection[idx];
        }

        // note_info JOIN course_info ON note_info.course_id = course_info.course_id
        String tablesWithJoin = NoteInfoEntry.TABLE_NAME + " JOIN " +
                CourseInfoEntry.TABLE_NAME + " ON " +
                NoteInfoEntry.getQName(NoteInfoEntry.COLUMN_COURSE_ID) + " = " +
                CourseInfoEntry.getQName(CourseInfoEntry.COLUMN_COURSE_ID);

        return db.query(tablesWithJoin, columns, selection, selectionArgs,
                null, null, sortOrder);
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId;

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case NOTES:
                rowId = db.insert(NoteInfoEntry.TABLE_NAME, null, values);
                // content://com.ndhlangisa.pluralsight.notekeeper.provider/notes/1
                return ContentUris.withAppendedId(Notes.CONTENT_URI, rowId);
            case COURSES:
                rowId = db.insert(CourseInfoEntry.TABLE_NAME, null, values);
                return ContentUris.withAppendedId(Courses.CONTENT_URI, rowId);
            case NOTES_EXPANDED:
                throw new UnsupportedOperationException("Expanded notes are read-only: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId;
        String rowSelection;
        String[] rowSelectionArgs;

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                return db.update(CourseInfoEntry.TABLE_NAME, values, selection, selectionArgs);
            case NOTES:
                return db.update(NoteInfoEntry.TABLE_NAME, values, selection, selectionArgs);
            case COURSES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = CourseInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.update(CourseInfoEntry.TABLE_NAME, values, rowSelection, rowSelectionArgs);
            case NOTES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.update(NoteInfoEntry.TABLE_NAME, values, rowSelection, rowSelectionArgs);
            case NOTES_EXPANDED:
            case NOTES_EXPANDED_ROW:
                throw new UnsupportedOperationException("Expanded notes are read-only: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase db = mDbOpenHelper.getWritableDatabase();
        long rowId;
        String rowSelection;
        String[] rowSelectionArgs;

        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                return db.delete(CourseInfoEntry.TABLE_NAME, selection, selectionArgs);
            case NOTES:
                return db.delete(NoteInfoEntry.TABLE_NAME, selection, selectionArgs);
            case COURSES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = CourseInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.delete(CourseInfoEntry.TABLE_NAME, rowSelection, rowSelectionArgs);
            case NOTES_ROW:
                rowId = ContentUris.parseId(uri);
                rowSelection = NoteInfoEntry._ID + " = ?";
                rowSelectionArgs = new String[]{Long.toString(rowId)};
                return db.delete(NoteInfoEntry.TABLE_NAME, rowSelection, rowSelectionArgs);
            case NOTES_EXPANDED:
            case NOTES_EXPANDED_ROW:
                throw new UnsupportedOperationException("Expanded notes are read-only: " + uri);
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    /** Tells callers what kind of data a URI points to: a directory (many rows) or a single item. */
    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        int uriMatch = sUriMatcher.match(uri);
        switch (uriMatch) {
            case COURSES:
                // vnd.android.cursor.dir/vnd.com.ndhlangisa.pluralsight.notekeeper.provider.courses
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Courses.PATH;
            case NOTES:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH;
            case NOTES_EXPANDED:
                return ContentResolver.CURSOR_DIR_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH_EXPANDED;
            case COURSES_ROW:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Courses.PATH;
            case NOTES_ROW:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH;
            case NOTES_EXPANDED_ROW:
                return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" + MIME_VENDOR_TYPE + Notes.PATH_EXPANDED;
            default:
                return null;
        }
    }
}