package com.example.attendanceapp;

import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.widget.VideoView;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.attendanceapp.databinding.ActivityStudentBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt;

public class StudentActivity extends AppCompatActivity {

    Toolbar toolbar;
    private String className;
    private String subjectName;
    private int position;
    private RecyclerView recyclerView;
    private StudentAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private ArrayList<StudentItem> studentItems = new ArrayList<>();
    private DbHelper dbHelper;
    private long cid;
    private MyCalendar calendar;
    private TextView subtitle;
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 200;
    private static final int REQUEST_VIDEO_CAPTURE = 101;
    private String currentPhotoPath;
    private String currentVideoPath;
    private static final int REQUEST_PICK_IMAGE = 2;
    private static final int REQUEST_PICK_VIDEO = 102;
    private int currentRollNumber;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student);

        calendar = new MyCalendar();
        dbHelper = new DbHelper(this);
        Intent intent = getIntent();
        className = intent.getStringExtra("className");
        subjectName = intent.getStringExtra("subjectName");
        position = intent.getIntExtra("position", -1);
        cid=intent.getLongExtra("cid",-1);


        setToolbar();
        loadData();

        recyclerView = findViewById(R.id.student_recycler);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter = new StudentAdapter(this, studentItems);
        recyclerView.setAdapter(adapter);
        adapter.setOnItemClickListener(position -> changeStatus(position));
        loadStatusData();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // The image is saved to the file specified by currentPhotoPath, you can use this path to access the image
            // For example, you can display the image in an ImageView
            ImageView imageView = findViewById(R.id.my_image_view);
            Bitmap myBitmap = BitmapFactory.decodeFile(currentPhotoPath);
            imageView.setImageBitmap(myBitmap);

            // Check if the image file exists in the cache directory
            File imageFile = new File(currentPhotoPath);
            if (imageFile.exists()) {
                // Display an alert that the image has been uploaded
                Toast.makeText(this, "Image has been uploaded.", Toast.LENGTH_SHORT).show();

                // Create an instance of FaceRecognition
                FacialRecognition facialRecognition = new FacialRecognition(this, className, subjectName,adapter,studentItems);

                // Call the compareWithStoredImages method
                facialRecognition.compareImageWithStoredImages(currentPhotoPath);

            } else {
                // Display an alert that there was an error
                Toast.makeText(this, "Error: Image was not uploaded.", Toast.LENGTH_SHORT).show();
            }


        } else if (requestCode == REQUEST_VIDEO_CAPTURE && resultCode == RESULT_OK) {
            // The video is saved to the file specified by currentVideoPath, you can use this path to access the video
            // For example, you can play the video in a VideoView
            VideoView videoView = findViewById(R.id.my_video_view);
            videoView.setVideoURI(Uri.parse(currentVideoPath));
            videoView.start();

            // Check if the video file exists in the cache directory
            File videoFile = new File(currentVideoPath);
            if (videoFile.exists()) {
                // Display an alert that the video has been uploaded
                Toast.makeText(this, "Video has been uploaded.", Toast.LENGTH_SHORT).show();

                // Create an instance of FaceRecognition
                FacialRecognition facialRecognition = new FacialRecognition(this, className, subjectName,adapter,studentItems);

                // Call the compareWithStoredImages method
                facialRecognition.compareVideoWithStoredImages(currentVideoPath);

            } else {
                // Display an alert that there was an error
                Toast.makeText(this, "Error: Video was not uploaded.", Toast.LENGTH_SHORT).show();
            }


        } else if (requestCode == REQUEST_PICK_IMAGE && resultCode == RESULT_OK) {
            // The image is picked from the gallery, you can save it to a permanent file in your application's storage
            Uri selectedImageUri = data.getData();
            try {
                File imageFile = createPermanentImageFile(); // Create a permanent file to save the image
                saveImageToGallery(selectedImageUri, imageFile); // Save the picked image to the permanent file

                // Check if the image file exists in the specified directory
                if (imageFile.exists()) {
                    // Display an alert that the image has been uploaded
                    Toast.makeText(this, "Image has been uploaded.", Toast.LENGTH_SHORT).show();
                } else {
                    // Display an alert that there was an error
                    Toast.makeText(this, "Error: Image was not uploaded.", Toast.LENGTH_SHORT).show();
                }

            } catch (IOException ex) {
                Log.e("TAG", "IOException", ex);
            }
        }
    }

    private void saveImageToGallery(Uri selectedImageUri, File imageFile) throws IOException {
        InputStream is = getContentResolver().openInputStream(selectedImageUri);
        FileOutputStream fos = new FileOutputStream(imageFile);
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = is.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }
        is.close();
        fos.close();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to use the camera", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_VIDEO_CAPTURE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                dispatchTakeVideoIntent();
            } else {
                Toast.makeText(this, "Camera permission is required to record a video", Toast.LENGTH_SHORT).show();
            }
        }
    }




    private void loadData() {
        Cursor cursor=dbHelper.getStudentTable(cid);
        Log.i("1234567890","loadData: "+cid);
        studentItems.clear();
        while (cursor.moveToNext()){
            long sid=cursor.getLong(cursor.getColumnIndex(DbHelper.S_ID));
            int roll=cursor.getInt(cursor.getColumnIndex(DbHelper.STUDENT_ROLL_KEY));
            String name=cursor.getString(cursor.getColumnIndex(DbHelper.STUDENT_NAME_KEY));
            studentItems.add(new StudentItem(sid,roll,name));
        }
        cursor.close();
    }

    private void changeStatus(int position) {
        String status = studentItems.get(position).getStatus();

        if(status.equals("P"))status="A";
        else if(status.equals("A"))status="N";
        else status="P";

        studentItems.get(position).setStatus(status);
        studentItems.get(position).setChanged(true);

        adapter.notifyItemChanged(position);
    }

    private SharedPreferences getPreferences() {
        return getSharedPreferences("MaterialTapTargetPrompt", MODE_PRIVATE);
    }


    private void setToolbar() {
        toolbar = findViewById(R.id.toolbar);
        TextView title = toolbar.findViewById(R.id.title_toolbar);
        subtitle = toolbar.findViewById(R.id.subtitle_toolbar);
        ImageButton back = toolbar.findViewById(R.id.back);
        ImageButton save = toolbar.findViewById(R.id.save);
        ImageButton camera = toolbar.findViewById(R.id.camera);

        save.setOnClickListener(v -> saveStatus());

        title.setText(className);
        subtitle.setText(subjectName+" | "+calendar.getDate());
        camera.setVisibility(View.VISIBLE);

        back.setOnClickListener(v -> onBackPressed());
        toolbar.inflateMenu(R.menu.student_menu);
        toolbar.setOnMenuItemClickListener(menuItem ->onMenuItemClick(menuItem));

        camera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(StudentActivity.this,Manifest.permission.CAMERA)
                        != PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(StudentActivity.this,new String[]{Manifest.permission.CAMERA},REQUEST_CAMERA_PERMISSION);
                }
                else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(StudentActivity.this);
                    builder.setMessage("Do you want to capture a photo or record a video?")
                            .setPositiveButton("Photo", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    try {
                                        createImageFile(); // Call the modified createImageFile method
                                        dispatchTakePictureIntent();
                                    } catch (IOException ex) {
                                        Log.e("TAG", "IOException", ex);
                                    }
                                }
                            })
                            .setNegativeButton("Video", new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    try {
                                        createVideoFile(); // Call the modified createVideoFile method
                                        dispatchTakeVideoIntent();
                                    } catch (IOException ex) {
                                        Log.e("TAG", "IOException", ex);
                                    }
                                }
                            });
                    builder.create().show();
                }
            }
        });

        SharedPreferences prefs = getPreferences();
        if (!prefs.getBoolean("prompt_shown", false)) {
            // Show the prompt
            new MaterialTapTargetPrompt.Builder(StudentActivity.this)
                    .setTarget(back)
                    .setPrimaryText("Back")
                    .setSecondaryText("Click here to go back")
                    .setIcon(R.drawable.icon_back) // Set the icon to the back icon
                    .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener()
                    {
                        @Override
                        public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state)
                        {
                            if (state == MaterialTapTargetPrompt.STATE_DISMISSED)
                            {
                                // Show the next prompt when the current prompt is dismissed
                                new MaterialTapTargetPrompt.Builder(StudentActivity.this)
                                        .setTarget(camera)
                                        .setPrimaryText("Camera")
                                        .setSecondaryText("Click here to capture a photo or record a video to mark attendance")
                                        .setIcon(R.drawable.icon_camera)
                                        .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener()
                                        {
                                            @Override
                                            public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state)
                                            {
                                                if (state == MaterialTapTargetPrompt.STATE_DISMISSED)
                                                {
                                                    // Show the next prompt when the current prompt is dismissed
                                                    new MaterialTapTargetPrompt.Builder(StudentActivity.this)
                                                            .setTarget(save)
                                                            .setPrimaryText("Save")
                                                            .setSecondaryText("Click here to save the attendance")
                                                            .setIcon(R.drawable.icon_save) // Set the icon to the save icon
                                                            .show();
                                                }
                                            }
                                        })
                                        .show();
                            }
                        }
                    })
                    .show();

            // After showing the prompt, set the shared preference to indicate that the prompt has been shown
            prefs.edit().putBoolean("prompt_shown", true).apply();
        }
    }

    public File createPermanentImageFile() throws IOException {
        // Create an image file name
        String rollNumberString = String.valueOf(currentRollNumber);
        String imageFileName = rollNumberString;

        // For images picked from the gallery, use the Pictures/ClassTrack directory
        File storageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "ClassTrack/" + className + "/" + subjectName);

        if (!storageDir.exists()) {
            if (!storageDir.mkdirs()) {
                Log.e("TAG", "Failed to create directory");
                return null;
            }
        }

        // Create a permanent file
        File image = new File(storageDir, imageFileName + ".jpg");

        // If a file with the same name already exists, delete it
        if (image.exists()) {
            boolean deleted = image.delete();
            if (!deleted) {
                Log.e("TAG", "Failed to delete existing file");
                return null;
            }
        }

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("TAG", "IOException", ex);
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    public File createImageFile() throws IOException {
        // Create an image file name
        String rollNumberString = String.valueOf(currentRollNumber);
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = className +" - "+ subjectName;

       File storageDir= getCacheDir();
            // For images picked from the gallery, use the cache directory

        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public File createVideoFile() throws IOException {
        String rollNumberString = String.valueOf(currentRollNumber);
        // Create a video file name
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String videoFileName = className +" - "+ subjectName;

        // This location works best if you want the created videos to be shared
        // between applications and persist after your app has been uninstalled.
        File storageDir= getCacheDir();

            // For videos captured by the camera, use the cache directory
        File video = File.createTempFile(
                videoFileName,  /* prefix */
                ".mp4",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentVideoPath = video.getAbsolutePath();
        return video;
    }

    private void dispatchTakeVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the video should go
            File videoFile = null;
            try {
                videoFile = createVideoFile(); // For videos captured by the camera
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("TAG", "IOException", ex);
            }
            // Continue only if the File was successfully created
            if (videoFile != null) {
                Uri videoURI = FileProvider.getUriForFile(this,
                        "com.example.android.fileprovider",
                        videoFile);
                takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoURI);
                startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
            }
        }
    }

    private void dispatchPickImageIntent(int rollNumber) {
        Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        if (pickImageIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(pickImageIntent, REQUEST_PICK_IMAGE);
            currentRollNumber = rollNumber;
        }
    }


    /*private void saveStatus() {
        for(StudentItem studentItem:studentItems){
            String status = studentItem.getStatus();
            if(status!="P")status="A";
            long value= dbHelper.addStatus(studentItem.getSid(),calendar.getDate(),status);

            if(value==-1)dbHelper.updateStatus(studentItem.getSid(),calendar.getDate(),status);
        }

    }  */

    private void saveStatus() {
        for(StudentItem studentItem:studentItems){
            if(studentItem.isChanged()) {
                String status = studentItem.getStatus();
                //if(status.equals("P")) status="A";
                //else if(status.equals("A")) status="N";
                //else status="P";

                long value= dbHelper.addStatus(studentItem.getSid(),cid,calendar.getDate(),status);

                if(value==-1) dbHelper.updateStatus(studentItem.getSid(),calendar.getDate(),status);

                studentItem.setChanged(false); // Reset the changed flag after saving
            }
        }
    }



    private void loadStatusData() {
        for(StudentItem studentItem:studentItems){
            String status = dbHelper.getStatus(studentItem.getSid(),calendar.getDate());
            if(status!=null) studentItem.setStatus(status);
            else studentItem.setStatus("");

        }
        adapter.notifyDataSetChanged();
    }



    private boolean onMenuItemClick(MenuItem menuItem) {
        if(menuItem.getItemId() == R.id.add_student){
            showAddStudentDialog();
        }
        else if(menuItem.getItemId() == R.id.show_Calendar){
            showCalendar();
        }
        else if(menuItem.getItemId() == R.id.show_attendance_sheet){
            openSheetList();
        }
        return true;
    }


    private void openSheetList() {
        long[] idArray = new long[studentItems.size()];
        String[] nameArray=new String[studentItems.size()];
        int[] rollArray = new int[studentItems.size()];

        for(int i=0;i<idArray.length;i++)
            idArray[i]=studentItems.get(i).getSid();
        for(int i=0;i<rollArray.length;i++)
            rollArray[i]=studentItems.get(i).getRoll();
        for(int i=0;i<nameArray.length;i++)
            nameArray[i]=studentItems.get(i).getName();

        Intent intent=new Intent(this,SheetListActivity.class);
        intent.putExtra("cid",cid);
        intent.putExtra("idArray",idArray);
        intent.putExtra("rollArray",rollArray);
        intent.putExtra("nameArray",nameArray);
        startActivity(intent);
    }

    private void showCalendar() {

        calendar.show(getSupportFragmentManager(),"");
        calendar.setOnCalendarOkClickListener(this::OnCalendarOkClicked);
    }

    private void OnCalendarOkClicked(int year, int month, int day) {
        calendar.setDate(year,month,day);
        subtitle.setText(subjectName+" | "+calendar.getDate());
        loadStatusData();

    }

    private void showAddStudentDialog() {
        MyDialog dialog = new MyDialog();
        dialog.show(getSupportFragmentManager(),MyDialog.STUDENT_ADD_DIALOG);
        dialog.setListener((roll, name) -> addStudent(roll, name));
    }

    private void addStudent(String roll_string, String name) {
        int roll=Integer.parseInt(roll_string);
        long sid=dbHelper.addStudent(cid,roll,name);
        StudentItem studentItem = new StudentItem(sid,roll,name);
        studentItems.add(studentItem);
        adapter.notifyDataSetChanged();

// Check if it's the first student
        if (studentItems.size() == 1) {
            // Show the MaterialTapTargetPrompt
            recyclerView.post(() -> {
                // Find the position of the newly added student item
                int position = studentItems.indexOf(studentItem);

                // Find the corresponding view in the RecyclerView
                RecyclerView.ViewHolder viewHolder = recyclerView.findViewHolderForAdapterPosition(position);

                if (viewHolder != null) {
                    new MaterialTapTargetPrompt.Builder(StudentActivity.this)
                            .setTarget(viewHolder.itemView)
                            .setPrimaryText("Mark Attendance")
                            .setSecondaryText("Tap here to mark attendances.")
                            .setPromptStateChangeListener(new MaterialTapTargetPrompt.PromptStateChangeListener() {
                                @Override
                                public void onPromptStateChanged(@NonNull MaterialTapTargetPrompt prompt, int state) {
                                    if (state == MaterialTapTargetPrompt.STATE_DISMISSED) {
                                        // Show the second prompt when the first prompt is dismissed
                                        new MaterialTapTargetPrompt.Builder(StudentActivity.this)
                                                .setTarget(viewHolder.itemView) // Set the target to the same view as the first prompt
                                                .setPrimaryText("Manage Student")
                                                .setSecondaryText("Hold to edit, delete or add image of the student")
                                                .show();
                                    }
                                }
                            })
                            .show();
                }
            });
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()){
            case 0:
                showUpdateStudentDialog(item.getGroupId());
                break;
            case 1:
                deleteStudent(item.getGroupId());
                break;
            case 2:
                dispatchPickImageIntent(studentItems.get(item.getGroupId()).getRoll());
                break;
        }
        return super.onContextItemSelected(item);
    }

    private void showUpdateStudentDialog(int position) {
        MyDialog dialog = new MyDialog(studentItems.get(position).getRoll(),studentItems.get(position).getName());
        dialog.show(getSupportFragmentManager(),MyDialog.STUDENT_UPDATE_DIALOG);
        dialog.setListener((roll_string, name) -> updateStudent(position, name));
    }

    private void updateStudent(int position, String name) {
        dbHelper.updateStudent(studentItems.get(position).getSid(),name);
        studentItems.get(position).setName(name);
        adapter.notifyItemChanged(position);
            }

    private void deleteStudent(int position) {
        dbHelper.deleteStudent(studentItems.get(position).getSid());
        studentItems.remove(position);
        adapter.notifyItemRemoved(position);
    }
}