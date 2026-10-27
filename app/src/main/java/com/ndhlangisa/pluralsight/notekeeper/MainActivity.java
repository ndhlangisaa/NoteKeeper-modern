package com.ndhlangisa.pluralsight.notekeeper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
//import android.os.strictmode.*;
import android.os.StrictMode;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.snackbar.Snackbar;
import com.ndhlangisa.pluralsight.notekeeper.NoteKeeperProviderContract.Notes;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private MainViewModel mViewModel;
    private DrawerLayout mDrawer;
    private NavigationView mNavigationView;
    private RecyclerView mRecyclerItems;
    private LinearLayoutManager mNotesLayoutManager;
    private GridLayoutManager mCoursesLayoutManager;
    private NoteRecyclerAdapter mNoteRecyclerAdapter;
    private CourseRecyclerAdapter mCourseRecyclerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        enableStrictMode();

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> startActivity(new Intent(this, NoteActivity.class)));

        // Write the default values from root_preferences.xml the very first time the app runs
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);

        // Make sure our notification channel exists (safe to call every time)
        NoteReminderNotification.createChannel(this);

        mDrawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, mDrawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        mDrawer.addDrawerListener(toggle);
        toggle.syncState();
        setupBackButton();

        mNavigationView = findViewById(R.id.nav_view);
        mNavigationView.setNavigationItemSelectedListener(this);

        mViewModel = new ViewModelProvider(this).get(MainViewModel.class);
        initializeDisplayContent();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mViewModel.loadNotes();   // pick up anything added or edited on the note screen
        updateNavHeader();        // pick up any name/email changes from Settings
    }

    private void enableStrictMode() {
        if (BuildConfig.DEBUG) {
            // In debug builds, log a warning whenever slow work (disk, network) happens on the main thread
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    /** Back button: close the drawer if it's open, otherwise behave normally (replaces onBackPressed). */
    private void setupBackButton() {
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (mDrawer.isDrawerOpen(GravityCompat.START)) {
                    mDrawer.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);                              // step out of the way...
                    getOnBackPressedDispatcher().onBackPressed();   // ...let Android handle Back
                    setEnabled(true);
                }
            }
        });
    }

    private void initializeDisplayContent() {
        mRecyclerItems = findViewById(R.id.list_items);
        mNotesLayoutManager = new LinearLayoutManager(this);
        mCoursesLayoutManager = new GridLayoutManager(this,
                getResources().getInteger(R.integer.course_grid_span));

        mNoteRecyclerAdapter = new NoteRecyclerAdapter();
        mCourseRecyclerAdapter = new CourseRecyclerAdapter();

        // Whenever the ViewModel delivers a new list, hand it to the matching adapter
        mViewModel.getNotes().observe(this, notes -> mNoteRecyclerAdapter.submitList(notes));
        mViewModel.getCourses().observe(this, courses -> mCourseRecyclerAdapter.submitList(courses));

//        displayNotes();
        // After a rotation, show the same list the user was looking at before
        if (mViewModel.isShowingCourses()) {
            displayCourses();
        } else {
            displayNotes();
        }
    }

    private void displayNotes() {
        mRecyclerItems.setLayoutManager(mNotesLayoutManager);
        mRecyclerItems.setAdapter(mNoteRecyclerAdapter);
        mNavigationView.setCheckedItem(R.id.nav_notes);

        // last line of displayNotes()
        mViewModel.setShowingCourses(false);
    }

    private void displayCourses() {
        mRecyclerItems.setLayoutManager(mCoursesLayoutManager);
        mRecyclerItems.setAdapter(mCourseRecyclerAdapter);
        mNavigationView.setCheckedItem(R.id.nav_courses);

        // last line of displayCourses()
        mViewModel.setShowingCourses(true);
    }

    private void updateNavHeader() {
        View headerView = mNavigationView.getHeaderView(0);
        TextView textUserName = headerView.findViewById(R.id.text_user_name);
        TextView textEmailAddress = headerView.findViewById(R.id.text_email_address);

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        textUserName.setText(pref.getString("user_display_name", ""));
        textEmailAddress.setText(pref.getString("user_email_address", ""));
    }

    // ---------- Navigation drawer ----------

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_notes) {
            displayNotes();
        } else if (id == R.id.nav_courses) {
            displayCourses();
        } else if (id == R.id.nav_share) {
            handleShare();
        } else if (id == R.id.nav_send) {
            handleSelection(R.string.nav_send_message);
        }

        mDrawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void handleShare() {
        String favoriteSocial = PreferenceManager.getDefaultSharedPreferences(this)
                .getString("user_favorite_social", "");
        Snackbar.make(mRecyclerItems, getString(R.string.nav_share_message, favoriteSocial),
                Snackbar.LENGTH_LONG).show();
    }

    private void handleSelection(int messageId) {
        Snackbar.make(mRecyclerItems, messageId, Snackbar.LENGTH_LONG).show();
    }

    // ---------- Toolbar overflow menu ----------

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        if (id == R.id.action_backup_notes) {
            backupNotes();
            return true;
        }
        if (id == R.id.action_upload_notes) {
            scheduleNoteUpload();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Runs the backup once, in the background, right away (replaces IntentService). */
    private void backupNotes() {
        NoteBackupWorker.enqueue(this, NoteBackup.ALL_COURSES);
        Snackbar.make(mRecyclerItems, R.string.backup_started, Snackbar.LENGTH_SHORT).show();
    }

    /** Runs the upload in the background, but only when there's a network (replaces JobScheduler). */
    private void scheduleNoteUpload() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        Data inputData = new Data.Builder()
                .putString(NoteUploadWorker.KEY_DATA_URI, Notes.CONTENT_URI.toString())
                .build();

        OneTimeWorkRequest uploadRequest = new OneTimeWorkRequest.Builder(NoteUploadWorker.class)
                .setConstraints(constraints)
                .setInputData(inputData)
                .build();

        // "Unique" work: if an upload is already queued or running, KEEP it instead of adding another
        WorkManager.getInstance(this).enqueueUniqueWork(
                NoteUploadWorker.UNIQUE_WORK_NAME, ExistingWorkPolicy.KEEP, uploadRequest);

        Snackbar.make(mRecyclerItems, R.string.upload_scheduled, Snackbar.LENGTH_SHORT).show();
    }
}