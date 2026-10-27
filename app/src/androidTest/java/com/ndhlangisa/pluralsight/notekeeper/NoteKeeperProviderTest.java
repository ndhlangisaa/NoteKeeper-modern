package com.ndhlangisa.pluralsight.notekeeper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/**
 * Instrumented tests: they run ON the emulator/phone, against the real ContentProvider and database.
 */
@RunWith(AndroidJUnit4.class)
public class NoteKeeperProviderTest {
    private NoteKeeperRepository mRepository;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        mRepository = new NoteKeeperRepository(context);
    }

    @Test
    public void databaseIsSeededWithFourCourses() {
        List<CourseInfo> courses = mRepository.getCourses();
        assertEquals(4, courses.size());
        assertTrue(courses.contains(new CourseInfo("android_intents", "")));
    }

    @Test
    public void createReadUpdateDeleteNote() {
        // Create
        Uri noteUri = mRepository.insertEmptyNote();
        assertNotNull(noteUri);
        long noteId = ContentUris.parseId(noteUri);

        // Update, then read it back (joined with its course)
        mRepository.updateNote(noteUri, "java_lang", "Test title", "Test text");
        NoteInfo note = mRepository.getNote(noteId);
        assertNotNull(note);
        assertEquals("Test title", note.getTitle());
        assertEquals("Test text", note.getText());
        assertEquals("Java Fundamentals: The Java Language", note.getCourse().getTitle());

        // Delete - and make sure it's really gone
        mRepository.deleteNote(noteUri);
        assertNull(mRepository.getNote(noteId));
    }
}