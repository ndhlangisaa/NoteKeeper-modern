package com.ndhlangisa.pluralsight.notekeeper;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;

/**
 * Shows the courses as tiles in a grid. Tapping a tile shows its title in a Snackbar.
 */
public class CourseRecyclerAdapter extends ListAdapter<CourseInfo, CourseRecyclerAdapter.ViewHolder> {

    public CourseRecyclerAdapter() {
        super(DIFF_CALLBACK);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_course_list, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CourseInfo course = getItem(position);
        holder.mTextCourse.setText(course.getTitle());
        holder.mCourse = course;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView mTextCourse;
        CourseInfo mCourse;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextCourse = itemView.findViewById(R.id.text_course);

            itemView.setOnClickListener(v ->
                    Snackbar.make(v, mCourse.getTitle(), Snackbar.LENGTH_LONG).show());
        }
    }

    private static final DiffUtil.ItemCallback<CourseInfo> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<CourseInfo>() {
                @Override
                public boolean areItemsTheSame(@NonNull CourseInfo oldCourse, @NonNull CourseInfo newCourse) {
                    return oldCourse.getCourseId().equals(newCourse.getCourseId());
                }

                @Override
                public boolean areContentsTheSame(@NonNull CourseInfo oldCourse, @NonNull CourseInfo newCourse) {
                    return oldCourse.getTitle().equals(newCourse.getTitle());
                }
            };
}