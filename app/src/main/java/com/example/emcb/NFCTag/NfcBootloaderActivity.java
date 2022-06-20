package com.example.emcb.NFCTag;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.os.Handler;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Chronometer;
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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static com.example.emcb.NFCTag.NfcBootloaderActivity.Action.*;
import static com.example.emcb.NFCTag.NfcBootloaderActivity.ActionStatus.*;

public class NfcBootloaderActivity extends AppCompatActivity implements TagDiscovery.onTagDiscoveryCompletedListener, FtmProtocol.TransferProgressionListener {

    static final String TAG = "NfcBootloaderActivity";
    private final int maxDataLength = 128;
    private final int MB_CTRL_Dyn = 13;
    private final int delayMs = 100;

    private Button selectFileButton;
    private Button writeFileButton;
    private Intent selectFileIntent;
    private TextView hexSelectFilenameTextView;
    private TextView mailboxStatusTextView;
    private TextView ftmStatusProgressTextView;
    private ProgressBar mProgressBar;
    private Chronometer mTimer;
    private TextView fileDataTextView;

    private NfcAdapter myNfcAdapter;
    private NFCTag myNfcTag;
    private ST25DVTag myST25DVTag;
    private FtmCommands mFtmCommands;
    private boolean mCancelCurrentTransfer = false;
    private boolean mIsErrorRecoveryEnabled = true;
    private boolean bootMode = false;

    private byte[] mPassword = {0,0,0,0,0,0,0,0};
    private int mNbrOfBytesToSend;
    private int mCurrentCountToSend;
    private Action mCurrentAction;
    private Action mNextAction;
    private byte mFtmResponse;
    private String bootloaderString;
    private byte[] bootloaderData;
    private byte[] mDataReceived;
    private byte[] startBoot = {-18,-86,-86};
    private byte[] endBoot = {-18,-86,-1};
    private byte[] readData;
    private byte[] sendData = new byte[maxDataLength];

    private int retry;
    private int maxRetry = 10;
    Handler handler = new Handler();

    enum Action {
        IDLE,
        GET_BOARD_INFO,
        ENABLE_MAILBOX,
        FIRMWARE_UPGRADE,
        SEND_DATA,
        READ_DATA,
        CHECK_STATUS,
        START_BOOT,
        END_BOOT
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
                mTimer.setBase(SystemClock.elapsedRealtime());
                mTimer.start();
                bootMode = true;
                executeAsynchronousAction(START_BOOT);
            }
        });
        writeFileButton.setClickable(false);
        writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state));

        hexSelectFilenameTextView = (TextView) findViewById(R.id.hexSelectFilenameTextView);
        hexSelectFilenameTextView.setText(R.string.blank);

        mailboxStatusTextView = (TextView) findViewById(R.id.mailboxStatusTextView);
        mailboxStatusTextView.setText(R.string.mailbox_disabled);
        mailboxStatusTextView.setTextColor(getResources().getColor(R.color.red));

        ftmStatusProgressTextView = (TextView) findViewById(R.id.ftmStatusProgressTextView);
        ftmStatusProgressTextView.setText(R.string.idle);
        mTimer = (Chronometer) findViewById(R.id.chronometer);

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);

        fileDataTextView = (TextView) findViewById(R.id.fileData);
        fileDataTextView.setMovementMethod(new ScrollingMovementMethod());

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
        if (action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) || action.equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            // If the resume was triggered by an NFC event, it will contain an EXTRA_TAG providing
            // the handle of the NFC Tag
            Tag androidTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (androidTag != null) {
                // This action will be done in an Asynchronous task.
                // onTagDiscoveryCompleted() of current activity will be called when the discovery is completed.
                new TagDiscovery(this).execute(androidTag);
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // onResume() gets called after this to handle the intent
        setIntent(intent);
    }

    public void ReadTextFile(Uri filePathName) throws IOException {
        InputStream is = getContentResolver().openInputStream(filePathName);
        int dataLength = is.available();
        int writeOffset = 0;
        bootloaderData = new byte[dataLength];
        mNbrOfBytesToSend = dataLength / 2;
        while (dataLength > 0) {
            int len = Math.min(maxDataLength, dataLength);
            int dataRead = is.read(bootloaderData, writeOffset, len);
            writeOffset += dataRead;
            dataLength = is.available();
        }
        bootloaderString = new String(bootloaderData, StandardCharsets.UTF_8);
        bootloaderData = hexToByteArray(bootloaderString);
        fileDataTextView.setText(bootloaderString);
        is.close();
    }

    @Override
    public void onTagDiscoveryCompleted(NFCTag nfcTag, TagHelper.ProductID productId, STException e) {
        if(e != null) {
            writeFileButton.setClickable(false);
            writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state));
            Toast.makeText(getApplication(), "Error while reading the tag: " + e.toString(), Toast.LENGTH_SHORT).show();
            return;
        }
        if (nfcTag != null && !bootMode) {
            myNfcTag = nfcTag;
            myST25DVTag = (ST25DVTag) myNfcTag;
            mFtmCommands = new FtmCommands(myST25DVTag);
            try {
                String uidString = nfcTag.getUidString();
                executeAsynchronousAction(Action.ENABLE_MAILBOX);
            } catch (STException error) {
                error.printStackTrace();
                writeFileButton.setClickable(false);
                writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state));
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
                ftmStatusProgressTextView.setText(String.format("%d / %d bytes", acknowledgedBytes, totalSize));
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
                        result = ACTION_SUCCESSFUL;
                        break;
                    case ENABLE_MAILBOX:
                        myST25DVTag.enableMailbox();
                        result = ACTION_SUCCESSFUL;
                        break;
                    case READ_DATA:
                        int mbAddress = 0;
                        readData = myST25DVTag.readMailboxMessage(mbAddress, maxDataLength);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case SEND_DATA:
                        for(int i = 0; i < maxDataLength; i++) {
                            sendData[i] = bootloaderData[mCurrentCountToSend + i];
                        }
                        mFtmResponse = myST25DVTag.writeMailboxMessage(maxDataLength, sendData);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case CHECK_STATUS:
                        mDataReceived = myST25DVTag.readDynConfig(MB_CTRL_Dyn);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case START_BOOT:
                        mFtmResponse = myST25DVTag.writeMailboxMessage(startBoot);
                        result = ACTION_SUCCESSFUL;
                        break;
                    case END_BOOT:
                        mFtmResponse = myST25DVTag.writeMailboxMessage(endBoot);
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
            switch (actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case GET_BOARD_INFO:
                            if (mIsMailboxEnabled) {
                                writeFileButton.setClickable(true);
                                writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state_normal));
                                mailboxStatusTextView.setText(R.string.mailbox_enabled);
                                mailboxStatusTextView.setTextColor(getResources().getColor(R.color.green));
                            } else {
                                writeFileButton.setClickable(false);
                                writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state));
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
                                writeFileButton.setClickable(true);
                                writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state_normal));
                                mailboxStatusTextView.setText(R.string.mailbox_enabled);
                                mailboxStatusTextView.setTextColor(getResources().getColor(R.color.green));
                                Toast.makeText(NfcBootloaderActivity.this, R.string.mailbox_enabled, Toast.LENGTH_LONG).show();
                            } else {
                                Toast.makeText(NfcBootloaderActivity.this, R.string.failed, Toast.LENGTH_LONG).show();
                            }
                            break;
                        case READ_DATA:
                            ftmStatusProgressTextView.setText("Read OK");
                            Toast.makeText(NfcBootloaderActivity.this, R.string.command_successful, Toast.LENGTH_LONG).show();
                            break;
                        case SEND_DATA:
                            if(mFtmResponse == 0) {
                                mCurrentCountToSend += maxDataLength;
                                transmissionProgress(mCurrentCountToSend, mCurrentCountToSend, mNbrOfBytesToSend);
                            }
                            if(mCurrentCountToSend == mNbrOfBytesToSend) {
                                mNextAction = END_BOOT;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeAsynchronousAction(CHECK_STATUS);
                                    }
                                }, delayMs);
                            } else {
                                mNextAction = SEND_DATA;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeAsynchronousAction(CHECK_STATUS);
                                    }
                                }, delayMs);
                            }
                            break;
                        case CHECK_STATUS:
                            if(mDataReceived[1] == -127) {
                                retry = 0;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeAsynchronousAction(mNextAction);
                                    }
                                }, delayMs);
                            } else {
                                retry++;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeAsynchronousAction(CHECK_STATUS);
                                    }
                                }, delayMs);
                            }
                            break;
                        case START_BOOT:
                            ftmStatusProgressTextView.setText("Boot Start");
                            mCurrentCountToSend = 0;
                            mProgressBar.setProgress(0);
                            mProgressBar.setSecondaryProgress(0);
                            if(mFtmResponse == 0) {
                                mNextAction = SEND_DATA;
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeAsynchronousAction(CHECK_STATUS);
                                    }
                                }, delayMs);
                            } else {
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeAsynchronousAction(START_BOOT);
                                    }
                                }, delayMs);
                            }
                            break;
                        case END_BOOT:
                            if(mFtmResponse == 0) {
                                mNextAction = null;
                                ftmStatusProgressTextView.setText("Boot End");
                                mTimer.stop();
                                bootMode = false;
                                Toast.makeText(NfcBootloaderActivity.this, R.string.command_successful, Toast.LENGTH_LONG).show();
                            } else {
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        executeAsynchronousAction(CHECK_STATUS);
                                    }
                                }, delayMs);
                            }
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
                    if(retry < maxRetry && bootMode) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                executeAsynchronousAction(mCurrentAction);
                            }
                        }, delayMs);
                        retry++;
                    } else {
                        mTimer.stop();
                        writeFileButton.setClickable(false);
                        writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state));
                        mailboxStatusTextView.setText(R.string.mailbox_disabled);
                        mailboxStatusTextView.setTextColor(getResources().getColor(R.color.red));
                        bootMode = false;
                        ftmStatusProgressTextView.setText(R.string.failed);
                        Toast.makeText(NfcBootloaderActivity.this, R.string.command_failed, Toast.LENGTH_LONG).show();
                    }
                    break;
                case NO_RESPONSE:
                    if(retry < maxRetry && bootMode) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                executeAsynchronousAction(mCurrentAction);
                            }
                        }, delayMs);
                        retry++;
                    } else {
                        mTimer.stop();
                        writeFileButton.setClickable(false);
                        writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state));
                        mailboxStatusTextView.setText(R.string.mailbox_disabled);
                        mailboxStatusTextView.setTextColor(getResources().getColor(R.color.red));
                        bootMode = false;
                        Toast.makeText(NfcBootloaderActivity.this, R.string.no_response_received, Toast.LENGTH_LONG).show();
                    }
                    break;
                case TAG_NOT_IN_THE_FIELD:
                    if(retry < maxRetry && bootMode) {
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                executeAsynchronousAction(mCurrentAction);
                            }
                        }, delayMs);
                        retry++;
                    } else {
                        mTimer.stop();
                        writeFileButton.setClickable(false);
                        writeFileButton.setBackgroundTintList(getColorStateList(R.color.button_color_state));
                        mailboxStatusTextView.setText(R.string.mailbox_disabled);
                        mailboxStatusTextView.setTextColor(getResources().getColor(R.color.red));
                        bootMode = false;
                        Toast.makeText(NfcBootloaderActivity.this, R.string.tag_not_in_the_field, Toast.LENGTH_LONG).show();
                    }
                    break;
            }
        }
    }

    public static byte[] hexToByteArray(String hex) {
        if(hex.length()%2 != 0) {
            hex = hex + "0";
        }
        byte[] b = new byte[hex.length() / 2];
        for(int i = 0; i < b.length; i++) {
            int index = i * 2;
            int v = Integer.parseUnsignedInt(hex.substring(index, index + 2), 16);
            b[i] = (byte) v;
        }
        return b;
    }

//    public void sendProtocol(){
//        bootloaderData = hexToByteArray(bootloaderString);
//        if (mNbrOfBytesToSend <= maxDataLength) {
////                            mDataReceived = mFtmCommands.sendCmdAndWaitForCompletion(FTM_CMD_SEND_DATA, bootloaderData, false, false, NfcBootloaderActivity.this, FtmCommands.DEFAULT_FTM_TIME_OUT_IN_MS);
//            mFtmResponse = myST25DVTag.writeMailboxMessage(startBoot);
//            readData = myST25DVTag.fastReadDynConfig(MB_CTRL_Dyn);
//            mFtmResponse = myST25DVTag.writeMailboxMessage(bootloaderData);
//            readData = myST25DVTag.fastReadDynConfig(MB_CTRL_Dyn);
//            mFtmResponse = myST25DVTag.writeMailboxMessage(endBoot);
//        } else {
//            int NbrOfBytesSend = 0;
//            int numberOfRetry = 0;
//            int mTimeOutDurationMs = 10000;
//            int mWaitTimeMs = 200;
//            CountDownTimer countDownTimer = new CountDownTimer(mTimeOutDurationMs, mWaitTimeMs) {
//                @Override
//                public void onTick(long l) {
//                    mFtmState = mCurState;
//                }
//
//                @Override
//                public void onFinish() {
//                    bootloaderTransfer = false;
//                }
//            };
//
//            countDownTimer.start();
//            while(bootloaderTransfer) {
//                switch (mFtmState) {
//                    case IDLE:
//                        break;
//                    case START_BOOT:
//                        startBoot[2] = (byte) mNbrOfPacketsToSend;
//                        mFtmResponse = myST25DVTag.writeMailboxMessage(startBoot);
//                        //Success Next go SEND
//                        if(mFtmResponse == 0) {
//                            mNextState = State.SEND_BOOT;
//                        }
//                        mPrevState = mFtmState;
//                        mCurState = State.WAIT_FOR_FTM;
//                        mFtmState = State.IDLE;
//                        break;
//                    case SEND_BOOT:
//                        if(NbrOfBytesSend < mNbrOfBytesToSend) {
//                            for(int j = 0; j < maxDataLength; j++) {
//                                sendData[j] = bootloaderData[NbrOfBytesSend + j];
//                            }
//                            mFtmResponse = myST25DVTag.writeMailboxMessage(sendData);
//                            if(mFtmResponse == 0) {
//                                mNextState = State.SEND_BOOT;
//                                NbrOfBytesSend += maxDataLength;
//                            }
//
//                        } else {
//                            mNextState = State.END_BOOT;
//                        }
//                        mPrevState = mFtmState;
//                        mCurState = State.WAIT_FOR_FTM;
//                        mFtmState = State.IDLE;
//                        break;
//                    case END_BOOT:
//                        endBoot[2] = (byte) mNbrOfPacketsToSend;
//                        mFtmResponse = myST25DVTag.writeMailboxMessage(endBoot);
//                        if(mFtmResponse == 0) {
//                            bootloaderTransfer = false;
//                        }
//                        mPrevState = mFtmState;
//                        mCurState = State.WAIT_FOR_FTM;
//                        mFtmState = State.IDLE;
//                        break;
//                    case WAIT_FOR_FTM:
//                        readData = myST25DVTag.fastReadDynConfig(MB_CTRL_Dyn);
//                        // Success go Next, Fail go Retry
//                        if(mFtmResponse == 0) {
//                            mCurState = mNextState;
//                        } else {
//                            //Fail 5 times, go Prev
//                            if(numberOfRetry > 5) {
//                                mCurState = mPrevState;
//                                numberOfRetry = 0;
//                            }
//                            numberOfRetry++;
//                        }
//                        mFtmState = State.IDLE;
//                        break;
//                    default:
//                        break;
//
//                }
//            }
//
//        }
//    }

}