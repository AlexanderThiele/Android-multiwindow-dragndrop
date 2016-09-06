package de.at.dragndropexample;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Created by Alex (kontakt@alexander-thiele.de)
 */
public class ImageProvider extends ContentProvider {
    @Override
    public boolean onCreate() {
        Log.e("onCreate", " ONCREATE FALSE");
        return true;
    }

    @Nullable
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        Log.e("QUERY", "QUERY");
        return null;
    }

    @Nullable
    @Override
    public String getType(Uri uri) {
        Log.e("type", " " + uri);
        return "image/png";
    }

    @Nullable
    @Override
    public String[] getStreamTypes(Uri uri, String mimeTypeFilter) {
        Log.e("streamType", " " + uri + " " + mimeTypeFilter);
        return new String[]{"image/png"};
    }

    @Nullable
    @Override
    public Uri insert(Uri uri, ContentValues values) {
        Log.e("INSERT", "INSERT");
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        Log.e("DELTE", "DELETE");
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        Log.e("UPDATE", "Update");
        return 0;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File folder = new File(getContext().getFilesDir(), "/images");
        folder.mkdirs();
        File file = new File(folder, "/img.png");

        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
    }


}
