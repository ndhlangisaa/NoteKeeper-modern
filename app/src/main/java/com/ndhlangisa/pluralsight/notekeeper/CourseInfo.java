package com.ndhlangisa.pluralsight.notekeeper;

import androidx.annotation.NonNull;

/**
 * A course the user can attach notes to (e.g. "Android Programming with Intents").
 * Immutable: once created, its values never change.
 */
public final class CourseInfo {
    private final String mCourseId;
    private final String mTitle;

    public CourseInfo(String courseId, String title) {
        mCourseId = courseId;
        mTitle = title;
    }

    public String getCourseId() {
        return mCourseId;
    }

    public String getTitle() {
        return mTitle;
    }

    /** The Spinner on the note screen calls toString() to get the text it displays. */
    @NonNull
    @Override
    public String toString() {
        return mTitle;
    }

    /** Two CourseInfo objects are "the same course" when their IDs match. */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CourseInfo that = (CourseInfo) o;
        return mCourseId.equals(that.mCourseId);
    }

    @Override
    public int hashCode() {
        return mCourseId.hashCode();
    }
}