package abmo.postur;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TabHost;
import android.widget.Toast;

import org.apache.http.HttpConnection;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends ActionBarActivity implements View.OnClickListener, View.OnLongClickListener {

    private static final int RESULT_LOAD_IMAGE = 1;
    private static final int CAM_REQUEST = 0;
    private static final String SERVER_ADDRESS = "http://abmo.site88.net/";

    ImageView imageToUpload;
    ImageView imageToDownload;
    EditText etImageDescription;
    Button bUploadImage;
    Button bDownloadImage;
    List<String> imagesOnServer = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        TabHost th = (TabHost)findViewById(R.id.tabhost);
        th.setup();
        TabHost.TabSpec specs = th.newTabSpec("Upload");
        specs.setContent(R.id.upload);
        specs.setIndicator("Upload");
        th.addTab(specs);
        specs = th.newTabSpec("Events");
        specs.setContent(R.id.events);
        specs.setIndicator("Events");
        th.addTab(specs);
        specs = th.newTabSpec("Attend");
        specs.setContent(R.id.attend);
        specs.setIndicator("Attend");
        th.addTab(specs);

        imageToUpload = (ImageView) findViewById(R.id.imageToUpload);
        imageToDownload = (ImageView) this.findViewById(R.id.imageToDownload);
        etImageDescription = (EditText) findViewById(R.id.etImageDescription);
        bUploadImage = (Button) findViewById(R.id.bUploadImage);
        bDownloadImage = (Button) findViewById(R.id.bDownloadImage);


        imageToUpload.setOnClickListener(this);
        imageToUpload.setOnLongClickListener(this);
        bUploadImage.setOnClickListener(this);
        bDownloadImage.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.imageToUpload:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
                break;
            case R.id.bUploadImage:
                Bitmap image = ((BitmapDrawable) imageToUpload.getDrawable()).getBitmap();
                new UploadImage(image, etImageDescription.getText().toString()).execute();
                break;

            case R.id.bDownloadImage:
                //for (String one_image : imagesOnServer) {
                    new DownloadImage("anime").execute();
                //}
                break;

        }
    }

    @Override
    public boolean onLongClick(View v) {
        switch(v.getId()) {
            case R.id.imageToUpload:
                //Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                //File file = getFile();
                //cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
                //startActivityForResult(cameraIntent, CAM_REQUEST);
                Intent cameraIntent = new Intent();
                cameraIntent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, CAM_REQUEST);
                break;

        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            try {
                Bitmap photoBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                int bitmapWidth = photoBitmap.getWidth();
                int bitmapHeight = photoBitmap.getHeight();
                if (bitmapWidth > 4096)
                    bitmapWidth = 4096;
                if (bitmapHeight > 4096)
                    bitmapHeight = 4096;
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(photoBitmap, bitmapWidth,
                        bitmapHeight, true);
                imageToUpload.setImageBitmap(scaledBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),selectedImage);
            //imageToUpload.setImageURI(selectedImage);
            //Bundle extras = data.getExtras();
            //Bitmap photoCapturedBitmap = (Bitmap)extras.get("data");

            //Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
        }
        else if (requestCode == CAM_REQUEST && resultCode == RESULT_OK && data != null) {
            //String path = "sdcard/camera_app/cam_image.jpg";
            //imageToUpload.setImageDrawable(Drawable.createFromPath(path));
            Bundle extras = data.getExtras();
            Bitmap photoCapturedBitmap = (Bitmap)extras.get("data");
            imageToUpload.setImageBitmap(photoCapturedBitmap);


        }

    }


    private class UploadImage extends AsyncTask<Void, Void, Void> {
        Bitmap image;
        String name;

        public UploadImage(Bitmap image, String name) {
            this.image = image;
            this.name = name;
        }

        @Override
        protected Void doInBackground(Void... params) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            image.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
            String encodedImage = Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT);

            ArrayList<NameValuePair> dataToSend = new ArrayList<>();
            dataToSend.add(new BasicNameValuePair("image", encodedImage));
            dataToSend.add(new BasicNameValuePair("name", name));

            HttpParams httpRequestParams = getHttpRequestParams();
            HttpClient client = new DefaultHttpClient(httpRequestParams);
            HttpPost post = new HttpPost(SERVER_ADDRESS + "SavePicture.php");

            try {
                post.setEntity(new UrlEncodedFormEntity(dataToSend));
                client.execute(post);
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            imagesOnServer.add(name);
            Toast.makeText(getApplicationContext(), "Image Uploaded", Toast.LENGTH_SHORT).show();
        }
    }

    private class DownloadImage extends AsyncTask<Void, Void, Bitmap> {
        private    String name;
        public DownloadImage(String name) {
            this.name = name;
        }

        @Override
        protected Bitmap doInBackground(Void... params) {
            String url = SERVER_ADDRESS + "pictures/ty.JPG"; // figure out how to retrieve all images from the server

            try {
                URLConnection connection = new URL(url).openConnection();
                connection.setConnectTimeout(1000 * 30);
                connection.setReadTimeout(1000 * 30);
                return BitmapFactory.decodeStream((InputStream) connection.getContent(), null, null);
            } catch(Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap != null) {
                int bitmapWidth = bitmap.getWidth();
                int bitmapHeight = bitmap.getHeight();
                if (bitmapWidth > 4096)
                    bitmapWidth = 4096;
                if (bitmapHeight > 4096)
                   bitmapHeight = 4096;
                Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, bitmapWidth, bitmapHeight, true);
                imageToDownload.setImageBitmap(scaledBitmap);
                //imageToDownload.setImageBitmap(bitmap);

                //imageToDownload.setImageBitmap(bitmap);
                Toast.makeText(getApplicationContext(), "fuck hackathon", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getApplicationContext(), "Can't download that shit", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private HttpParams getHttpRequestParams() {
        HttpParams httpRequestParams = new BasicHttpParams();
        HttpConnectionParams.setConnectionTimeout(httpRequestParams, 1000 * 30);
        HttpConnectionParams.setSoTimeout(httpRequestParams, 1000 * 30);
        return httpRequestParams;
    }

}
