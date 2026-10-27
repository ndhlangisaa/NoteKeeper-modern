package com.ndhlangisa.pluralsight.notekeeper;

import android.app.Application;
import android.content.ContentUris;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Notes;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Holds the data and database work for NoteActivity
 * (replaces the original's CursorLoaders, AsyncTasks and onSaveInstanceState code).
 */
public class NoteViewModel extends AndroidViewModel {
    // Keys for values that must survive even if Android kills the app in the background
    private static final String KEY_NOTE_URI = "note_uri";
    private static final String KEY_IS_NEW_NOTE = "is_new_note";

    private final NoteKeeperRepository mRepository;
    private final SavedStateHandle mState;
    private final MutableLiveData<List<CourseInfo>> mCourses = new MutableLiveData<>();
    private final MutableLiveData<NoteInfo> mNote = new MutableLiveData<>();
    private boolean mStarted;
    private boolean mNoteDisplayed;

    // The default ViewModelProvider factory sees this constructor and passes both values in for us
    public NoteViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
        super(application);
        mRepository = new NoteKeeperRepository(application);
        mState = state;
    }

    public LiveData<List<CourseInfo>> getCourses() {
        return mCourses;
    }

    /** The note exactly as it was when the screen opened (used to undo on Cancel). */
    public LiveData<NoteInfo> getNote() {
        return mNote;
    }

    @Nullable
    public Uri getNoteUri() {
        return mState.get(KEY_NOTE_URI);
    }

    public boolean isNewNote() {
        Boolean isNewNote = mState.get(KEY_IS_NEW_NOTE);
        return isNewNote != null && isNewNote;
    }

    public boolean isNoteDisplayed() {
        return mNoteDisplayed;
    }

    public void setNoteDisplayed(boolean noteDisplayed) {
        mNoteDisplayed = noteDisplayed;
    }

    /** Called from NoteActivity.onCreate. Does its work only once, even if the screen rotates. */
    public void start(int noteId) {
        if (mStarted) return;
        mStarted = true;

        NoteKeeperRepository.runInBackground(() -> mCourses.postValue(mRepository.getCourses()));

        if (getNoteUri() != null) {
            // Android killed our process while this screen was in the background, and is now
            // restoring it. SavedStateHandle kept the URI, so don't create another note.
            if (!isNewNote()) loadNote();
            return;
        }

        if (noteId == NoteActivity.ID_NOT_SET) {
            mState.set(KEY_IS_NEW_NOTE, true);
            createNewNote();
        } else {
            mState.set(KEY_IS_NEW_NOTE, false);
            mState.set(KEY_NOTE_URI, ContentUris.withAppendedId(Notes.CONTENT_URI, noteId));
            loadNote();
        }
    }

    private void createNewNote() {
        Executor mainThread = ContextCompat.getMainExecutor(getApplication());
        NoteKeeperRepository.runInBackground(() -> {
            Uri newNoteUri = mRepository.insertEmptyNote();
            // SavedStateHandle must be updated on the main thread
            mainThread.execute(() -> mState.set(KEY_NOTE_URI, newNoteUri));
        });
    }

    private void loadNote() {
        long noteId = ContentUris.parseId(getNoteUri());
        NoteKeeperRepository.runInBackground(() -> mNote.postValue(mRepository.getNote(noteId)));
    }

    public void saveNote(String courseId, String title, String text) {
        Uri noteUri = getNoteUri();
        if (noteUri == null) return; // the new note row hasn't been created yet
        NoteKeeperRepository.runInBackground(() -> mRepository.updateNote(noteUri, courseId, title, text));
    }

    public void deleteNote() {
        Uri noteUri = getNoteUri();
        if (noteUri == null) return;
        NoteKeeperRepository.runInBackground(() -> mRepository.deleteNote(noteUri));
    }

    /** Puts the original values back (used when the user presses Cancel on an existing note). */
    public void restoreOriginalNote() {
        NoteInfo original = mNote.getValue();
        if (original == null) return;
        saveNote(original.getCourse().getCourseId(), original.getTitle(), original.getText());
    }
}