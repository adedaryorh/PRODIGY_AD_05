package com.example.qrcode;

import android.Manifest;
import android.app.Activity;
import android.app.TaskInfo;
import android.content.ContentValues;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.List;
import java.util.stream.Stream;

public class MainActivity extends AppCompatActivity {

    private MaterialButton cameraBtn;
    private MaterialButton galleryBtn;
    private MaterialButton scanBtn;
    private ImageView imageIv;
    private TextView resultTv;

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;


    private String[] cameraPermissions;
    private String[] storagePermissions;

    //uri of the image to be taken from camera or gallery
    private Uri imageUri = null;

    private BarcodeScannerOptions barcodeScannerOptions;
    private BarcodeScanner barcodeScanner;
    private  static final String TAG ="MAIN_TAG";




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        //init UI views
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        scanBtn = findViewById(R.id.scanBtn);
        imageIv = findViewById(R.id.imageIv);
        resultTv = findViewById(R.id.resultTv);




        //image from camera
        cameraPermissions = new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        //imgae from gallery.
        storagePermissions = new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

        /**
         *init barcodeScannerOptions, put comma separated typed in .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS) or add Barcode.FORMAT_ALL_FORMATS)
         * it works for all formats like
         * Code 128(FORMAT_CODE_128), Code 39(Format_CODE_39), Code 93(FOrmat_code_93), Codobar(FORMAT_CODOBAR), EAN-13(FORMAT_EAN_13),
         * EAN-8(FORMAT_EAN_8), ITF(FORMAT_ITF), UPC-A, UPC-E, QR CODE(FORMAT_QR_CODE), PDF417, AZTEC, DATA MATRIX.
         */
        barcodeScannerOptions = new BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build();
        barcodeScanner = BarcodeScanning.getClient(barcodeScannerOptions);




        //handle cameraBtn click, check permissions related to camera(I.e WRITE STORAGE & CAMERA) and also capture image.
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkCameraPermission()){
                    pickImageCamera();
                }
                else {
                    requestCameraPermission();
                }
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (checkStoragePermissions()){
                    pickImageGallery();
                }
                else {
                    requestStoragePermission();
                }
            }
        });

        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null){
                    Toast.makeText(MainActivity.this, "Select Image firstly", Toast.LENGTH_SHORT).show();
                }
                else {
                    detectResultFromImage();
                }
            }
        });
    }

    public void detectResultFromImage(){
         try {
             InputImage inputImage = InputImage.fromFilePath(this, imageUri);

             Task<List<Barcode>> barcodeResult = barcodeScanner.process(inputImage).addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                 @Override
                 public void onSuccess(List<Barcode> barcodes) {
                     //task completed successfully, we can get detailed info now.
                     extractBarCodeQRCodeInfo(barcodes);
                 }
             }).addOnFailureListener(new OnFailureListener() {
                 @Override
                 public void onFailure(@NonNull Exception e) {
                     Toast.makeText(MainActivity.this, "Failed to Scan bcos "+ e.getMessage(), Toast.LENGTH_SHORT).show();
                 }
             });
         }
         catch(Exception e){
             //failed with an exp either due to preparing the InputImage or issues with the barcodeScanner init
             Toast.makeText(MainActivity.this, "Failed  due to "+ e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void extractBarCodeQRCodeInfo(List<Barcode> barcodes) {
        for (Barcode barcode : barcodes){
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();
            Log.d(TAG, "extractBarCodeInfo: rawValue: "+ rawValue);

            /**
             * Types of supported Barcodes
             * Barcode.TYPE_UNKNOWN, BARCODE.TYPE_CONTACT_INFO, TYPE_EMAIL, TYPE_ISBN, TYPE_PHONE
             * TYPE_PRODUCT, TYPE_SMS, TYPE_TEXT, TYPE_URL, TYPE_WIFI, TYPE_GEO, TYPE_CALENDAR_EVENT, TYPE_DRIVER_LICENSE
             */
            int valueType = barcode.getValueType();
            switch (valueType){
                case Barcode.TYPE_WIFI:{
                    Barcode.WiFi typeWifi = barcode.getWifi();
                    //retrieve all information abt the wifi
                    String ssid = ""+ typeWifi.getSsid();
                    String password = ""+ typeWifi.getPassword();
                    String encryptionType = ""+ typeWifi.getEncryptionType();

                    Log.d(TAG, "extractBarCodeInfo: TYPE_WIFI: ");
                    Log.d(TAG, "extractBarCodeInfo: ssid: "+ ssid);
                    Log.d(TAG, "extractBarCodeInfo: password: "+ password);
                    Log.d(TAG, "extractBarCodeInfo: encryptionType: "+ encryptionType);

                    resultTv.setText("TYPE: TYPE_WIFI \nssid: " + ssid + "\npassword: "+ password + "\nencryptionType: "+ encryptionType + "\nraw value: "+ rawValue);
                }
                break;
                case Barcode.TYPE_URL:{
                    Barcode.UrlBookmark typeUrl = barcode.getUrl();

                    String title = ""+typeUrl.getTitle();
                    String url = ""+typeUrl.getUrl();
                    Log.d(TAG, "extractBarCodeInfo: TYPE_URL: ");
                    Log.d(TAG, "extractBarCodeInfo: title: " + title);
                    Log.d(TAG, "extractBarCodeInfo: url: " + url);

                    resultTv.setText("TYPE: TYPE_URL \ntitle: "+ title +"\nurl: "+ url+ "\nraw value: "+ rawValue);
                }
                break;
                case Barcode.TYPE_EMAIL:{
                    Barcode.Email typeEmail = barcode.getEmail();

                    String address = "" + typeEmail.getAddress();
                    String body = "" + typeEmail.getBody();
                    String subject = "" + typeEmail.getSubject();

                    Log.d(TAG, "extractBarCodeInfo: TYPE_EMAIL");
                    Log.d(TAG, "extractBarCodeInfo: address: " + address);
                    Log.d(TAG, "extractBarCodeInfo: body: " + body);
                    Log.d(TAG, "extractBarCodeInfo: subject: " + subject);

                    resultTv.setText("TYPE: TYPE_EMAIL \naddress: "+ address +"\nbody: "+ body+ "\nsubject: "+ subject+ "\nraw value: "+ rawValue);
                }
                break;
                case Barcode.TYPE_CONTACT_INFO:{

                    Barcode.ContactInfo typeContact = barcode.getContactInfo();

                    String title = ""+typeContact.getTitle();
                    String organizer = ""+ typeContact.getOrganization();
                    String name = ""+typeContact.getName().getFirst()+""+typeContact.getName().getLast();
                    String phones = ""+typeContact.getPhones().get(0).getNumber();

                    Log.d(TAG, "extractBarCodeInfo: TYPE_CONTACT_INFO");
                    Log.d(TAG, "extractBarCodeInfo: title: " + title);
                    Log.d(TAG, "extractBarCodeInfo: organizer: " + organizer);
                    Log.d(TAG, "extractBarCodeInfo: name: " + name);
                    Log.d(TAG, "extractBarCodeInfo: phones: " + phones);

                    resultTv.setText("TYPE: TYPE_CONTACT_INFO \ntitle: "+ title +"\norganizer: "+ organizer+ "\nname: "+ name+ "\nphones: "+ phones+"\nraw value: "+ rawValue);
                }
                break;
                default:{
                    resultTv.setText("raw value: "+ rawValue);
                }
            }
        }
    }


    private void pickImageGallery(){
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK){

                        Intent data = result.getData();
                        imageUri = data.getData();
                        Log.d(TAG, "onActivityResult: imageUri: "+ imageUri);

                        imageIv.setImageURI(imageUri);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    public void pickImageCamera(){
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.TITLE, "Sample Title");
        contentValues.put(MediaStore.Images.Media.DESCRIPTION, "Sample Image description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private final ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //we will receive image here, if taken from camera.
                    if (result.getResultCode() == Activity.RESULT_OK){
                        Intent data = result.getData();
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        imageIv.setImageURI(imageUri);
                    }
                    else {
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void requestStoragePermission(){
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }
    private void requestCameraPermission(){
        ActivityCompat.requestPermissions(this, cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        boolean resultCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;

        boolean resultStorage = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return resultCamera && resultStorage;
    }

    private boolean checkStoragePermissions(){
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED;

        return result;
    }


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case CAMERA_REQUEST_CODE: {
                //check if some actions from permission dialog perfomed or not Allow or Deny.
                if (grantResults.length > 0){
                    //confirm if camera, storage permissions granted.
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    //confirm if both permissions are granted or not
                    if (cameraAccepted && storageAccepted){
                        pickImageCamera();
                    }
                    else {
                        //when either of both permissions is denied, wont launch camera intent
                        Toast.makeText(this, "Camera and storage permissions are required...", Toast.LENGTH_SHORT).show();
                    }

                }
            }
            break;
            case STORAGE_REQUEST_CODE:{
                if (grantResults.length > 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (storageAccepted){

                        pickImageGallery();
                    }
                    else {
                        Toast.makeText(this,  "Storage permissions is required...", Toast.LENGTH_SHORT).show();
                    }
                }

            }
            break;
        }
    }


}