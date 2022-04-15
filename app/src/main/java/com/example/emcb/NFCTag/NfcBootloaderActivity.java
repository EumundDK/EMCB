package com.example.emcb.NFCTag;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emcb.R;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.ftmprotocol.FtmCommands;
import com.st.st25sdk.ftmprotocol.FtmProtocol;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static com.example.emcb.NFCTag.NfcBootloaderActivity.Action.*;
import static com.example.emcb.NFCTag.NfcBootloaderActivity.ActionStatus.*;
import static com.st.st25sdk.ftmprotocol.FtmCommands.*;

public class NfcBootloaderActivity extends AppCompatActivity implements TagDiscovery.onTagDiscoveryCompletedListener, FtmProtocol.TransferProgressionListener {

    static final String TAG = "NfcBootloaderActivity";

    private Action mCurrentAction;

    private Button selectFileButton;
    private Button writeFileButton;
    private Intent selectFileIntent;
    private TextView hexSelectFilenameTextView;
    private TextView mailboxStatusTextView;
    private TextView ftmStatusProgressTextView;
    private ProgressBar mProgressBar;
    private TextView fileDataTextView;
//    private FtmCommands mFtmCommands;

    private NfcAdapter myNfcAdapter;
    private NFCTag myNfcTag;
    private ST25DVTag myST25DVTag;
    private byte mFtmResponse;

    private boolean mCancelCurrentTransfer = false;
    private boolean mIsErrorRecoveryEnabled = true;

    private int mNbrOfBytesToSend;
    private byte[] mPassword = {0,0,0,0,0,0,0,0};

    private InputStream mFirmwareInputStream;

    private int sendFtmDataLength = 253;
    private byte sendFtmDataCounter = 0;

    private byte[] bootloaderData = new byte[8192];
    private byte[] randomData;

    private byte[] receiveData = new byte[256];
    private int receiveLength = 256;
    private byte[] sendFtmData = new byte[256];

    enum Action {
        IDLE,
        GET_BOARD_INFO,
        ENABLE_MAILBOX,
        FIRMWARE_UPGRADE,
        SEND_DATA,
        READ_DATA
    };

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD,
        CONFIG_PASSWORD_NEEDED,
        ERROR_DURING_PWD_PRESENTATION,
        NO_RESPONSE
    };

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        Uri path = data.getData();
                        String fileName = path.getPath();
                        try {
                            hexSelectFilenameTextView.setText(fileName);
                            ReadTextFile(path);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_bootloader);

        myNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        selectFileButton = (Button) findViewById(R.id.selectFileButton);
        selectFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFileIntent = new Intent(Intent.ACTION_GET_CONTENT);
                selectFileIntent.setType("*/*");
                activityResultLauncher.launch(selectFileIntent);
            }
        });

        writeFileButton = (Button) findViewById(R.id.writeFileButton);
        writeFileButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Write HEX File on button press
                executeAsynchronousAction(SEND_DATA);
            }
        });
        hexSelectFilenameTextView = (TextView) findViewById(R.id.hexSelectFilenameTextView);
        hexSelectFilenameTextView.setText(R.string.blank);

        mailboxStatusTextView = (TextView) findViewById(R.id.mailboxStatusTextView);
        mailboxStatusTextView.setText(R.string.mailbox_disabled);
        mailboxStatusTextView.setTextColor(getResources().getColor(R.color.red));

        ftmStatusProgressTextView = (TextView) findViewById(R.id.ftmStatusProgressTextView);
        ftmStatusProgressTextView.setText(R.string.idle);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        fileDataTextView = (TextView) findViewById(R.id.fileData);

    }

    @Override
    protected void onPause() {
        super.onPause();

        if (myNfcAdapter != null) {
            myNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Check if if this phone has NFC hardware
        if (myNfcAdapter == null) {

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

            // set title
            alertDialogBuilder.setTitle("Warning!");

            // set dialog message
            alertDialogBuilder
                    .setMessage("This phone doesn't have NFC hardware!")
                    .setCancelable(true)
                    .setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,int id) {
                            dialog.cancel();
                            finish();
                        }
                    });

            // create alert dialog
            AlertDialog alertDialog = alertDialogBuilder.create();

            // show it
            alertDialog.show();

        } else {
            //Toast.makeText(this, "We are ready to play with NFC!", Toast.LENGTH_SHORT).show();

            // Give priority to the current activity when receiving NFC events (over other actvities)
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
            IntentFilter[] nfcFilters = null;
            String[][] nfcTechLists = null;
            myNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcFilters, nfcTechLists);
        }

        // The current activity can be resumed for several reasons (NFC tag tapped is one of them).
        // Check what was the reason which triggered the resume of current application
        Intent intent = getIntent();
        String action = intent.getAction();

        if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) ||
                action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {

            // If the resume was triggered by an NFC event, it will contain an EXTRA_TAG providing
            // the handle of the NFC Tag
            Tag androidTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (androidTag != null) {
                // This action will be done in an Asynchronous task.
                // onTagDiscoveryCompleted() of current activity will be called when the discovery is completed.
                new TagDiscovery(this).execute(androidTag);
            }
        }

        if (mCurrentAction == IDLE) {
            executeAsynchronousAction(GET_BOARD_INFO);
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        // onResume() gets called after this to handle the intent
        setIntent(intent);
    }

    public void ReadTextFile(Uri filePathName) throws IOException {
        String string = "";
        StringBuilder stringBuilder = new StringBuilder();
        InputStream is = getContentResolver().openInputStream(filePathName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        int dataLength = is.available();
        bootloaderData = new byte[dataLength];
        mNbrOfBytesToSend = dataLength;
        int writeOffset = 0;
        while (dataLength > 0) {
            int len = Math.min(dataLength, 8192);
            int dataRead = is.read(bootloaderData, writeOffset, len);

            writeOffset += dataRead;

            dataLength = is.available();
        }

        while (true) {
            try {
                if ((string = reader.readLine()) == null) break;
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            stringBuilder.append(string);
        }
        String s = new String(bootloaderData, StandardCharsets.UTF_8);
        fileDataTextView.setText(s);
        is.close();
        Toast.makeText(getBaseContext(), filePathName.toString(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onTagDiscoveryCompleted(NFCTag nfcTag, TagHelper.ProductID productId, STException e) {
        if(e != null) {
            Toast.makeText(getApplication(), "Error while reading the tag: " + e.toString(), Toast.LENGTH_SHORT).show();
            return;
        }

        if (nfcTag != null) {
            myNfcTag = nfcTag;
            myST25DVTag = (ST25DVTag) myNfcTag;

            try {
                String uidString = nfcTag.getUidString();
                executeAsynchronousAction(Action.ENABLE_MAILBOX);

            } catch (STException error) {
                error.printStackTrace();
                Toast.makeText(getApplication(),"Discovery successful but failed to read the tag!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void transmissionProgress(int transmittedBytes, int acknowledgedBytes, int totalSize) {

        int progress = (acknowledgedBytes * 100) / totalSize;
        int secondaryProgress = (transmittedBytes * 100) / totalSize;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftmStatusProgressTextView.setText(R.string.writing);

                switch (mCurrentAction) {
                    case FIRMWARE_UPGRADE:
                    case SEND_DATA:
                        mProgressBar.setProgress(progress);
                        mProgressBar.setSecondaryProgress(secondaryProgress);
                        break;
                    default:
                        break;
                }
            }
        });
    }

    @Override
    public void receptionProgress(int i, int i1, int i2) {

    }

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
        mCurrentAction = action;
        new myAsyncTask(action).execute();
    }

    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;
        private boolean mIsMailboxEnabled;
        private byte[] mDataReceived;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;
            mCancelCurrentTransfer = false;

            try {
                switch (mAction) {
                    case GET_BOARD_INFO:
                        mIsMailboxEnabled = myST25DVTag.isMailboxEnabled(true);
                        if (mIsMailboxEnabled) {
                            // Retrieve board name and FW version
                            //byte[] data = new byte[] { 0x00, 0x00 };
                            //mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_GET_BOARD_INFO, data,false, true, null, FtmCommands.SHORT_FTM_TIME_OUT_IN_MS);
                            mailboxStatusTextView.setText(R.string.mailbox_enabled);
                            mailboxStatusTextView.setTextColor(getResources().getColor(R.color.green));
                            //parseBoardInfo(mDataReceived);
                        }
                        result = ACTION_SUCCESSFUL;
                        break;
                    case ENABLE_MAILBOX:
                        myST25DVTag.enableMailbox();
                        result = ACTION_SUCCESSFUL;
                        break;
                    case READ_DATA:
                        byte cmdId = mIsErrorRecoveryEnabled ? FTM_CMD_READ_DATA : FTM_CMD_READ_DATA_NO_ERROR_RECOVERY;
                        int mbAddress = 0;
//                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(cmdId, null, mIsErrorRecoveryEnabled, true, NfcBootloaderActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        receiveData = myST25DVTag.readMailboxMessage(mbAddress, receiveLength);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case SEND_DATA:
                        // Send some random data of the requested length (mNbrOfBytesToSend)
                        if (mNbrOfBytesToSend < 256) {
//                            mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_DATA, null, mIsErrorRecoveryEnabled, true, NfcBootloaderActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                            mFtmResponse = myST25DVTag.writeMailboxMessage(bootloaderData);
                        } else {
                            // There will be one byte of command and (mNbrOfBytesToSend-1) bytes of random data
//                            randomData = new byte[1];
//                            mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_DATA, randomData, mIsErrorRecoveryEnabled, true, NfcBootloaderActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                            for(int i = 0; i < mNbrOfBytesToSend; i++) {
                                int checkData = (i % (sendFtmDataLength));
                                if(checkData == 0) {
                                    sendFtmData[checkData] = sendFtmDataCounter;
                                    sendFtmDataCounter++;
                                } else {
                                    sendFtmData[checkData] = bootloaderData[i];
                                }

                                if((i >= sendFtmDataLength ) && (checkData % sendFtmDataLength == 0)) {
                                    mFtmResponse = myST25DVTag.writeMailboxMessage(sendFtmData);
                                } else if (i >= (mNbrOfBytesToSend - 1)) {
                                    mFtmResponse = myST25DVTag.writeMailboxMessage(sendFtmData);
                                }
                            }

                        }
                        result = ACTION_SUCCESSFUL;
                        break;
                    case FIRMWARE_UPGRADE:
                        // Send the password
//                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_PASSWORD, mPassword, mIsErrorRecoveryEnabled, true, NfcBootloaderActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        // Command successful
                        // Send the firmware
//                        mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_FW_UPGRADE, bootloaderData, mIsErrorRecoveryEnabled, true, NfcBootloaderActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
                        result = ACTION_SUCCESSFUL;
                        break;

                    default:
                        result = ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                switch (e.getError()) {
                    case CONFIG_PASSWORD_NEEDED:
                        result = ActionStatus.CONFIG_PASSWORD_NEEDED;
                        break;

                    case TAG_NOT_IN_THE_FIELD:
                        result = TAG_NOT_IN_THE_FIELD;
                        break;

                    case RFREADER_NO_RESPONSE:
                        result = NO_RESPONSE;
                        break;

                    default:
                        e.printStackTrace();
                        result = ACTION_FAILED;
                        break;
                }
            } catch (Exception e) {
                e.printStackTrace();
                result = ACTION_FAILED;
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            mCurrentAction = IDLE;

            switch (actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case GET_BOARD_INFO:
                            if (mIsMailboxEnabled) {
                                mailboxStatusTextView.setText(R.string.mailbox_enabled);
                                mailboxStatusTextView.setTextColor(getResources().getColor(R.color.green));
                            } else {
                                mailboxStatusTextView.setText(R.string.mailbox_disabled);
                                mailboxStatusTextView.setTextColor(getResources().getColor(R.color.red));
                            }
                            break;
                        case ENABLE_MAILBOX:
                            try {
                                mIsMailboxEnabled = myST25DVTag.isMailboxEnabled(false);
                            } catch (STException e) {
                                e.printStackTrace();
                            }
                            if (mIsMailboxEnabled) {
                                mailboxStatusTextView.setText(R.string.mailbox_enabled);
                                mailboxStatusTextView.setTextColor(getResources().getColor(R.color.green));
                                Toast.makeText(NfcBootloaderActivity.this, R.string.mailbox_enabled, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(NfcBootloaderActivity.this, R.string.failed, Toast.LENGTH_LONG).show();
                            }
                            break;

                        case READ_DATA:
                        case FIRMWARE_UPGRADE:
                        case SEND_DATA:
                            ftmStatusProgressTextView.setText(R.string.success);
                            Toast.makeText(NfcBootloaderActivity.this, R.string.command_successful, Toast.LENGTH_LONG).show();
                            break;
                    }
                    break;

                case CONFIG_PASSWORD_NEEDED:
                    //displayConfigurationPasswordDialogBox();
                    break;

                case ERROR_DURING_PWD_PRESENTATION:
                    Toast.makeText(NfcBootloaderActivity.this, R.string.error_during_password_presentation, Toast.LENGTH_LONG).show();
                    break;

                case ACTION_FAILED:
                    if (!mCancelCurrentTransfer) {
                        ftmStatusProgressTextView.setText(R.string.failed);
                        Toast.makeText(NfcBootloaderActivity.this, R.string.command_failed, Toast.LENGTH_LONG).show();

                    }
                    break;

                case NO_RESPONSE:
                    if (!mCancelCurrentTransfer) {
                        if (mAction == GET_BOARD_INFO) {
                            //UIHelper.displayMessage(FtmBootloaderActivity.this, R.string.failed_to_retrieve_board_name);
                            Toast.makeText(NfcBootloaderActivity.this, R.string.failed_to_retrieve_board_name, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(NfcBootloaderActivity.this, R.string.no_response_received, Toast.LENGTH_LONG).show();
                        }
                    }
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    if (!mCancelCurrentTransfer) {
                        if (mAction == GET_BOARD_INFO) {
                            //UIHelper.displayMessage(FtmBootloaderActivity.this, R.string.please_put_the_phone_on_the_discovery_board);
                            Toast.makeText(NfcBootloaderActivity.this, R.string.please_put_the_phone_on_the_discovery_board, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(NfcBootloaderActivity.this, R.string.tag_not_in_the_field, Toast.LENGTH_LONG).show();
                        }
                    }
                    break;
            }

        }
    }

}