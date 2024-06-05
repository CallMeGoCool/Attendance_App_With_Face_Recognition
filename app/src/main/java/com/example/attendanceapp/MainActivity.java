package com.example.attendanceapp;

import static androidx.core.app.PendingIntentCompat.getActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.Toolbar;


import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton fab;
    RecyclerView recyclerView;
    ClassAdapter classAdapter;
    RecyclerView.LayoutManager layoutManager;
    ArrayList<ClassItem> classItems = new ArrayList<>();
    Toolbar toolbar;
    DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DbHelper(this);

        fab = findViewById(R.id.fab_main);
        fab.setOnClickListener(v -> showAddClassDialog());

        loadData();

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        classAdapter = new ClassAdapter(this, classItems);
        recyclerView.setAdapter(classAdapter);
        classAdapter.setOnItemClickListener(position -> gotoItemActivity(position));

        setToolbar();

        SharedPreferences prefs = getSharedPreferences("MaterialTapTargetPrompt", MODE_PRIVATE);
        boolean firstStart = prefs.getBoolean("prompt_shown", false);

        if (!firstStart) {
            new MaterialTapTargetPrompt.Builder(MainActivity.this)
                    .setTarget(findViewById(R.id.fab_main))
                    .setPrimaryText("Add a class")
                    .setSecondaryText("Tap this button to add a new class")
                    .show();

            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("prompt_shown", true);
            editor.apply();
        }
    }

    private void loadData() {
        Cursor cursor = dbHelper.getClassTable();

        classItems.clear();
        while (cursor.moveToNext()) {
            int id = cursor.getInt(cursor.getColumnIndex(DbHelper.C_ID));
            String className = cursor.getString(cursor.getColumnIndex(DbHelper.CLASS_NAME_KEY));
            String subjectName = cursor.getString(cursor.getColumnIndex(DbHelper.SUBJECT_NAME_KEY));

            classItems.add(new ClassItem(id, className, subjectName));
        }
        cursor.close(); // Ensure cursor is closed to avoid memory leaks
    }

    private void setToolbar() {
        toolbar = findViewById(R.id.toolbar);
        TextView title = toolbar.findViewById(R.id.title_toolbar);
        TextView subTitle = toolbar.findViewById(R.id.subtitle_toolbar);
        ImageButton back = toolbar.findViewById(R.id.back);
        ImageButton save = toolbar.findViewById(R.id.save);
        ImageButton camera = toolbar.findViewById(R.id.camera);

        title.setText("Attendance App");
        subTitle.setVisibility(View.GONE);
        back.setVisibility(View.INVISIBLE);
        save.setVisibility(View.INVISIBLE);
        camera.setVisibility(View.GONE);
    }

    private void gotoItemActivity(int position) {
        Intent intent = new Intent(this, StudentActivity.class);

        intent.putExtra("className", classItems.get(position).getClassName());
        intent.putExtra("subjectName", classItems.get(position).getSubjectName());
        intent.putExtra("position", position);
        intent.putExtra("cid", classItems.get(position).getCid());
        startActivity(intent);
    }

    private void showAddClassDialog() {
        MyDialog dialog = new MyDialog();
        dialog.setListener((className, subjectName) -> addClass(className, subjectName));
        dialog.show(getSupportFragmentManager(), MyDialog.CLASS_ADD_DIALOG);
    }

    private void addClass(String className, String subjectName) {
        long cid = dbHelper.addClass(className, subjectName);
        if (cid == -1) {
            Toast.makeText(this, "Error adding class", Toast.LENGTH_SHORT).show();
        } else {
            ClassItem classItem = new ClassItem(cid, className, subjectName);
            classItems.add(classItem);
            classAdapter.notifyDataSetChanged();

            // Add the onboarding prompt for the newly added class box
            recyclerView.post(() -> {
                // Find the position of the newly added class item
                int position = classItems.indexOf(classItem);

                // Find the corresponding view in the RecyclerView
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);

                if (viewHolder != null) {
                    // Show the MaterialTapTargetPrompt
                    new MaterialTapTargetPrompt.Builder(MainActivity.this)
                            .setTarget(viewHolder.itemView)
                            .setPrimaryText("Manage your class")
                            .setSecondaryText("Tap to enter class, hold for class options")
                            .show();
                }
            });
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case 0:
                showUpdateClassDialog(item.getGroupId());
                return true;
            case 1:
                deleteClass(item.getGroupId());
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void showUpdateClassDialog(int position) {
        MyDialog dialog = new MyDialog();
        dialog.setListener((className, subjectName) -> updateClass(position, className, subjectName));
        dialog.show(getSupportFragmentManager(), MyDialog.CLASS_UPDATE_DIALOG);
    }

    private void updateClass(int position, String className, String subjectName) {
        long result = dbHelper.updateClass(classItems.get(position).getCid(), className, subjectName);
        if (result == -1) {
            Toast.makeText(this, "Error updating class", Toast.LENGTH_SHORT).show();
        } else {
            classItems.get(position).setClassName(className);
            classItems.get(position).setSubjectName(subjectName);
            classAdapter.notifyItemChanged(position);
        }
    }

    private void deleteClass(int position) {
        dbHelper.deleteClass(classItems.get(position).getCid());
        classItems.remove(position);
        classAdapter.notifyItemRemoved(position);
    }
}