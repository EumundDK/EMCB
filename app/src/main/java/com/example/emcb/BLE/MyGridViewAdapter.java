package com.example.emcb.BLE;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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

    private LayoutInflater mInflater;
    private ArrayList<DeviceData> mDeviceDataList;
    private BluetoothLeService mBluetoothLeService = new BluetoothLeService();

    private int itemSelectedCard = RecyclerView.NO_POSITION;
    private int previousItemSelectCard = RecyclerView.NO_POSITION;
    private final byte[] myCommand = {0x08, 0x00, 0x03};

    // data is passed into the constructor
    MyGridViewAdapter(Context context, ArrayList<DeviceData> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mDeviceDataList = data;
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
        Button mSwitch;
        CardView mCardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            mTagName = itemView.findViewById(R.id.itemNameTextView);
            mStatusOff = itemView.findViewById(R.id.itemStatusOnOff);
            mCurrent = itemView.findViewById(R.id.itemCurrentTextView);
            mCurrentSymbolTextView = itemView.findViewById(R.id.currentSymbolTextView);
            mSwitch = itemView.findViewById(R.id.onOffSwitch);
            mCardView = itemView.findViewById(R.id.card_view);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    itemSelectedCard = getLayoutPosition();
                    notifyItemChanged(itemSelectedCard);
                    myCommand[1] = Byte.parseByte(mDeviceDataList.get(itemSelectedCard).getName());
                    myCommand[2] = 0x03;
                    mBluetoothLeService.writeCharacteristicData(myCommand);
                    Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " Selected", LENGTH_SHORT).show();
                }
            });

            mSwitch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int itemSelectedSwitch = getLayoutPosition();
                    byte itemStatus = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getStatus());
                    byte itemName = Byte.parseByte(mDeviceDataList.get(itemSelectedSwitch).getName());
                    if((itemStatus & onOffStatusCheck) == offStatus) {
                        myCommand[1] = itemName;
                        myCommand[2] = 0x02;
                        mBluetoothLeService.writeCharacteristicData(myCommand);
                        Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " ON", LENGTH_SHORT).show();
                    }

                    if((itemStatus & onOffStatusCheck) == onStatus) {
                        myCommand[1] = itemName;
                        myCommand[2] = 0x01;
                        mBluetoothLeService.writeCharacteristicData(myCommand);
                        Toast.makeText(mInflater.getContext(), "Tag No. " + Arrays.toString(myCommand) + " OFF", LENGTH_SHORT).show();
                    }
                }
            });
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

    //INT & STATUS NOT WORKING
    public void checkStatus(ViewHolder holder, DeviceData readerData, int position) {
        int itemStatus = Integer.parseUnsignedInt(readerData.getStatus(), 16);
        if (((itemStatus & selectStatusCheck) == selectStatusCheck) || (position == itemSelectedCard)) {
            previousItemSelectCard = position;
            itemSelectedCard = RecyclerView.NO_POSITION;
            holder.mCardView.setCardBackgroundColor(mInflater.getContext().getColor(R.color.light_blue));
        }

        if ((itemStatus & onOffStatusCheck) == onStatus) {
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_green_12));
        } else if ((itemStatus & onOffStatusCheck) == offStatus) {
            holder.mStatusOff.setImageDrawable(mInflater.getContext().getDrawable(R.drawable.ic_baseline_circle_red_12));
        }
    }
}
