
package com.example.babai.ranndom2.Activities;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.babai.ranndom2.BackUp.Backup;
import com.example.babai.ranndom2.BackUp.GoogleDriveBackup;
import com.example.babai.ranndom2.R;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveId;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.OpenFileActivityBuilder;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BackupActivity extends AppCompatActivity {
    private int REQUEST_CODE_PICKER = 2;
    private int REQUEST_CODE_SELECT = 3;

    private Backup backup;
    private GoogleApiClient mGoogleApiClient;
    private String TAG = "glucosio_drive_backup";
    private Button backupButton;
    private Button restoreButton;
    private IntentSender intentPicker;


    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);


        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Backup and Restore");

        backup = new GoogleDriveBackup();
        backup.init(this);
        connectClient();
        mGoogleApiClient = backup.getClient();

        backupButton = (Button) findViewById(R.id.driveUpload);
        restoreButton = (Button) findViewById(R.id.driveDownload);

        backupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFolderPicker();
            }
        });

        restoreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openFilePicker();
            }
        });
    }

    private void openFilePicker() {
        //        build an intent that we'll use to start the open file activity
        IntentSender intentSender = Drive.DriveApi
                .newOpenFileActivityBuilder()
//                these mimetypes enable these folders/files types to be selected
                //.setMimeType(new String[]{DriveFolder.MIME_TYPE, "text/plain"})
                .build(mGoogleApiClient);
        try {
            startIntentSenderForResult(
                    intentSender, REQUEST_CODE_SELECT, null, 0, 0, 0);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Unable to send intent", e);
            showErrorDialog();
        }
    }

    private void openFolderPicker() {
        try {
            if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                if (intentPicker == null)
                    intentPicker = buildIntent();
                //Start the picker to choose a folder
                startIntentSenderForResult(
                        intentPicker, REQUEST_CODE_PICKER, null, 0, 0, 0);
            }
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Unable to send intent", e);
            showErrorDialog();
        }
    }

    private IntentSender buildIntent() {
        return Drive.DriveApi
                .newOpenFileActivityBuilder()
                .setMimeType(new String[]{DriveFolder.MIME_TYPE})
                .build(mGoogleApiClient);
    }

    private void downloadFromDrive(DriveFile file) {
        file.open(mGoogleApiClient, DriveFile.MODE_READ_ONLY, null)
                .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                    @Override
                    public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                        if (!result.getStatus().isSuccess()) {
                            showErrorDialog();
                            return;
                        }

                        // DriveContents object contains pointers
                        // to the actual byte stream
                        DriveContents contents = result.getDriveContents();
                        InputStream input = contents.getInputStream();

                        try {
                            File file = new File(Environment.getDataDirectory(), "//data//" + "com.example.babai.ranndom2"
                                    + "//databases//" + "notes.db");
                            OutputStream output = new FileOutputStream(file);
                            try {
                                try {
                                    byte[] buffer = new byte[4 * 1024]; // or other buffer size
                                    int read;

                                    while ((read = input.read(buffer)) != -1) {
                                        output.write(buffer, 0, read);
                                    }
                                    output.flush();
                                } finally {
                                    output.close();
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                input.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        Toast.makeText(getApplicationContext(), "Restart", Toast.LENGTH_LONG).show();

                        // Reboot app
                        Intent mStartActivity = new Intent(getApplicationContext(), MainActivity.class);
                        int mPendingIntentId = 123456;
                        PendingIntent mPendingIntent = PendingIntent.getActivity(getApplicationContext(), mPendingIntentId, mStartActivity, PendingIntent.FLAG_CANCEL_CURRENT);
                        AlarmManager mgr = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
                        mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 100, mPendingIntent);
                        System.exit(0);
                    }
                });
    }

    private void uploadToDrive(DriveId mFolderDriveId) {
        Log.v("HAHAHAHHA","Drive upload called");
        if (mFolderDriveId != null) {
            //Create the file on GDrive
            final DriveFolder folder = mFolderDriveId.asDriveFolder();
            Drive.DriveApi.newDriveContents(mGoogleApiClient)
                    .setResultCallback(new ResultCallback<DriveApi.DriveContentsResult>() {
                        @Override
                        public void onResult(@NonNull DriveApi.DriveContentsResult result) {
                            if (!result.getStatus().isSuccess()) {
                                Log.e(TAG, "Error while trying to create new file contents");
                                showErrorDialog();
                                return;
                            }
                            final DriveContents driveContents = result.getDriveContents();

                            // Perform I/O off the UI thread.
                            new Thread() {
                                @Override
                                public void run() {
                                    // write content to DriveContents
                                    OutputStream outputStream = driveContents.getOutputStream();

                                    FileInputStream inputStream = null;
                                    try {
                                        inputStream = new FileInputStream(new File(Environment.getDataDirectory(), "//data//" + "com.example.babai.ranndom2"
                                                + "//databases//" + "notes.db"));
                                    } catch (FileNotFoundException e) {
                                        showErrorDialog();
                                        e.printStackTrace();
                                    }

                                    byte[] buf = new byte[1024];
                                    int bytesRead;
                                    try {
                                        if (inputStream != null) {
                                            while ((bytesRead = inputStream.read(buf)) > 0) {
                                                outputStream.write(buf, 0, bytesRead);
                                            }
                                        }
                                    } catch (IOException e) {
                                        showErrorDialog();
                                        e.printStackTrace();
                                    }


                                    MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                                            .setTitle("notes.db")
                                            //.setMimeType("text/plain")
                                            .build();

                                    // create a file in selected folder
                                    folder.createFile(mGoogleApiClient, changeSet, driveContents)
                                            .setResultCallback(new ResultCallback<DriveFolder.DriveFileResult>() {
                                                @Override
                                                public void onResult(@NonNull DriveFolder.DriveFileResult result) {
                                                    if (!result.getStatus().isSuccess()) {
                                                        Log.d(TAG, "Error while trying to create the file");
                                                        showErrorDialog();
                                                        finish();
                                                        return;
                                                    }
                                                    showSuccessDialog();
                                                    finish();
                                                }
                                            });
                                }
                            }.start();
                        }
                    });
        }
    }

    @Override
    protected void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    backup.start();
                }
                break;
            // REQUEST_CODE_PICKER
            case 2:
                intentPicker = null;

                if (resultCode == RESULT_OK) {
                    //Get the folder drive id
                    DriveId mFolderDriveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

                    uploadToDrive(mFolderDriveId);
                }
                break;

            // REQUEST_CODE_SELECT
            case 3:
                if (resultCode == RESULT_OK) {
                    // get the selected item's ID
                    DriveId driveId = data.getParcelableExtra(
                            OpenFileActivityBuilder.EXTRA_RESPONSE_DRIVE_ID);

                    DriveFile file = driveId.asDriveFile();
                    downloadFromDrive(file);

                } else {
                    showErrorDialog();
                }
                finish();
                break;

        }
    }

    private void showSuccessDialog() {
        Toast.makeText(getApplicationContext(), "Backup success", Toast.LENGTH_SHORT).show();
    }

    private void showErrorDialog() {
        Toast.makeText(getApplicationContext(), "Backup failed", Toast.LENGTH_SHORT).show();
    }


    public void connectClient() {
        backup.start();
    }

    public void disconnectClient() {
        backup.stop();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        finish();
        return true;
    }
}
