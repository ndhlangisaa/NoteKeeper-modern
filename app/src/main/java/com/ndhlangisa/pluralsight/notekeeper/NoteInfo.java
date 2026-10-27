package com.ndhlangisa.pluralsight.notekeeper;

import androidx.annotation.NonNull;

import java.util.Objects;

/**
 * One note: which course it belongs to, plus a title and body text.
 * Immutable, so it is safe to pass between the background thread and the UI thread.
 */
public final class NoteInfo {
    private final int mId;
    private final CourseInfo mCourse;
    private final String mTitle;
    private final String mText;

    public NoteInfo(int id, CourseInfo course, String title, String text) {
        mId = id;
        mCourse = course;
        mTitle = title;
        mText = text;
    }

    public int getId() {
        return mId;
    }

    public CourseInfo getCourse() {
        return mCourse;
    }

    public String getTitle() {
        return mTitle;
    }

    public String getText() {
        return mText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NoteInfo that = (NoteInfo) o;
        return mId == that.mId
                && Objects.equals(mCourse, that.mCourse)
                && Objects.equals(mTitle, that.mTitle)
                && Objects.equals(mText, that.mText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mId, mCourse, mTitle, mText);
    }

    @NonNull
    @Override
    public String toString() {
        return mCourse.getCourseId() + "|" + mTitle + "|" + mText;
    }
}