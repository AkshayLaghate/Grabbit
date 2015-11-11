package com.indcoders.ytgrabber;

import android.Manifest;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Call;
import com.squareup.okhttp.Headers;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_CONTACTS = 6;
    ArrayList<String> formatList, qualityList, linkList, sizeList;
    String title, thumb;
    File mediaFile;
    ProgressDialog progressBar;
    EditText et;
    private String format;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        et = (EditText) findViewById(R.id.editText);
        Button bFetch = (Button) findViewById(R.id.button);

        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (Intent.ACTION_SEND.equals(action) && type != null) {
            if ("text/plain".equals(type)) {
                handleSendText(intent); // Handle text being sent
            }
        }
        formatList = new ArrayList<>();
        qualityList = new ArrayList<>();
        linkList = new ArrayList<>();
        sizeList = new ArrayList<>();

        progressBar = new ProgressDialog(this);

        bFetch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String id = et.getText().toString();
                if (ActivityCompat.checkSelfPermission(getApplicationContext(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    showStoragePermission();
                } else {

                    new FetchLinks().execute(id);
                }

            }
        });

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

    }

    private void handleSendText(Intent intent) {
        String sharedText = intent.getStringExtra(Intent.EXTRA_TEXT);
        if (sharedText != null) {
            // Update UI to reflect text being shared
            int index = sharedText.indexOf("youtu.be");
            Log.v("index", "" + index);
            StringBuilder ids = new StringBuilder();
            while (index < sharedText.length()) {
                ids.append(sharedText.charAt(index + 9));
                index++;
            }
            et.setText(ids);
        }
    }

    public void showStoragePermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);

        } else {

            // No explanation needed, we can request the permission.

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_CONTACTS);

            // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
            // app-defined int constant. The callback method gets the
            // result of the request.
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_CONTACTS: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                    Toast.makeText(this, "F U!!", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void whatFormat(String form8) {
        if (form8.contains("mp4")) {
            format = "mp4";
        } else if (form8.contains("webm")) {
            format = "webm";
        } else if (form8.contains("3gpp")) {
            format = "3gp";
        } else if (form8.contains("flv")) {
            format = "flv";
        }
    }

    private void showDialog() {
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_links);


        // set the custom dialog components - text, image and button

        ImageView ivThumb = (ImageView) dialog.findViewById(R.id.ivThumb);
        TextView tvTitle = (TextView) dialog.findViewById(R.id.tvTitle);

        tvTitle.setText(title);
        ListView lvLinks = (ListView) dialog.findViewById(R.id.lvLinks);
        lvLinks.setAdapter(new LinksAdapter());
        lvLinks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                whatFormat(formatList.get(i));
                new downloadVideo().execute(linkList.get(i));
            }
        });

        Picasso.with(this).load(thumb).into(ivThumb);

        dialog.show();
    }

    private void saveVideo() {

    }

    public class FetchLinks extends AsyncTask<String, Void, Void> {

        boolean error = false;
        String errorMsg;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setIndeterminate(false);
            progressBar.setMessage("Fetching Links... ");
            progressBar.setCanceledOnTouchOutside(false);
            progressBar.show();
        }

        @Override
        protected Void doInBackground(String... strings) {

            String url = "https://zazkov-youtube-grabber-v1.p.mashape.com/download.video.php?id=" + strings[0];
            Log.d("url :", url);

            String jsonStr = null;

            OkHttpClient client = new OkHttpClient();


            Request request = new Request.Builder()
                    .url(url)
                    .header("X-Mashape-Key", "8oh7FdicbKmshlelUm03nJbdY0o1p1TbPWKjsnef32LQMaWAL6")
                    .header("Accept", "application/json")
                    .build();

            Response response = null;
            try {
                response = client.newCall(request).execute();
                jsonStr = response.body().string();
            } catch (IOException e) {
                error = true;
                e.printStackTrace();
                Log.d("Response", "Error: " + e);
                errorMsg = e.toString();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });
            }
            Log.d("Response: ", "> " + jsonStr);

            if (jsonStr != null) {
                try {
                    JSONObject jsonObj = new JSONObject(jsonStr);

                    title = jsonObj.getString("title");
                    thumb = jsonObj.getString("img");

                    // Getting JSON Array node
                    JSONArray dataArray = jsonObj.getJSONArray("map");

                    // looping through all results
                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONArray c = dataArray.getJSONArray(i);

                        String format = c.getString(0);
                        String quality = c.getString(1);
                        String link = c.getString(2);
                        String size = c.getString(4);

                        formatList.add(format);
                        qualityList.add(quality);
                        linkList.add(link);
                        sizeList.add(size);
                    }
                } catch (JSONException e) {
                    error = true;
                    Log.e("error", e.toString());
                    errorMsg = e.toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                        }
                    });
                }
            } else {
                error = true;
                Log.e("ServiceHandler", "Couldn't get any data from the url");
                errorMsg = "Couldn't get any data from the url";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                    }
                });

            }

            client = null;
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.dismiss();
            showDialog();
        }
    }

    private class LinksAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return formatList.size();
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            if (view == null) {
                // if it's not recycled, initialize some attributes
                LayoutInflater inflater = (LayoutInflater)
                        getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                view = inflater.inflate(R.layout.list_item, viewGroup, false);
            }

            TextView tvFormat = (TextView) view.findViewById(R.id.tvFormat);
            TextView tvQuality = (TextView) view.findViewById(R.id.tvQuality);
            TextView tvSize = (TextView) view.findViewById(R.id.tvSize);

            tvFormat.setText(formatList.get(i));
            tvQuality.setText(qualityList.get(i));
            tvSize.setText(sizeList.get(i));

            return view;
        }
    }

    public class downloadVideo extends AsyncTask<String, Long, Boolean> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressBar.setIndeterminate(false);
            progressBar.setMessage("Downloading : ");
            progressBar.setCanceledOnTouchOutside(false);
            progressBar.show();
        }

        @Override
        protected Boolean doInBackground(String... strings) {
            OkHttpClient client = new OkHttpClient();
            String url = strings[0];
            Call call = client.newCall(new Request.Builder().url(url).get().build());

            try {
                Response response = call.execute();
                if (response.code() == 200 || response.code() == 201) {

                    Headers responseHeaders = response.headers();
                    for (int i = 0; i < responseHeaders.size(); i++) {
                        Log.d("Download Log", responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    InputStream inputStream = null;
                    try {
                        inputStream = response.body().byteStream();

                        byte[] buff = new byte[1024 * 4];
                        long downloaded = 0;
                        long target = response.body().contentLength();
                        mediaFile = new File(Environment.getExternalStorageDirectory(), title + "." + format);
                        OutputStream output = new FileOutputStream(mediaFile);

                        publishProgress(0L, target);
                        while (true) {
                            int readed = inputStream.read(buff);

                            if (readed == -1) {
                                break;
                            }
                            output.write(buff, 0, readed);
                            //write buff
                            downloaded += readed;
                            publishProgress(downloaded, target);
                            if (isCancelled()) {
                                return false;
                            }
                        }

                        output.flush();
                        output.close();

                        return downloaded == target;
                    } catch (IOException ignore) {
                        return false;
                    } finally {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                } else {
                    return false;
                }
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onProgressUpdate(Long... values) {
            super.onProgressUpdate(values);
            progressBar.setMax(values[1].intValue());
            progressBar.setProgress(values[0].intValue());
            progressBar.setMessage("Downloading : " + values[0].intValue() + "/" + values[1].intValue());
        }


        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            Toast.makeText(getApplicationContext(), "Completed " + aVoid, Toast.LENGTH_SHORT).show();
            if (mediaFile != null && mediaFile.exists()) {
                saveVideo();
            }
            progressBar.dismiss();
        }
    }
}
