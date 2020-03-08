package edu.buffalo.cse.cse486586.groupmessenger1;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import android.database.MatrixCursor;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.*;



/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 * @author manju_karthik_shivashankar
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        String fname = (String) values.get("key");
        String cVal = (String) values.get("value");
        try{
            FileOutputStream opStream = getContext().openFileOutput(fname, Context.MODE_PRIVATE);
            opStream.write(cVal.getBytes());
            opStream.close();
        }catch (Exception e) {
            Log.e(TAG, "Unable to write to file");
        }
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        String[] cols = {"key","value"};
        MatrixCursor  curs = new MatrixCursor(cols);
        try{
            FileInputStream ipStream = getContext().openFileInput(selection);
            BufferedReader rd = new BufferedReader(new InputStreamReader(ipStream));
            curs.addRow(new String[] {selection,rd.readLine()});

        }
        catch(FileNotFoundException e){}
        catch(IOException e){}
        return curs;
    }
}
