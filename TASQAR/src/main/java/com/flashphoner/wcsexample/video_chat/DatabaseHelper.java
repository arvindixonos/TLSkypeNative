package com.flashphoner.wcsexample.video_chat;

import android.app.ActionBar;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper
{

    private static final String DATABASE_NAME = "storage.db";
    private static final String TABLE_NAME = "storage_db";
    private static final String COL1 = "ID";
    private static final String COL2 = "FILE_NAME";
    private static final String COL3 = "FILE_PATH";
    private static final String COL4 = "STATE";
    private static final String COL5 = "DATE";

    public DatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                " FILE_NAME TEXT, FILE_PATH TEXT, STATE TEXT, DATE TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addData(String fileName, String filePath, String state, String timeStamp)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, fileName);
        contentValues.put(COL3, filePath);
        contentValues.put(COL4, state);
        contentValues.put(COL5, timeStamp);

        long result  = db.insert(TABLE_NAME, null, contentValues);

        if(result == -1)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public Cursor showData()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        return data;
    }

    public boolean updateData(String id, String name, String email, String tvShow, String timeStamp)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, id);
        contentValues.put(COL2, name);
        contentValues.put(COL3, email);
        contentValues.put(COL4, tvShow);
        contentValues.put(COL5, timeStamp);
        db.update(TABLE_NAME, contentValues, "ID = ?", new String[] {id});
        return true;
    }

    public Integer deleteData(String id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "ID = ?", new String[] {id});
    }

}

class LoginDatabaseHelper extends SQLiteOpenHelper
{

    private static final String DATABASE_NAME = "userprofile.db";
    private static final String TABLE_NAME = "userprofile_db";
    private static final String COL1 = "ID";
    private static final String COL2 = "USERID";
    private static final String COL3 = "NAME";
    private static final String COL4 = "EMAIL";
    private static final String COL5 = "NUMBER";
    private static final String COL6 = "PASSWORD";
    private static final String COL7 = "HIERARCHY";
    private static final String COL8 = "ROLE";

    public LoginDatabaseHelper(Context context)
    {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db)
    {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (ID INTEGER /*PRIMARY KEY AUTOINCREMENT*/, " +
                " USERID TEXT, NAME TEXT, EMAIL TEXT, NUMBER TEXT, PASSWORD TEXT, HIERARCHY TEXT, ROLE TEXT)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
        db.execSQL("DROP IF TABLE EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public boolean addData(String USERID, String name, String email, String number, String password, String hierarchy, String role)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL1, "0");
        contentValues.put(COL2, USERID);
        contentValues.put(COL3, name);
        contentValues.put(COL4, email);
        contentValues.put(COL5, number);
        contentValues.put(COL6, password);
        contentValues.put(COL7, hierarchy);
        contentValues.put(COL8, role);

        long result  = db.insert(TABLE_NAME, null, contentValues);

        if(result == -1)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    public Cursor showData()
    {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor data = db.rawQuery("SELECT * FROM " + TABLE_NAME, null);
        return data;
    }

    public boolean updateData(String USERID, String name, String email, String number, String password, String hierarchy, String role)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL2, USERID);
        contentValues.put(COL3, name);
        contentValues.put(COL4, email);
        contentValues.put(COL5, number);
        contentValues.put(COL6, password);
        contentValues.put(COL7, hierarchy);
        contentValues.put(COL8, role);
        db.update(TABLE_NAME, contentValues, "ID = ?", new String[] {"0"});
        return true;
    }

    public Integer deleteData(String id)
    {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "ID = ?", new String[] {id});
    }

}