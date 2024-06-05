package com.example.attendanceapp;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class SheetListActivity extends AppCompatActivity {
    private ListView sheetList;
    private ArrayAdapter adapter;
    private ArrayList<String> listItems = new ArrayList();
    private long cid;
    Toolbar toolbar;
    private TextView subTitle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sheet_list);

        cid = getIntent().getLongExtra("cid", -1);
        Log.i("1234567890", "onCreate: " + cid);
        loadListItems();

        setToolbar();

        sheetList = findViewById(R.id.sheetList);
        adapter = new ArrayAdapter(this, R.layout.sheet_list, R.id.date_list_item, listItems);

        sheetList.setAdapter(adapter);

        sheetList.setOnItemClickListener((parent, view, position, id) -> openSheetActivity(position));

    }

    private void setToolbar() {
        toolbar = findViewById(R.id.toolbar);
        TextView title = toolbar.findViewById(R.id.title_toolbar);
        subTitle = toolbar.findViewById(R.id.subtitle_toolbar);
        ImageButton back = toolbar.findViewById(R.id.back);
        ImageButton save = toolbar.findViewById(R.id.save);
        ImageButton camera = toolbar.findViewById(R.id.camera);

        title.setText("Select Attendance Month");
        subTitle.setText("Choose a month to view attendance records");
        save.setVisibility(View.GONE);
        camera.setVisibility(View.GONE);

        back.setOnClickListener(v -> onBackPressed());

    }

    private void openSheetActivity(int position) {
        long[] idArray = getIntent().getLongArrayExtra("idArray");
        int[] rollArray = getIntent().getIntArrayExtra("rollArray");
        String[] nameArray = getIntent().getStringArrayExtra("nameArray");
        Intent intent = new Intent(this, SheetActivity.class);
        intent.putExtra("idArray", idArray);
        intent.putExtra("rollArray", rollArray);
        intent.putExtra("nameArray", nameArray);
        intent.putExtra("month", listItems.get(position));

        startActivity(intent);
    }

    private void loadListItems() {
        Cursor cursor = new DbHelper(this).getDistinctMonths(cid);

        SimpleDateFormat originalFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.ENGLISH);
        SimpleDateFormat targetFormat = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);

        while (cursor.moveToNext()) {
            String date = cursor.getString(cursor.getColumnIndex(DbHelper.DATE_KEY));
            try {
                Date dateObj = originalFormat.parse(date);
                String formattedDate = targetFormat.format(dateObj);
                listItems.add(formattedDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}