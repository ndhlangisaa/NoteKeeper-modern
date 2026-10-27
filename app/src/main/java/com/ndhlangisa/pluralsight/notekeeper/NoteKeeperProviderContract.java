package com.ndhlangisa.pluralsight.notekeeper;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * The PUBLIC description of our ContentProvider: its address (authority), its URIs and column names.
 * Code that talks to the provider uses these constants instead of the database contract.
 */
public final class NoteKeeperProviderContract {
    private NoteKeeperProviderContract() {}

    public static final String AUTHORITY = "com.ndhlangisa.pluralsight.notekeeper.provider";
    public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);

    protected interface CoursesIdColumns {
        String COLUMN_COURSE_ID = "course_id";
    }

    protected interface CoursesColumns {
        String COLUMN_COURSE_TITLE = "course_title";
    }

    protected interface NotesColumns {
        String COLUMN_NOTE_TITLE = "note_title";
        String COLUMN_NOTE_TEXT = "note_text";
    }

    public static final class Courses implements BaseColumns, CoursesColumns, CoursesIdColumns {
        public static final String PATH = "courses";
        // content://com.ndhlangisa.pluralsight.notekeeper.provider/courses
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);
    }

    public static final class Notes implements BaseColumns, NotesColumns, CoursesIdColumns, CoursesColumns {
        public static final String PATH = "notes";
        // content://com.ndhlangisa.pluralsight.notekeeper.provider/notes
        public static final Uri CONTENT_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH);

        // "Expanded" notes = notes JOINed with courses, so each row also has the course title
        public static final String PATH_EXPANDED = "notes_expanded";
        public static final Uri CONTENT_EXPANDED_URI = Uri.withAppendedPath(AUTHORITY_URI, PATH_EXPANDED);
    }
}