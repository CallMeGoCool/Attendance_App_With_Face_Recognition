package com.example.attendanceapp;

import static android.app.ProgressDialog.show;

import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.net.Uri;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.Calendar;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class SheetActivity extends AppCompatActivity {

    Toolbar toolbar;
    private TextView subTitle;
    private static final int CREATE_FILE_REQUEST_CODE = 1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet);

        showTable();

        setToolbar();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                Uri uri = data.getData();
                writeDataToFile(uri);
            }
        }
    }

    private void writeDataToFile(Uri uri) {
        DbHelper dbHelper = new DbHelper(this);
        long[] idArray = getIntent().getLongArrayExtra("idArray");
        int[] rollArray = getIntent().getIntArrayExtra("rollArray");
        String[] nameArray = getIntent().getStringArrayExtra("nameArray");
        String month= getIntent().getStringExtra("month");

        int DAY_IN_MONTH = getDayInMonth(month);

        StringBuilder data = new StringBuilder();
        data.append("Roll No.,Name");
        for(int i=1;i<=DAY_IN_MONTH;i++){
            data.append(",Day "+i);
        }
        data.append(",Attendance Count\n"); // Add new column header for attendance count

        for(int i=0;i<idArray.length;i++){
            data.append(rollArray[i]).append(",").append(nameArray[i]);
            int attendanceCount = 0; // Initialize the attendance count for each student
            for(int j=1;j<=DAY_IN_MONTH;j++){
                String day=String.valueOf(j);
                if(day.length()==1) day="0"+day;
                String date=day+"."+month;
                String status=dbHelper.getStatus(idArray[i],date);
                data.append(",").append(status);
                if ("P".equals(status)) { // If the status is "P", increment the attendance count
                    attendanceCount++;
                }
            }
            data.append(",").append(attendanceCount); // Add the attendance count for each student
            data.append("\n");
        }

        try {
            getContentResolver().openOutputStream(uri).write(data.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void showTable() {
        DbHelper dbHelper = new DbHelper(this);
        TableLayout tableLayout=findViewById(R.id.tableLayout);
        long[] idArray = getIntent().getLongArrayExtra("idArray");
        int[] rollArray = getIntent().getIntArrayExtra("rollArray");
        String[] nameArray = getIntent().getStringArrayExtra("nameArray");
        String month= getIntent().getStringExtra("month");

        int DAY_IN_MONTH = getDayInMonth(month);

        int rowSize=idArray.length+1;
        TableRow[] rows=new TableRow[rowSize];
        TextView[] roll_tvs=new TextView[rowSize];
        TextView[] name_tvs=new TextView[rowSize];
        TextView [][] status_tvs=new TextView[rowSize][DAY_IN_MONTH + 1];
        TextView[] attendanceCount_tvs = new TextView[rowSize]; // New TextView array for attendance count

        for(int i=0;i<rowSize;i++){
            roll_tvs[i]=new TextView(this);
            roll_tvs[i].setTextColor(Color.BLACK);
            name_tvs[i]=new TextView(this);
            name_tvs[i].setTextColor(Color.BLACK);
            attendanceCount_tvs[i] = new TextView(this); // Initialize the new TextViews
            attendanceCount_tvs[i].setTextColor(Color.BLACK);
            for(int j=1;j<=DAY_IN_MONTH;j++){
                status_tvs[i][j]=new TextView(this);
                status_tvs[i][j].setTextColor(Color.BLACK);
            }
        }

        //header
        roll_tvs[0].setText("Roll No.");
        roll_tvs[0].setTypeface(roll_tvs[0].getTypeface(), Typeface.BOLD);
        name_tvs[0].setText("Name");
        name_tvs[0].setTypeface(name_tvs[0].getTypeface(), Typeface.BOLD);
        attendanceCount_tvs[0].setText("Attendance Count"); // Set the header for the new column
        attendanceCount_tvs[0].setTypeface(attendanceCount_tvs[0].getTypeface(), Typeface.BOLD);
        for(int i=1;i<=DAY_IN_MONTH;i++){
            status_tvs[0][i].setText(String.valueOf(i));
            status_tvs[0][i].setTypeface(status_tvs[0][i].getTypeface(), Typeface.BOLD);
        }
        for(int i=1;i<rowSize;i++){
            roll_tvs[i].setText(String.valueOf(rollArray[i-1]));
            name_tvs[i].setText(nameArray[i-1]);
            int attendanceCount = 0; // Initialize the attendance count for each student
            for(int j=1;j<=DAY_IN_MONTH;j++){
                String day=String.valueOf(j);
                if(day.length()==1) day="0"+day;
                String date=day+"."+month;
                String status=dbHelper.getStatus(idArray[i-1],date);
                status_tvs[i][j].setText(status);
                if ("P".equals(status)) { // If the status is "P", increment the attendance count
                    attendanceCount++;
                }
            }
            attendanceCount_tvs[i].setText(String.valueOf(attendanceCount)); // Set the attendance count for each student
        }

        for(int i=0;i<rowSize;i++){
            rows[i]=new TableRow(this);

            if(i%2==0)
                rows[i].setBackgroundColor(Color.parseColor("#EEEEEE"));
            else
                rows[i].setBackgroundColor(Color.parseColor("#E4E4E4"));

            roll_tvs[i].setPadding(16,16,16,16);
            name_tvs[i].setPadding(16,16,16,16);
            attendanceCount_tvs[i].setPadding(16,16,16,16); // Set padding for the new TextViews

            rows[i].addView(roll_tvs[i]);
            rows[i].addView(name_tvs[i]);

            for(int j=1;j<=DAY_IN_MONTH;j++){
                status_tvs[i][j].setPadding(16,16,16,16);
                rows[i].addView(status_tvs[i][j]);
            }
            rows[i].addView(attendanceCount_tvs[i]); // Add the new TextViews to the rows

            tableLayout.addView(rows[i]);
        }
        tableLayout.setShowDividers(TableLayout.SHOW_DIVIDER_MIDDLE);
    }

    private int getDayInMonth(String month) {
            int monthIndex=Integer.parseInt(month.substring(0,2))-1;
            int year=Integer.parseInt(month.substring(3));

            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.MONTH, monthIndex);
            calendar.set(Calendar.YEAR, year);
            return calendar.getActualMaximum(Calendar.DAY_OF_MONTH);


    }

     private void setToolbar() {
        toolbar = findViewById(R.id.toolbar);
        TextView title = toolbar.findViewById(R.id.title_toolbar);
        subTitle = toolbar.findViewById(R.id.subtitle_toolbar);
        ImageButton back = toolbar.findViewById(R.id.back);
        ImageButton save = toolbar.findViewById(R.id.save);
        ImageButton camera = toolbar.findViewById(R.id.camera);

        title.setText("Attendance Records");
        subTitle.setText("View student attendance of your class");
        camera.setVisibility(View.GONE);

        back.setOnClickListener(v -> onBackPressed());

        save.setOnClickListener(v -> createFile());

         // Show the MaterialTapTargetPrompt after the toolbar is set
         new MaterialTapTargetPrompt.Builder(SheetActivity.this)
                 .setTarget(save)
                 .setPrimaryText("Save Attendance")
                 .setSecondaryText("Tap here to download the attendance records of the month")
                 .setIcon(R.drawable.icon_save)
                 .show();
}

    private void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "Attendance_Data.csv");
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }
    }