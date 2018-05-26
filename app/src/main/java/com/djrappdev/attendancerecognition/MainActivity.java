package com.djrappdev.attendancerecognition;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private final String app_id = "858c50a3";
    private final String app_key = "1f30faa8bbb6406231731963e9129191";
    private RequestQueue queue;
    private Button addStudent, takePhoto, verifyStudent;
    private String studentPath;
    private TextView statusText, verifyResult;
    private EditText osisField;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        addStudent = findViewById(R.id.addStudent);
        statusText = findViewById(R.id.attendanceStatus);
        osisField = findViewById(R.id.osisField);
        takePhoto = findViewById(R.id.takePhoto);
        verifyStudent = findViewById(R.id.verifyStudent);
        verifyResult = findViewById(R.id.verifyResult);

        queue = Volley.newRequestQueue(this);

        //Adds OnClickListener for adding students
        addStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!osisField.getText().toString().equals("")) {
                    try {
                        addStudent(imageToBase64(studentPath), osisField.getText().toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        });

        //Adds OnClickListener to take a photo
        takePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!osisField.getText().toString().equals("")) {
                    dispatchTakePictureIntent(osisField.getText().toString());
                }
            }
        });

        //Adds method to verify students
        verifyStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    verifyStudent(imageToBase64(studentPath));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    //Functions to take photo and save it to internal storage
    private void dispatchTakePictureIntent(String OSIS) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile(OSIS);
            } catch (IOException ex) {
                // Error occurred while creating the File
                statusText.setText("Failed to create student's file!");
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
    private File createImageFile(String OSIS) throws IOException {
        // Create an image file name
        String imageFileName = OSIS;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        studentPath = image.getAbsolutePath();
        return image;
    }

    //Function to convert image into Base64
    public String imageToBase64(String imagePath) throws IOException {
        File image = new File(imagePath);
        byte[] bytes = Files.readAllBytes(image.toPath());
        return Base64.getEncoder().encodeToString(bytes);
    }

    //Functions to enroll and verify different faces
    public void addStudent(final String image, final String OSIS) throws JSONException {
        String url = "https://api.kairos.com/enroll";
        JSONObject request = new JSONObject();
        request.put("image", image);
        request.put("subject_id", OSIS);
        request.put("gallery_name", "Students");
        Request postRequest = new JsonObjectRequest(Request.Method.POST, url, request,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d("Response", response.toString());
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error Response", error.getMessage());
                    }
                }) {
            //Old code that is obsoleted by JSONObject above. If anyone can find a way to fix it, much appreciated!
            /*@Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("image", image);
                params.put("subject_id", OSIS);
                params.put("gallery_name", "Students");
                return params;
            }*/

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("app_id", app_id);
                headers.put("app_key", app_key);
                return headers;
            }
        };
        queue.add(postRequest);
    }

    public void verifyStudent(final String image) throws JSONException {
        String url = "https://api.kairos.com/recognize";
        JSONObject request = new JSONObject();
        request.put("gallery_name", "Students");
        request.put("image", image);
        //Hardcoded test
        //request.put("image","https://upload.wikimedia.org/wikipedia/commons/2/25/Xi_Jinping_October_2013_%28cropped%29_%28cropped%29.jpg");
        Request postRequest = new JsonObjectRequest(Request.Method.POST, url, request,
                new Response.Listener<JSONObject>() {
                    @Override
                    //TODO: Case checking so app knows what to do with the verification response from Kairos API.
                    public void onResponse(JSONObject response) {
                        // response
                        Log.d("Response", response.toString());
                        try {
                            Log.d("Response", "" + response.getJSONArray("Errors").getJSONObject(0).getInt("ErrCode"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // error
                        Log.d("Error Response", error.getMessage());
                    }
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("app_id", app_id);
                headers.put("app_key", app_key);
                return headers;
            }
        };
        queue.add(postRequest);
    }
}