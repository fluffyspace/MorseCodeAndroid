package com.example.morsecode.Adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import androidx.recyclerview.widget.RecyclerView;

import com.example.morsecode.R;
import com.example.morsecode.models.Message;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    public List<Message> list;
    private Integer idSender;
    private Integer idReceiver;
    public static final int MESSAGE_TYPE_IN = 1;
    public static final int MESSAGE_TYPE_OUT = 2;

    public ChatAdapter(Context context, List<Message> list, Integer id, Integer idReceiver) { // you can pass other parameters in constructor
        this.context = context;
        this.list = list;
        this.idSender = id;
        this.idReceiver = idReceiver;
    }

    private class MessageViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV;
        TextView timeTV;
        MessageViewHolder(final View itemView) {
            super(itemView);
            messageTV = itemView.findViewById(R.id.textViewMessage);
            timeTV = itemView.findViewById(R.id.time);
        }
        void bind(int position) {
            Message messageModel = list.get(position);
            messageTV.setText(messageModel.getMessage());
            timeTV.setText(messageModel.getSentAt());
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MessageViewHolder(LayoutInflater.from(context).inflate(viewType == MESSAGE_TYPE_IN ? R.layout.item_message_send : R.layout.item_message_receive, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (getItemCount() > 0) {
            MessageViewHolder messageViewHolder = (MessageViewHolder) holder;
            messageViewHolder.bind(position);
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (list.get(position).getSenderId() == idSender){
            return MESSAGE_TYPE_IN;
        }else if (list.get(position).getSenderId() == idReceiver){
            return MESSAGE_TYPE_OUT;
        }else{
            return MESSAGE_TYPE_IN;
        }
    }
}