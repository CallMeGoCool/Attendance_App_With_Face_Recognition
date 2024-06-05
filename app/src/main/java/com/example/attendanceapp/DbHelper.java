package com.example.attendanceapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

class DbHelper extends SQLiteOpenHelper{

    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "Attendance.db";
    private static final String TAG = "DbHelper";

    // Class table
    private static final String CLASS_TABLE_NAME = "CLASS_TABLE";
    public static final String C_ID = "_CID";
    public static final String CLASS_NAME_KEY = "CLASS_NAME";
    public static final String SUBJECT_NAME_KEY = "SUBJECT_NAME";

    private static final String CREATE_CLASS_TABLE =
            "CREATE TABLE " + CLASS_TABLE_NAME + " (" +
                    C_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    CLASS_NAME_KEY + " TEXT NOT NULL, " +
                    SUBJECT_NAME_KEY + " TEXT NOT NULL," +
                    "UNIQUE (" + CLASS_NAME_KEY + ", " + SUBJECT_NAME_KEY + ")" +
                    ")";

    private static final String DROP_CLASS_TABLE = "DROP TABLE IF EXISTS " + CLASS_TABLE_NAME;
    private static final String SELECT_CLASS_TABLE = "SELECT * FROM " + CLASS_TABLE_NAME;

    // Student table
    private static final String STUDENT_TABLE_NAME = "STUDENT_TABLE";
    public static final String S_ID = "_SID";
    public static final String STUDENT_NAME_KEY = "STUDENT_NAME";
    public static final String STUDENT_ROLL_KEY = "ROLL";

    private static final String CREATE_STUDENT_TABLE =
            "CREATE TABLE " + STUDENT_TABLE_NAME +
                    " (" +
                    S_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    C_ID + " INTEGER NOT NULL, " +
                    STUDENT_NAME_KEY + " TEXT NOT NULL, " +
                    STUDENT_ROLL_KEY + " INTEGER, " +
                    "FOREIGN KEY (" + C_ID + ") REFERENCES " + CLASS_TABLE_NAME + "(" + C_ID + ")" +
                    ")";

    private static final String DROP_STUDENT_TABLE = "DROP TABLE IF EXISTS " + STUDENT_TABLE_NAME;
    private static final String SELECT_STUDENT_TABLE = "SELECT * FROM " + STUDENT_TABLE_NAME;

    // Status table
    private static final String STATUS_TABLE_NAME = "STATUS_TABLE";
    public static final String STATUS_ID = "_STATUS_ID";
    public static final String DATE_KEY = "_STATUS_DATE";
    public static final String STATUS_KEY = "STATUS";

    private static final String CREATE_STATUS_TABLE =
            "CREATE TABLE " + STATUS_TABLE_NAME +
                    " (" +
                    STATUS_ID + " INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    S_ID + " INTEGER NOT NULL, " +
                    C_ID + " INTEGER NOT NULL, " +
                    DATE_KEY + " DATE NOT NULL, " +
                    STATUS_KEY + " TEXT NOT NULL, " +
                    "UNIQUE (" + S_ID + "," + DATE_KEY + ")," +
                    " FOREIGN KEY (" + S_ID + ") REFERENCES " + STUDENT_TABLE_NAME + "(" + S_ID + ")," +
                    " FOREIGN KEY (" + C_ID + ") REFERENCES " + CLASS_TABLE_NAME + "(" + C_ID + ")" +
                    ")";

    private static final String DROP_STATUS_TABLE = "DROP TABLE IF EXISTS " + STATUS_TABLE_NAME;
    private static final String SELECT_STATUS_TABLE = "SELECT * FROM " + STATUS_TABLE_NAME;

    public DbHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.execSQL(CREATE_CLASS_TABLE);
            db.execSQL(CREATE_STUDENT_TABLE);
            db.execSQL(CREATE_STATUS_TABLE);
        } catch (SQLException e) {
            Log.e(TAG, "Error creating tables", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        try {
            db.execSQL(DROP_CLASS_TABLE);
            db.execSQL(DROP_STUDENT_TABLE);
            db.execSQL(DROP_STATUS_TABLE);
            onCreate(db);
        } catch (SQLException e) {
            Log.e(TAG, "Error upgrading tables", e);
        }
    }

    long addClass(String className, String subjectName) {
        long result = -1;
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CLASS_NAME_KEY, className);
        values.put(SUBJECT_NAME_KEY, subjectName);

        try {
            result = database.insert(CLASS_TABLE_NAME, null, values);
        } catch (SQLException e) {
            Log.e(TAG, "Error adding class", e);
        } finally {
            database.close();
        }

        return result;
    }

    Cursor getClassTable() {
        Cursor cursor = null;
        SQLiteDatabase database = this.getReadableDatabase();

        try {
            cursor = database.rawQuery(SELECT_CLASS_TABLE, null);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting class table", e);
        }

        return cursor;
    }

    int deleteClass(long cid) {
        int rowsDeleted = 0;
        SQLiteDatabase database = this.getWritableDatabase();

        try {
            rowsDeleted = database.delete(CLASS_TABLE_NAME, C_ID + "=?", new String[]{String.valueOf(cid)});
        } catch (SQLException e) {
            Log.e(TAG, "Error deleting class", e);
        } finally {
            database.close();
        }

        return rowsDeleted;
    }

    long updateClass(long cid, String className, String subjectName) {
        long result = -1;
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(CLASS_NAME_KEY, className);
        values.put(SUBJECT_NAME_KEY, subjectName);

        try {
            result = database.update(CLASS_TABLE_NAME, values, C_ID + "=?", new String[]{String.valueOf(cid)});
        } catch (SQLException e) {
            Log.e(TAG, "Error updating class", e);
        } finally {
            database.close();
        }

        return result;
    }

    long addStudent(long cid, int roll, String name) {
        long result = -1;
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(C_ID, cid);
        values.put(STUDENT_ROLL_KEY, roll);
        values.put(STUDENT_NAME_KEY, name);

        try {
            result = database.insert(STUDENT_TABLE_NAME, null, values);
        } catch (SQLException e) {
            Log.e(TAG, "Error adding student", e);
        } finally {
            database.close();
        }

        return result;
    }

    Cursor getStudentTable(long cid) {
        Cursor cursor = null;
        SQLiteDatabase database = this.getReadableDatabase();

        try {
            cursor = database.query(STUDENT_TABLE_NAME, null, C_ID + "=?", new String[]{String.valueOf(cid)}, null, null, STUDENT_ROLL_KEY);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting student table", e);
        }

        return cursor;
    }

    int deleteStudent(long sid) {
        int rowsDeleted = 0;
        SQLiteDatabase database = this.getWritableDatabase();

        try {
            rowsDeleted = database.delete(STUDENT_TABLE_NAME, S_ID + "=?", new String[]{String.valueOf(sid)});
        } catch (SQLException e) {
            Log.e(TAG, "Error deleting student", e);
        } finally {
            database.close();
        }

        return rowsDeleted;
    }

    long updateStudent(long sid, String name) {
        long result = -1;
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STUDENT_NAME_KEY, name);

        try {
            result = database.update(STUDENT_TABLE_NAME, values, S_ID + "=?", new String[]{String.valueOf(sid)});
        } catch (SQLException e) {
            Log.e(TAG, "Error updating student", e);
        } finally {
            database.close();
        }

        return result;
    }

    long addStatus(long sid, long cid, String date, String status) {
        long result = -1;
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(S_ID, sid);
        values.put(C_ID, cid);
        values.put(DATE_KEY, date);
        values.put(STATUS_KEY, status);

        try {
            result = database.insert(STATUS_TABLE_NAME, null, values);
        } catch (SQLException e) {
            Log.e(TAG, "Error adding status", e);
        } finally {
            database.close();
        }

        return result;
    }

    long updateStatus(long sid, String date, String status) {
        long result = -1;
        SQLiteDatabase database = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(STATUS_KEY, status);
        String whereClause = DATE_KEY + "='" + date + "' AND " + S_ID + "=" + sid;

        try {
            result = database.update(STATUS_TABLE_NAME, values, whereClause, null);
        } catch (SQLException e) {
            Log.e(TAG, "Error updating status", e);
        } finally {
            database.close();
        }

        return result;
    }

    String getStatus(long sid, String date) {
        String status = null;
        SQLiteDatabase database = this.getReadableDatabase();
        String whereClause = DATE_KEY + "='" + date + "' AND " + S_ID + "=" + sid;
        Cursor cursor = null;

        try {
            cursor = database.query(STATUS_TABLE_NAME, null, whereClause, null, null, null, null);
            if (cursor.moveToFirst()) {
                status = cursor.getString(cursor.getColumnIndex(STATUS_KEY));
            }
        } catch (SQLException e) {
            Log.e(TAG, "Error getting status", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
            database.close();
        }

        return status;
    }

    Cursor getDistinctMonths(long cid) {
        Cursor cursor = null;
        SQLiteDatabase database = this.getReadableDatabase();

        try {
            cursor = database.query(STATUS_TABLE_NAME, new String[]{DATE_KEY}, C_ID + "=" + cid, null, "substr(" + DATE_KEY + ",4,7)", null, null);
        } catch (SQLException e) {
            Log.e(TAG, "Error getting distinct months", e);
        }

        return cursor;
    }
}
