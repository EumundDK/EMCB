
package com.example.emcb.BLE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emcb.R;

import java.util.ArrayList;
import java.util.Arrays;

import static android.widget.Toast.LENGTH_SHORT;

public class MyListViewAdapter extends RecyclerView.Adapter<MyListViewAdapter.ViewHolder>{
    private static final int offStatus = 0;
    private static final int onStatus = 16;
    private static final int selectStatusCheck = 8;
    private static final int onOffStatusCheck = 16;

    private static final int onCmd = 241;
    private static final int offCmd = 242;
    private static final int selectCmd = 248;
    private static final int currentCmd = 250;
    private static final int cutoffCmd = 251;

    private LayoutInflater mInflater;
    private Context mContext;
    private ArrayList<DeviceData> mDeviceDataList;
    private ArrayList<Byte> mDeviceEEPData;

    private int itemSelectedCard = RecyclerView.NO_POSITION;
    private int previousItemSelectCard = RecyclerView.NO_POSITION;
    private final byte[] myCommand = {0x08, 0x00, 0x03};
//    private final byte[] myCommand2 = {0x08, 0x00, 0x03, 0x00, 0x00};
    private byte[] myEEPCommand = {0x08, 0x00, 0x4E, 0x00, 0x00};
    private boolean temp = true;

    CountDownTimer countDownTimer = new CountDownTimer(5000, 1000) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            temp = true;
        }
    };

    MyListViewAdapter(Context context, ArrayList<DeviceData> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mDeviceDataList = data;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.data_info_item_list3, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        DeviceData readerData = mDeviceDataList.get(position);
        holder.mTagName.setText(readerData.getName());
        if(checkDuplicate(holder, readerData, position)) {
            holder.mCurrent.setText(readerData.getCurrent());
            checkStatus(holder, readerData, position);
        }
    }

    @Override
    public int getItemCount() {
        return mDeviceDataList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView mTagName;
        ImageView mStatusOff;
        TextView mDeviceName;
        TextView mCurrent;
        CardView mCardView;
//        ImageButton mButton;
        Button mOnSwitch;
        Button mOffSwitch;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTagName = itemView.findViewById(R.id.itemNameTextView);
            mStatusOff = itemView.findViewById(R.id.itemStatusOnOff);
            mDeviceName = itemView.findViewById(R.id.itemDeviceName);
            mCurrent = itemView.findViewById(R.id.itemCurrentTextView);
            mCardView = itemView.findViewById(R.id.card_view);
//            mButton = itemView.findViewById(R.id.onOffButton);
            mOnSwitch = itemView.findViewById(R.id.onButton);
            mOffSwitch = itemView.findViewById(R.id.offButton);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemSelectedCard = getLayoutPosition();
                    notifyItemChanged(itemSelectedCard);
                    deviceSelection(itemSelectedCard);
                }
            });

            mOnSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int itemSelectedSwitch = getLayoutPosition();
                    byte itemName = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getName());
                    myCommand[1] = itemName;
                    myCommand[2] = (byte) onCmd;
                    DeviceDataTabActivity.writeCharacteristicData(myCommand);
                    Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " ON", LENGTH_SHORT).show();
                    temp = false;
                    countDownTimer.start();
                }
            });

            mOffSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int itemSelectedSwitch = getLayoutPosition();
                    byte itemName = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getName());
                    myCommand[1] = itemName;
                    myCommand[2] = (byte) offCmd;
                    DeviceDataTabActivity.writeCharacteristicData(myCommand);
                    Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " OFF", LENGTH_SHORT).show();
                    temp = false;
                    countDownTimer.start();
                }
            });
        }
    }

    DeviceData getItem(int id) {
        return mDeviceDataList.get(id);
    }

    public boolean checkDuplicate(MyListViewAdapter.ViewHolder holder, DeviceData readerData, int position) {
        if(readerData.getDuplicate() == 0) { //NO DATA BLANK OUT
            holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.bright_grey));
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_grey_24));
            holder.mCurrent.setText(R.string.blank);
            holder.itemView.setClickable(false);
            holder.mOnSwitch.setVisibility(View.INVISIBLE);
            holder.mOffSwitch.setVisibility(View.INVISIBLE);
            return false;
        } else if(readerData.getDuplicate() == 1) {
            if(position != previousItemSelectCard) { //GOOD DATA NOT PREVIOUSLY SELECTED
                holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.white));
            }
            holder.itemView.setClickable(true);
            holder.mOnSwitch.setVisibility(View.VISIBLE);
            holder.mOffSwitch.setVisibility(View.VISIBLE);
            return true;
        } else { // DUPE DATA ERROR
            holder.mCurrent.setText("Err");
            holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.light_red));
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_grey_24));
            holder.itemView.setClickable(true);
            holder.mOnSwitch.setVisibility(View.INVISIBLE);
            holder.mOffSwitch.setVisibility(View.INVISIBLE);
            return true;
        }
    }

    public void checkStatus(MyListViewAdapter.ViewHolder holder, DeviceData readerData, int position) {
        int itemStatus = Integer.parseUnsignedInt(readerData.getStatus(), 16);
        if ((itemStatus & onOffStatusCheck) == onStatus) {
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_green_24));
            holder.mOnSwitch.setBackgroundTintList(mInflater.getContext().getColorStateList(R.color.button_color_on));
            holder.mOffSwitch.setBackgroundTintList(mInflater.getContext().getColorStateList(R.color.button_color_default));

        } else if ((itemStatus & onOffStatusCheck) == offStatus) {
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_red_24));

            holder.mOffSwitch.setBackgroundTintList(mInflater.getContext().getColorStateList(R.color.button_color_off));
            holder.mOnSwitch.setBackgroundTintList(mInflater.getContext().getColorStateList(R.color.button_color_default));
        }
    }

    public void updateDeviceDataList(ArrayList<DeviceData> newDeviceDataList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new BleDiffUtilCallbacks(mDeviceDataList, newDeviceDataList));
        diffResult.dispatchUpdatesTo(this);
        mDeviceDataList = newDeviceDataList;
    }

    public void updateDeviceData(int position) {
        this.notifyItemChanged(position);
    }

    public void refreshDeviceData() {
        this.notifyDataSetChanged();
    }

//    private void deviceSelection() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//        String[] itemList;
//        itemList = new String[] {"Current", "Cutoff Period"};
//        builder.setTitle("Select Settings")
//                .setItems(itemList, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialogInterface, int i) {
//                        deviceSettingDialog(i);
//                    }
//                });
//        builder.show();
//    }

    private void deviceSelection(int selectNo) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        LayoutInflater inflater = mInflater;
        View content = inflater.inflate(R.layout.dialog_device_setting, null);
        builder.setView(content).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                });
        TextView dialogCurrent = content.findViewById(R.id.dialogCurrent);
        dialogCurrent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                currentSettingDialog();
            }
        });
        TextView dialogCutoffPeriod = content.findViewById(R.id.dialogCutoffPeriod);
        dialogCutoffPeriod.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cutOffPeriodDialog();
            }
        });
        TextView dialogOnOff = content.findViewById(R.id.dialogOnOff);
        TextView dialogAutoReconnect = content.findViewById(R.id.dialogAutoReconnect);
        TextView dialogOwnerName = content.findViewById(R.id.dialogOwnerName);
        dialogCurrent.setText(String.valueOf(mDeviceDataList.get(selectNo).getCurrentSetting()));
        dialogCutoffPeriod.setText(Integer.toString(mDeviceDataList.get(selectNo).getCutoffPeriod()));
        dialogOnOff.setText(Integer.toString(mDeviceDataList.get(selectNo).getOnOffSetting()));
        dialogAutoReconnect.setText(Integer.toString(mDeviceDataList.get(selectNo).getAutoReconnect()));
        dialogOwnerName.setText(mDeviceDataList.get(selectNo).getOwnerName());
        builder.create();
        builder.show();
    }

    private void currentSettingDialog() {
        AlertDialog.Builder builderCurrent = new AlertDialog.Builder(mContext);
        EditText inputCurrent = new EditText(mContext);
        builderCurrent.setTitle("Current Settings");
        builderCurrent.setMessage("Enter Current Settings(1.0 ~ 40.0):");
        builderCurrent.setView(inputCurrent);
        builderCurrent.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int dataInput;
                dataInput = (int) (Double.parseDouble(inputCurrent.getText().toString()) * 10);
                myEEPCommand[1] = Byte.parseByte(mDeviceDataList.get(itemSelectedCard).getName());;
                myEEPCommand[2] = (byte) currentCmd;
                myEEPCommand[3] = (byte) (dataInput % 256);
                myEEPCommand[4] = (byte) (dataInput / 256);
                DeviceDataTabActivity.writeCharacteristicData(myEEPCommand);
            }
        });
        builderCurrent.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builderCurrent.show();
    }

    private void cutOffPeriodDialog() {
        AlertDialog.Builder builderCutOff = new AlertDialog.Builder(mContext);
        EditText inputCutOff = new EditText(mContext);
        builderCutOff.setTitle("Cut-off Period");
        builderCutOff.setMessage("Enter Cut-off Period (20 ~ 300):");
        builderCutOff.setView(inputCutOff);
        builderCutOff.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                int dataInput;
                dataInput= Integer.parseInt(inputCutOff.getText().toString());
                myEEPCommand[1] = Byte.parseByte(mDeviceDataList.get(itemSelectedCard).getName());;
                myEEPCommand[2] = (byte) cutoffCmd;
                myEEPCommand[3] = (byte) (dataInput % 256);
                myEEPCommand[4] = (byte) (dataInput / 256);
                DeviceDataTabActivity.writeCharacteristicData(myEEPCommand);
            }
        });
        builderCutOff.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        builderCutOff.show();
    }

//    private void deviceSettingDialog(int selection) {
//        AlertDialog.Builder itemDialog = new AlertDialog.Builder(mContext);
//        TextView input = new TextView(mContext);
//
//        itemDialog.setTitle(mDeviceDataList.get(itemSelectedCard).getName());
//        itemDialog.setView(input);
//        itemDialog.setPositiveButton("OK", (dialog, which) -> {
//            dialog.dismiss();
//            int dataInput;
//            if(selection == 0) {
//                dataInput = (int) (Double.parseDouble(input.getText().toString()) * 10);
//            } else {
//                dataInput= Integer.parseInt(input.getText().toString());
//            }
//            myCommand2[1] = Byte.parseByte(mDeviceDataList.get(itemSelectedCard).getName());
//            myCommand2[2] = (byte) (78 + selection);
//            myCommand2[3] = (byte) (dataInput % 256);
//            myCommand2[4] = (byte) (dataInput / 256);
//            DeviceDataTabActivity.writeCharacteristicData(myCommand);
//        });
//        itemDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
//        itemDialog.show();
//
//    }
}
