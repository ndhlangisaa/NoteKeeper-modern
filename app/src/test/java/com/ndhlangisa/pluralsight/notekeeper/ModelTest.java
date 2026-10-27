package com.ndhlangisa.pluralsight.notekeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

/**
 * Local unit tests: plain Java, run on your Mac in a second - no emulator needed.
 */
public class ModelTest {

    @Test
    public void courseToStringIsItsTitle() {
        // The note screen's Spinner displays toString(), so this must stay the title
        CourseInfo course = new CourseInfo("java_lang", "Java Fundamentals: The Java Language");
        assertEquals("Java Fundamentals: The Java Language", course.toString());
    }

    @Test
    public void coursesWithTheSameIdAreEqual() {
        // NoteActivity relies on this to find a note's course in the Spinner
        CourseInfo fromCourseList = new CourseInfo("android_async", "Android Async Programming and Services");
        CourseInfo fromNote = new CourseInfo("android_async", "Different title, same id");
        assertEquals(fromCourseList, fromNote);
        assertEquals(fromCourseList.hashCode(), fromNote.hashCode());
    }

    @Test
    public void notesWithDifferentTextAreNotEqual() {
        // ListAdapter uses equals() to decide whether a row needs redrawing
        CourseInfo course = new CourseInfo("java_core", "Java Fundamentals: The Core Platform");
        NoteInfo original = new NoteInfo(1, course, "Serialization", "Remember SerialVersionUID");
        NoteInfo edited = new NoteInfo(1, course, "Serialization", "Remember serialVersionUID!");
        assertNotEquals(original, edited);
    }
}