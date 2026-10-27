package com.ndhlangisa.pluralsight.notekeeper;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;

import java.util.List;

/**
 * Holds the data for MainActivity (replaces the original CursorLoader).
 * A ViewModel survives screen rotation, so the lists aren't reloaded every time the phone turns.
 */
public class MainViewModel extends AndroidViewModel {
    private static final String KEY_SHOWING_COURSES = "showing_courses";
    private final SavedStateHandle mState;
    private final NoteKeeperRepository mRepository;
    private final MutableLiveData<List<NoteInfo>> mNotes = new MutableLiveData<>();
    private final MutableLiveData<List<CourseInfo>> mCourses = new MutableLiveData<>();

//    public MainViewModel(@NonNull Application application) {
//        super(application);
//        mRepository = new NoteKeeperRepository(application);
//        loadCourses(); // courses never change while the app runs, so load them once
//    }
public MainViewModel(@NonNull Application application, @NonNull SavedStateHandle state) {
    super(application);
    mRepository = new NoteKeeperRepository(application);
    mState = state;
    loadCourses(); // courses never change while the app runs, so load them once
}

    /** Which list is on screen. Kept in SavedStateHandle so it survives rotation and process death. */
    public boolean isShowingCourses() {
        Boolean showingCourses = mState.get(KEY_SHOWING_COURSES);
        return showingCourses != null && showingCourses;
    }

    public void setShowingCourses(boolean showingCourses) {
        mState.set(KEY_SHOWING_COURSES, showingCourses);
    }

    /** The activity observes these; it is told whenever a new list arrives. */
    public LiveData<List<NoteInfo>> getNotes() {
        return mNotes;
    }

    public LiveData<List<CourseInfo>> getCourses() {
        return mCourses;
    }

    /** Re-reads the notes (MainActivity calls this in onResume, after the user may have edited one). */
    public void loadNotes() {
        // postValue = "deliver this to observers on the main thread" (safe to call from any thread)
        NoteKeeperRepository.runInBackground(() -> mNotes.postValue(mRepository.getNotes()));
    }

    private void loadCourses() {
        NoteKeeperRepository.runInBackground(() -> mCourses.postValue(mRepository.getCourses()));
    }
}