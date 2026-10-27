package com.ndhlangisa.pluralsight.notekeeper;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;

public class NoteActivity extends AppCompatActivity {
    public static final String NOTE_ID = "com.ndhlangisa.pluralsight.notekeeper.NOTE_ID";
    private static final long REMINDER_DELAY_MILLIS = 10 * 1000; // 10 seconds, handy for testing
    private ActivityResultLauncher<String> mNotificationPermissionLauncher;
    public static final int ID_NOT_SET = -1;
    private NoteViewModel mViewModel;
    private Spinner mSpinnerCourses;
    private ArrayAdapter<CourseInfo> mAdapterCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private ModuleStatusView mViewModuleStatus;
    private boolean mIsCancelling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_note);
        // Keep our content clear of the status bar and navigation bar
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mSpinnerCourses = findViewById(R.id.spinner_courses);
        mTextNoteTitle = findViewById(R.id.text_note_title);
        mTextNoteText = findViewById(R.id.text_note_text);
        mViewModuleStatus = findViewById(R.id.module_status);

        // The Spinner shows CourseInfo.toString() - the course title - for each item
        mAdapterCourses = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, new ArrayList<>());
        mAdapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mSpinnerCourses.setAdapter(mAdapterCourses);

        // Must be registered in onCreate. The lambda runs after the user answers the permission dialog.
        mNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted) {
                        scheduleReminder();
                    } else {
                        showSnackbar(R.string.reminder_permission_denied);
                    }
                });

        mViewModel = new ViewModelProvider(this).get(NoteViewModel.class);
        mViewModel.getCourses().observe(this, courses -> {
            mAdapterCourses.clear();
            mAdapterCourses.addAll(courses);
            displayNoteWhenReady();
        });
        mViewModel.getNote().observe(this, note -> displayNoteWhenReady());

        int noteId = getIntent().getIntExtra(NOTE_ID, ID_NOT_SET);
        mViewModel.start(noteId);
        loadModuleStatusValues();
    }

    /** Fills the form - but only once BOTH the courses and the note have arrived. */
    private void displayNoteWhenReady() {
        NoteInfo note = mViewModel.getNote().getValue();
        if (note == null || mAdapterCourses.isEmpty() || mViewModel.isNoteDisplayed())
            return;

        // getPosition() uses CourseInfo.equals(), which compares course IDs
        int coursePosition = mAdapterCourses.getPosition(note.getCourse());
        if (coursePosition >= 0) mSpinnerCourses.setSelection(coursePosition);
        mTextNoteTitle.setText(note.getTitle());
        mTextNoteText.setText(note.getText());

        // Only fill the form the first time. After a rotation, Android restores what the user typed.
        mViewModel.setNoteDisplayed(true);
    }

    private void loadModuleStatusValues() {
        // In real life we'd look up the selected course's module statuses from the database
        int totalNumberOfModules = 11;
        int completedNumberOfModules = 7;
        boolean[] moduleStatus = new boolean[totalNumberOfModules];
        for (int moduleIndex = 0; moduleIndex < completedNumberOfModules; moduleIndex++)
            moduleStatus[moduleIndex] = true;

        mViewModuleStatus.setModuleStatus(moduleStatus);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mIsCancelling) {
            if (mViewModel.isNewNote()) {
                mViewModel.deleteNote();          // throw away the note row we created
            } else {
                mViewModel.restoreOriginalNote(); // undo any saves made during this visit
            }
        } else if (mViewModel.isNewNote() && isFinishing() && isNoteBlank()) {
            mViewModel.deleteNote();              // brand-new note left empty: don't keep it
        } else {
            saveNote();
        }
    }

    private void saveNote() {
        // An existing note that hasn't been shown yet must not be overwritten with an empty form
        if (!mViewModel.isNewNote() && !mViewModel.isNoteDisplayed()) return;

        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        if (course == null) return; // courses haven't loaded yet

        mViewModel.saveNote(course.getCourseId(), textOf(mTextNoteTitle), textOf(mTextNoteText));
    }

    private boolean isNoteBlank() {
        return textOf(mTextNoteTitle).trim().isEmpty() && textOf(mTextNoteText).trim().isEmpty();
    }

    private static String textOf(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString();
    }

    // ---------- Toolbar menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        }
        if (id == R.id.action_cancel) {
            mIsCancelling = true;
            finish();
            return true;
        }

        if (id == R.id.action_set_reminder) {
            onSetReminderClicked();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        if (course == null) return;

        String subject = textOf(mTextNoteTitle);
        String text = getString(R.string.email_body_template, course.getTitle(), textOf(mTextNoteText));

        // ACTION_SENDTO + "mailto:" means "only apps that send email"
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:"));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, text);
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            showSnackbar(R.string.no_email_app);
        }
    }

    // ---------- Reminders ----------

    private void onSetReminderClicked() {
        boolean needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED;

        if (needsPermission) {
            mNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            scheduleReminder();
        }
    }

    private void scheduleReminder() {
        Uri noteUri = mViewModel.getNoteUri();
        if (noteUri == null) return;
        int noteId = (int) ContentUris.parseId(noteUri);

        // What should happen when the alarm goes off: send this broadcast to NoteReminderReceiver
        Intent intent = new Intent(this, NoteReminderReceiver.class);
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TITLE, textOf(mTextNoteTitle));
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_TEXT, textOf(mTextNoteText));
        intent.putExtra(NoteReminderReceiver.EXTRA_NOTE_ID, noteId);

        // A PendingIntent lets the system send that broadcast for us later.
        // FLAG_IMMUTABLE is required since Android 12.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, noteId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = getSystemService(AlarmManager.class);
        long alarmTime = SystemClock.elapsedRealtime() + REMINDER_DELAY_MILLIS;
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, alarmTime, pendingIntent);

        showSnackbar(R.string.reminder_set);
    }

    private void showSnackbar(int messageId) {
        Snackbar.make(findViewById(R.id.main), messageId, Snackbar.LENGTH_LONG).show();
    }
}