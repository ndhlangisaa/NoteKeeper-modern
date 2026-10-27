package com.ndhlangisa.pluralsight.notekeeper;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Turns a List of NoteInfo into rows in the RecyclerView.
 * ListAdapter + DiffUtil work out which rows changed, so only those are redrawn.
 */
public class NoteRecyclerAdapter extends ListAdapter<NoteInfo, NoteRecyclerAdapter.ViewHolder> {

    public NoteRecyclerAdapter() {
        super(DIFF_CALLBACK);
    }

    /** Called when the RecyclerView needs a NEW row view (only a screenful or so are ever created). */
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_list, parent, false);
        return new ViewHolder(itemView);
    }

    /** Called to put the note at `position` into an existing (possibly recycled) row. */
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NoteInfo note = getItem(position);
        holder.mTextCourse.setText(note.getCourse().getTitle());
        holder.mTextTitle.setText(note.getTitle());
        holder.mId = note.getId();
    }

    /** Holds the views of one row so we don't call findViewById every time a row is reused. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mTextCourse;
        final TextView mTextTitle;
        int mId;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextCourse = itemView.findViewById(R.id.text_course);
            mTextTitle = itemView.findViewById(R.id.text_title);

            itemView.setOnClickListener(v -> {
                Intent intent = new Intent(v.getContext(), NoteActivity.class);
                intent.putExtra(NoteActivity.NOTE_ID, mId);
                v.getContext().startActivity(intent);
            });
        }
    }

    /** Tells ListAdapter how to compare two lists of notes. */
    private static final DiffUtil.ItemCallback<NoteInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<NoteInfo>() {
                @Override
                public boolean areItemsTheSame(@NonNull NoteInfo oldNote, @NonNull NoteInfo newNote) {
                    return oldNote.getId() == newNote.getId();   // same database row?
                }

                @Override
                public boolean areContentsTheSame(@NonNull NoteInfo oldNote, @NonNull NoteInfo newNote) {
                    return oldNote.equals(newNote);              // same values?
                }
            };
}