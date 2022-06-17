package com.example.emcb.BLE;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emcb.R;

import java.util.ArrayList;
import java.util.Arrays;

import static android.widget.Toast.LENGTH_SHORT;

public class MyGridViewAdapter extends RecyclerView.Adapter<MyGridViewAdapter.ViewHolder>{
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

    private int itemSelectedCard = RecyclerView.NO_POSITION;
    private int previousItemSelectCard = RecyclerView.NO_POSITION;
    private final byte[] myCommand = {0x08, 0x00, 0x03};
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

    // data is passed into the constructor
    MyGridViewAdapter(Context context, ArrayList<DeviceData> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mDeviceDataList = data;
        this.mContext = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.data_info_item_grid, parent, false);
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
        TextView mCurrent;
        TextView mCurrentSymbolTextView;
        ImageButton mSwitch;
//        ImageButton mOnSwitch;
//        ImageButton mOffSwitch;
        CardView mCardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTagName = itemView.findViewById(R.id.itemNameTextView);
            mStatusOff = itemView.findViewById(R.id.itemStatusOnOff);
            mCurrent = itemView.findViewById(R.id.itemCurrentTextView);
            mCurrentSymbolTextView = itemView.findViewById(R.id.currentSymbolTextView);
            mSwitch = itemView.findViewById(R.id.onOffSwitch);
//            mOnSwitch = itemView.findViewById(R.id.onSwitch);
//            mOffSwitch = itemView.findViewById(R.id.offSwitch);
            mCardView = itemView.findViewById(R.id.card_view);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemSelectedCard = getLayoutPosition();
                    myCommand[1] = Byte.parseByte(mDeviceDataList.get(itemSelectedCard).getName());
                    myCommand[2] = (byte) selectCmd;
                    DeviceDataTabActivity.writeCharacteristicData(myCommand);
                    Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " Selected", LENGTH_SHORT).show();
                    notifyItemChanged(itemSelectedCard);
                    temp = false;
                    countDownTimer.start();
                }
            });

//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    itemSelectedCard = getLayoutPosition();
//                    notifyItemChanged(itemSelectedCard);
//                    controlDialog(itemSelectedCard);
//                }
//            });

            mSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int itemSelectedSwitch = getLayoutPosition();
                    byte itemStatus = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getStatus());
                    byte itemName = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getName());
                    if((itemStatus & onOffStatusCheck) == offStatus) {
                        myCommand[1] = itemName;
                        myCommand[2] = (byte) onCmd;
                        DeviceDataTabActivity.writeCharacteristicData(myCommand);
                        Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " ON", LENGTH_SHORT).show();
                        mDeviceDataList.get(itemSelectedSwitch).setStatus(String.valueOf(onStatus));
                        notifyItemChanged(itemSelectedSwitch);

                    }

                    if((itemStatus & onOffStatusCheck) == onStatus) {
                        myCommand[1] = itemName;
                        myCommand[2] = (byte) offCmd;
                        DeviceDataTabActivity.writeCharacteristicData(myCommand);
                        Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " OFF", LENGTH_SHORT).show();
                        mDeviceDataList.get(itemSelectedSwitch).setStatus(String.valueOf(offStatus));
                        notifyItemChanged(itemSelectedSwitch);
                    }
                    temp = false;
                    countDownTimer.start();
                }
            });

//            mOnSwitch.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    int itemSelectedSwitch = getLayoutPosition();
//                    byte itemName = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getName());
//                    myCommand[1] = itemName;
//                    myCommand[2] = (byte) onCmd;
//                    DeviceDataTabActivity.writeCharacteristicData(myCommand);
//                    Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " ON", LENGTH_SHORT).show();
//                    temp = false;
//                    countDownTimer.start();
//                }
//            });
//
//            mOffSwitch.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    int itemSelectedSwitch = getLayoutPosition();
//                    byte itemName = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getName());
//                    myCommand[1] = itemName;
//                    myCommand[2] = (byte) offCmd;
//                    DeviceDataTabActivity.writeCharacteristicData(myCommand);
//                    Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " OFF", LENGTH_SHORT).show();
//                    temp = false;
//                    countDownTimer.start();
//                }
//            });
        }
    }

    // convenience method for getting data at click position
    DeviceData getItem(int id) {
        return mDeviceDataList.get(id);
    }

    public boolean checkDuplicate(ViewHolder holder, DeviceData readerData, int position) {
        if(readerData.getDuplicate() == 0) { //NO DATA BLANK OUT
            holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.bright_grey));
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_grey_12));
            holder.mCurrent.setText(R.string.blank);
            holder.itemView.setClickable(false);
            holder.mSwitch.setClickable(false);
            return false;
        } else if(readerData.getDuplicate() == 1) {
            if(position != previousItemSelectCard) { //GOOD DATA NOT PREVIOUSLY SELECTED
                holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.white));
            }
            holder.mCurrentSymbolTextView.setVisibility(View.VISIBLE);
            holder.itemView.setClickable(true);
            holder.mSwitch.setClickable(true);
            return true;
        } else { // DUPE DATA ERROR
            holder.mCurrent.setText("Err");
            holder.mCurrentSymbolTextView.setVisibility(View.INVISIBLE);
            holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.light_red));
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_grey_12));
            holder.itemView.setClickable(true);
            holder.mSwitch.setClickable(false);
            return true;
        }
    }

    public void checkStatus(ViewHolder holder, DeviceData readerData, int position) {
        int itemStatus = Integer.parseUnsignedInt(readerData.getStatus());
        if (((itemStatus & selectStatusCheck) == selectStatusCheck)) {
            holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.light_blue));
        }

        if ((itemStatus & onOffStatusCheck) == onStatus) {
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_green_12));
        } else if ((itemStatus & onOffStatusCheck) == offStatus) {
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_red_12));
        }
    }

    public void updateDeviceData(int position) {
        if(temp) {
            this.notifyItemChanged(position);
        }
    }

    public void refreshDeviceData() {
            this.notifyDataSetChanged();
    }
}
