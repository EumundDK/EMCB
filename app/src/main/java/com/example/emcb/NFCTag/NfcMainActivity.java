package com.example.emcb.NFCTag;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.ColorStateList;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.emcb.BLE.BleMainActivity;
import com.example.emcb.MainActivity;
import com.example.emcb.R;
import com.st.st25sdk.NFCTag;
import com.st.st25sdk.STException;
import com.st.st25sdk.TagHelper;
import com.st.st25sdk.type5.st25dv.ST25DVTag;

import java.lang.reflect.Array;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static com.example.emcb.NFCTag.TagDiscovery.TAG;

public class NfcMainActivity extends AppCompatActivity implements TagDiscovery.onTagDiscoveryCompletedListener {

    static final int CURRENT_SETTING = 0;
    static final int SLAVE_ID = 2;
    static final int CUTOFF_PERIOD = 4;
    static final int ONOFF_SETTING = 6;
    static final int AUTO_RECONNECT = 7;
    static final int OWNER_NAME = 8;

    static final int NFC_READ = 0;
    static final int NFC_WRITE = 1;

    private NfcAdapter mNfcAdapter;

    private NFCTag mNfcTag;
//    private int mNfcScanMode;

    private TextView uidTextView;
//    private Switch nfcSwitch;
//    private TextView nfcModeTextView;
    private EditText mCurrentSettingEdit;
    private EditText mSlaveIdEdit;
    private EditText mCutoffPeriodEdit;
    private EditText mOnOffSettingEdit;
    private EditText mAutoReconnectEdit;
    private EditText mOwnerNameEdit;
    private Button mWriteMemoryBtn;
//    private Button mReadMemoryBtn;

    private CheckBox mCurrentSettingCheckBox;
    private CheckBox mSlaveIdCheckBox;
    private CheckBox mCutoffPeriodCheckBox;
    private CheckBox mOnOffSettingCheckBox;
    private CheckBox mAutoReconnectCheckBox;
    private CheckBox mOwnerNameCheckBox;

//    private boolean switchState;
    private byte[] writeData;
    private byte[] readData;
    private int dataLength;
    private int currentSetting;
    private Double currentConvert;
    private int slaveId;
    private int cutoffPeriod;
    private int onOffSetting;
    private int autoReconnect;
    private byte[] ownerName;
    private String ownerNameText;

    private ST25DVTag mST25DVTag;

    private int currentSlaveID;
    private int checkSlaveID;

    enum Action {
        READ_TAG_MEMORY,
        WRITE_TAG_MEMORY
    }

    enum ActionStatus {
        ACTION_SUCCESSFUL,
        ACTION_FAILED,
        TAG_NOT_IN_THE_FIELD
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nfc_main);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        uidTextView = findViewById(R.id.uidTextView);
//        nfcSwitch = findViewById(R.id.nfcSwitch);
//        nfcModeTextView = findViewById(R.id.nfc_mode_change);
//        nfcSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
//                switchState = b;
//                if(switchState) {
//                    nfcSwitch.setText("NFC Tap Mode Auto");
//                    nfcModeTextView.setVisibility(View.VISIBLE);
//                    mWriteMemoryBtn.setClickable(false);
//                    mReadMemoryBtn.setClickable(false);
//                    mWriteMemoryBtn.setBackgroundTintList(getColorStateList(R.color.button_color_state));
//                    mReadMemoryBtn.setBackgroundTintList(getColorStateList(R.color.button_color_state));
//
//                } else {
//                    nfcSwitch.setText("NFC Tap Mode Manual");
//                    nfcModeTextView.setVisibility(View.INVISIBLE);
//                    mWriteMemoryBtn.setClickable(true);
//                    mReadMemoryBtn.setClickable(true);
//                    mWriteMemoryBtn.setBackgroundTintList(getColorStateList(R.color.button_color_state_normal));
//                    mReadMemoryBtn.setBackgroundTintList(getColorStateList(R.color.button_color_state_normal));
//                }
//                invalidateOptionsMenu();
//            }
//        });
        mCurrentSettingEdit = (EditText) findViewById(R.id.currentSettingEdit);
        mSlaveIdEdit = (EditText) findViewById(R.id.slaveIdEdit);
        mCutoffPeriodEdit = (EditText) findViewById(R.id.cutOffPeriodEdit);
        mOnOffSettingEdit = (EditText) findViewById(R.id.onOffSettingEdit);
        mAutoReconnectEdit = (EditText) findViewById(R.id.autoReconnectEdit);
        mOwnerNameEdit = (EditText) findViewById(R.id.ownerNameEdit);

        mCurrentSettingCheckBox = (CheckBox) findViewById(R.id.currentSettingCheck);
        mCurrentSettingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mCurrentSettingEdit.setEnabled(false);
                    mCurrentSettingCheckBox.setText(mCurrentSettingEdit.getText());
                } else {
                    mCurrentSettingEdit.setEnabled(true);
                }
            }
        });

        mSlaveIdCheckBox = (CheckBox) findViewById(R.id.slaveIdCheck);
        mSlaveIdCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mSlaveIdEdit.setEnabled(false);
                    mSlaveIdCheckBox.setText(mSlaveIdEdit.getText());
                } else {
                    mSlaveIdEdit.setEnabled(true);
                }
            }
        });
        mCutoffPeriodCheckBox = (CheckBox) findViewById(R.id.cutOffPeriodCheck);
        mCutoffPeriodCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mCutoffPeriodEdit.setEnabled(false);
                    mCutoffPeriodCheckBox.setText(mCutoffPeriodEdit.getText());
                } else {
                    mCutoffPeriodEdit.setEnabled(true);
                }
            }
        });
        mOnOffSettingCheckBox = (CheckBox) findViewById(R.id.onOffSettingCheck);
        mOnOffSettingCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mOnOffSettingEdit.setEnabled(false);
                    mOnOffSettingCheckBox.setText(mOnOffSettingEdit.getText());
                } else {
                    mOnOffSettingEdit.setEnabled(true);
                }
            }
        });
        mAutoReconnectCheckBox = (CheckBox) findViewById(R.id.autoReconnectCheck);
        mAutoReconnectCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mAutoReconnectEdit.setEnabled(false);
                    mAutoReconnectCheckBox.setText(mAutoReconnectEdit.getText());
                } else {
                    mAutoReconnectEdit.setEnabled(true);
                }
            }
        });
        mOwnerNameCheckBox = (CheckBox) findViewById(R.id.ownerNameCheck);
        mOwnerNameCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b) {
                    mOwnerNameEdit.setEnabled(false);
                    mOwnerNameCheckBox.setText(mOwnerNameEdit.getText());
                } else {
                    mOwnerNameEdit.setEnabled(true);
                }
            }
        });

        mWriteMemoryBtn = findViewById(R.id.writeMemoryBtn);
        mWriteMemoryBtn.setOnClickListener(view ->  {
            if(mNfcTag != null) {
                executeAsynchronousAction(Action.WRITE_TAG_MEMORY);
            } else {
                clearNfcStatus();
                Toast.makeText(NfcMainActivity.this, "Action failed!", Toast.LENGTH_LONG).show();
            }
        });
//        mReadMemoryBtn = findViewById(R.id.readMemoryBtn);
//        mReadMemoryBtn.setOnClickListener(view -> {
//            if(mNfcTag != null) {
//                executeAsynchronousAction(Action.READ_TAG_MEMORY);
//            } else {
//                Toast.makeText(NfcMainActivity.this, "Action failed!", Toast.LENGTH_LONG).show();
//            }
//        });
        Button mBootloaderBtn = findViewById(R.id.bootloaderBtn);
        mBootloaderBtn.setOnClickListener(view -> {
            Intent intent = new Intent(this, NfcBootloaderActivity.class);
            intent.setAction(Intent.ACTION_DEFAULT);
            startActivity(intent);
        });
        clearNfcStatus();
//        nfcSwitch.setChecked(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Check if if this phone has NFC hardware
        if (mNfcAdapter == null) {
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            // set title
            alertDialogBuilder.setTitle("Warning!");
            // set dialog message
            alertDialogBuilder
                    .setMessage("This phone doesn't have NFC hardware!")
                    .setCancelable(true)
                    .setPositiveButton("Leave", (dialog, id) -> {
                        dialog.cancel();
                        finish();
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
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcFilters, nfcTechLists);
        }
        // The current activity can be resumed for several reasons (NFC tag tapped is one of them).
        // Check what was the reason which triggered the resume of current application
        Intent intent = getIntent();
        String action = intent.getAction();
        if (action.equals(NfcAdapter.ACTION_NDEF_DISCOVERED) ||
                action.equals(NfcAdapter.ACTION_TECH_DISCOVERED) ||
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
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
//        getMenuInflater().inflate(R.menu.nfc_scan_menu, menu);
//        if(switchState) {
//            if (mNfcScanMode == NFC_WRITE) {
//                menu.findItem(R.id.menu_write).setVisible(false);
//                menu.findItem(R.id.menu_read).setVisible(true);
//                nfcModeTextView.setText("Writing");
//            } else {
//                menu.findItem(R.id.menu_write).setVisible(true);
//                menu.findItem(R.id.menu_read).setVisible(false);
//                nfcModeTextView.setText("Reading");
//            }
//        } else {
//            menu.findItem(R.id.menu_read).setVisible(false);
//            menu.findItem(R.id.menu_write).setVisible(false);
//        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
//            case R.id.menu_write:
//                mNfcScanMode = NFC_WRITE;
//                invalidateOptionsMenu();
//                return true;
//            case R.id.menu_read:
//                mNfcScanMode = NFC_READ;
//                invalidateOptionsMenu();
//                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // onResume() gets called after this to handle the intent
        setIntent(intent);
    }

    @Override
    public void onTagDiscoveryCompleted(NFCTag nfcTag, TagHelper.ProductID productId, STException error) {
        if (error != null) {
            clearNfcStatus();
            Toast.makeText(getApplication(), "Error while reading the tag: " + error.toString(), Toast.LENGTH_LONG).show();
            return;
        }
        if (nfcTag != null) {
            mNfcTag = nfcTag;
            mST25DVTag = (ST25DVTag) mNfcTag;
            try {
                String uidString = nfcTag.getUidString();
                uidTextView.setText(uidString);
//                if(switchState) {
//                    if(mNfcScanMode == NFC_WRITE) {
//                        executeAsynchronousAction(Action.WRITE_TAG_MEMORY);
//                    } else {
                        executeAsynchronousAction(Action.READ_TAG_MEMORY);
//                    }
//                }
            } catch (STException e) {
                e.printStackTrace();
                clearNfcStatus();
                Toast.makeText(this, "Discovery successful but failed to read the tag!", Toast.LENGTH_LONG).show();
            }
        } else {
            clearNfcStatus();
            Toast.makeText(this, "Tag discovery failed!", Toast.LENGTH_LONG).show();
        }
    }

    private void executeAsynchronousAction(Action action) {
        Log.d(TAG, "Starting background action " + action);
        new myAsyncTask(action).execute();
    }

    private class myAsyncTask extends AsyncTask<Void, Void, ActionStatus> {
        Action mAction;

        public myAsyncTask(Action action) {
            mAction = action;
        }

        @Override
        protected ActionStatus doInBackground(Void... param) {
            ActionStatus result;
            Charset charset = StandardCharsets.US_ASCII;
            writeData = new byte[24];
            readData = null;
            dataLength = 0;
            currentSetting = 0;
            slaveId = 0;
            cutoffPeriod = 0;
            onOffSetting = 0;
            autoReconnect = 0;
            int startAddress = 0;
            int readLength = 8;
            try {
                switch (mAction) {
                    case READ_TAG_MEMORY:
                        readData = mNfcTag.readBytes(startAddress,readLength);
                        if(readData != null) {
                            dataLength = readData.length;
                        }
                        if(dataLength == readLength) {
                            currentConvert = (double) (((readData[0] & 0xFF) + (readData[1] & 0xFF) * 256)) / 10.0;
                            slaveId = (readData[2] & 0xFF) + ((readData[3] & 0xFF) * 256);
                            cutoffPeriod = (readData[4] & 0xFF) + ((readData[5] & 0xFF) * 256);
                            onOffSetting = readData[6];
                            autoReconnect = readData[7];
                            ownerName = mNfcTag.readBytes(OWNER_NAME,16);
                            ownerNameText = new String(ownerName, StandardCharsets.UTF_8);
                        }
                        // If we get to this point, it means that no STException occured so the action was successful
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    case WRITE_TAG_MEMORY:
                        if(mCurrentSettingEdit.isEnabled()) {
                            currentConvert = Double.parseDouble(mCurrentSettingEdit.getText().toString());
                        } else {
                            currentConvert =  Double.parseDouble(mCurrentSettingCheckBox.getText().toString());
                        }
                        currentSetting = (int) (currentConvert * 10);

                        if(currentSetting < 256) {
                            writeData[CURRENT_SETTING] = (byte) currentSetting;
                            writeData[CURRENT_SETTING + 1] = 0;
                        } else {
                            writeData[CURRENT_SETTING] = (byte) (currentSetting % 256);
                            writeData[CURRENT_SETTING + 1] = (byte) (currentSetting / 256);
                        }

                        if(mSlaveIdEdit.isEnabled()) {
                            slaveId = Integer.parseInt(mSlaveIdEdit.getText().toString());
                        } else {
                            slaveId = Integer.parseInt(mSlaveIdCheckBox.getText().toString());
                        }
                        writeData[SLAVE_ID] = (byte) slaveId;

                        if(mCutoffPeriodEdit.isEnabled()) {
                            cutoffPeriod = Integer.parseInt(mCutoffPeriodEdit.getText().toString());
                        } else {
                            cutoffPeriod = Integer.parseInt(mCutoffPeriodCheckBox.getText().toString());
                        }

                        if(cutoffPeriod < 256) {
                            writeData[CUTOFF_PERIOD] = (byte) cutoffPeriod;
                            writeData[CUTOFF_PERIOD + 1] = 0;
                        } else {
                            writeData[CUTOFF_PERIOD] = (byte) (cutoffPeriod % 256);
                            writeData[CUTOFF_PERIOD + 1] = (byte) (cutoffPeriod / 256);
                        }

                        if(mOnOffSettingEdit.isEnabled()) {
                            onOffSetting = Integer.parseInt(mOnOffSettingEdit.getText().toString());
                        } else {
                            onOffSetting = Integer.parseInt(mOnOffSettingCheckBox.getText().toString());
                        }
                        writeData[ONOFF_SETTING] = (byte) onOffSetting;

                        if(mAutoReconnectEdit.isEnabled()) {
                            autoReconnect = Integer.parseInt(mAutoReconnectEdit.getText().toString());
                        } else {
                            autoReconnect = Integer.parseInt(mAutoReconnectCheckBox.getText().toString());
                        }
                        writeData[AUTO_RECONNECT] = (byte) autoReconnect;

                        if(mOwnerNameEdit.isEnabled()) {
                            ownerName = charset.encode(mOwnerNameEdit.getText().toString()).array();
                        } else {
                            ownerName = charset.encode(mOwnerNameCheckBox.getText().toString()).array();
                        }

                        if(mST25DVTag.isMailboxEnabled(true)) {
                            mST25DVTag.disableMailbox();
                        }
                        mNfcTag.writeBytes(startAddress, writeData);
                        mNfcTag.writeBytes(OWNER_NAME, ownerName);
                        mST25DVTag.enableMailbox();
                        // If we get to this point, it means that no STException occured so the action was successful
                        result = ActionStatus.ACTION_SUCCESSFUL;
                        break;

                    default:
                        result = ActionStatus.ACTION_FAILED;
                        break;
                }

            } catch (STException e) {
                switch (e.getError()) {
                    case TAG_NOT_IN_THE_FIELD:
                        result = ActionStatus.TAG_NOT_IN_THE_FIELD;
                        break;

                    default:
                        e.printStackTrace();
                        result = ActionStatus.ACTION_FAILED;
                        break;
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(ActionStatus actionStatus) {

            switch(actionStatus) {
                case ACTION_SUCCESSFUL:
                    switch (mAction) {
                        case READ_TAG_MEMORY:
                            mWriteMemoryBtn.setClickable(true);
                            mWriteMemoryBtn.setBackgroundTintList(getColorStateList(R.color.button_color_state_normal));
                            mCurrentSettingEdit.setText(String.valueOf(currentConvert));
                            mSlaveIdEdit.setText(String.valueOf(slaveId));
                            mCutoffPeriodEdit.setText(String.valueOf(cutoffPeriod));
                            mOnOffSettingEdit.setText(String.valueOf(onOffSetting));
                            mAutoReconnectEdit.setText(String.valueOf(autoReconnect));
                            if(ownerName[0] == 0 || ownerName[0] == 0xFF) {
                                mOwnerNameEdit.getText().clear();
                            }
                            Toast.makeText(NfcMainActivity.this, "Read successful", Toast.LENGTH_LONG).show();
                            break;
                        case WRITE_TAG_MEMORY:
                            currentSlaveID = Integer.parseInt(mSlaveIdEdit.getText().toString());
                            if((currentSlaveID < 100)) {
                                currentSlaveID = currentSlaveID + 1; //Increment by 1 when write OK
                            } else if ((currentSlaveID > 100)) {
                                currentSlaveID = 1;
                            }
                            mSlaveIdEdit.setText(String.valueOf(currentSlaveID));

                            Toast.makeText(NfcMainActivity.this, "Write successful", Toast.LENGTH_LONG).show();
                            break;
                    }
                    break;

                case ACTION_FAILED:
                    clearNfcStatus();
                    Toast.makeText(NfcMainActivity.this, "Action failed!", Toast.LENGTH_LONG).show();
                    break;

                case TAG_NOT_IN_THE_FIELD:
                    clearNfcStatus();
                    Toast.makeText(NfcMainActivity.this, "Tag not in the field!", Toast.LENGTH_LONG).show();
                    break;
            }

        }
    }

    private void clearNfcStatus() {
        uidTextView.setText(R.string.uidTextViewHint);
        mWriteMemoryBtn.setClickable(false);
        mWriteMemoryBtn.setBackgroundTintList(getColorStateList(R.color.button_color_state));
    }


}