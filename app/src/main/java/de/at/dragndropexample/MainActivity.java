package de.at.dragndropexample;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.ContentProvider;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;

public class MainActivity extends AppCompatActivity {

    private final int RC_IMAGE = 12;

    private EditText mPlainTextField;

    private ImageView mImagePreview;

    private Uri mLocalUri;
    private Bitmap mPickedBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button mPlainText = (Button) findViewById(R.id.plainText);
        mPlainTextField = (EditText) findViewById(R.id.plainTextText);

        Button mImage = (Button) findViewById(R.id.image);
        mImagePreview = (ImageView) findViewById(R.id.imagePreview);

        mPlainText.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ClipData dragData = ClipData.newPlainText(v.getContext().getString(R.string.app_name), mPlainTextField.getText().toString());
                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(v);

                mPlainTextField.startDragAndDrop(dragData, myShadow, null, View.DRAG_FLAG_GLOBAL);
                return true;
            }
        });

        mImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                    return;
                }

                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(Intent.createChooser(intent, "Select Photo"), RC_IMAGE);
            }
        });

        mImage.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(mLocalUri == null){
                    Toast.makeText(MainActivity.this, "Select Picture first", Toast.LENGTH_SHORT).show();
                    return true;
                }

                ClipData dragData = ClipData.newUri(getContentResolver(), "Image Uri", mLocalUri);

                // Instantiates the drag shadow builder.
                View.DragShadowBuilder myShadow = new MyDragShadowBuilder(v, mPickedBitmap);

                // Starts the drag
                v.startDragAndDrop(dragData,  // the data to be dragged
                        myShadow,  // the drag shadow builder
                        null,      // no need to use local data
                        View.DRAG_FLAG_GLOBAL |View.DRAG_FLAG_GLOBAL_URI_READ
                );
                return true;
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == RC_IMAGE) {
                if (data == null) {
                    Toast.makeText(this, "Error Data null", Toast.LENGTH_SHORT).show();
                    return;
                }

                Log.e("file uri", data.getData().toString());
                Uri mImageUrl = data.getData();

                try {
                    mPickedBitmap = decodeUri(mImageUrl);
                    mImagePreview.setImageBitmap(mPickedBitmap);

                    FileOutputStream fos = null;
                    try {
                        File folder = new File(getApplicationContext().getFilesDir() + "/images/");
                        folder.mkdirs();
                        File file = new File(folder, "img.png");

                        fos = new FileOutputStream(file);

                        byte[] buffer = new byte[4096];
                        int n = 0;
                        InputStream is = getContentResolver().openInputStream(mImageUrl);
                        while (-1 != (n = is.read(buffer))) {
                            fos.write(buffer, 0, n);
                        }

                        mLocalUri =  Uri.parse("content://de.at.dragndropexample.provider/img.png");
                        Log.e("fileProvider uri", mLocalUri.toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (fos != null) {
                                fos.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    mPickedBitmap = null;
                    Toast.makeText(this, "Error decode Image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 1000;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }

    private static class MyDragShadowBuilder extends View.DragShadowBuilder {

        private static Drawable shadowDrawable;
        private static Bitmap shadowBitmap;
        private float density;

        private int width;
        private int height;

        MyDragShadowBuilder(View view) {
            this(view, null);
        }

        MyDragShadowBuilder(View view, Bitmap bitmap){
            super(view);

            density = view.getContext().getResources().getDisplayMetrics().density;

            if(bitmap != null) {
                shadowBitmap = bitmap;
            } else {
                shadowDrawable = view.getContext().getResources().getDrawable(R.mipmap.ic_launcher, null);
            }

        }

        @Override
        public void onProvideShadowMetrics(Point size, Point touch) {
            // Defines local variables
            width = (int) (density * 100);
            height = (int) (density * 100);
            if(shadowDrawable != null) {
                shadowDrawable.setBounds(0, 0, width, height);
            }else if(shadowBitmap != null){
                //shadowBitmap.setHeight(height);
                //shadowBitmap.setWidth(width);
            }
            size.set(width, height);
            touch.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            if(shadowDrawable != null) {
                shadowDrawable.draw(canvas);
            } else if(shadowBitmap != null) {
                canvas.drawBitmap(shadowBitmap, new Rect(0,0,shadowBitmap.getWidth(), shadowBitmap.getHeight()), new Rect(0,0,width,height), null);
            }
        }
    }
}
