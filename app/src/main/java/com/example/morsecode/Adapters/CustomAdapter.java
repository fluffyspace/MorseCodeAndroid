package com.example.morsecode.Adapters;

import android.content.Context;
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

public class CustomAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final Context context;
    List<Message> list;
    private Integer idSender;
    private Integer idReceiver;
    public static final int MESSAGE_TYPE_IN = 1;
    public static final int MESSAGE_TYPE_OUT = 2;

    public CustomAdapter(Context context, List<Message> list, Integer id, Integer idReceiver) { // you can pass other parameters in constructor
        this.context = context;
        this.list = list;
        this.idSender = id;
        this.idReceiver = idReceiver;
    }

    private class MessageInViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV;
        MessageInViewHolder(final View itemView) {
            super(itemView);
            messageTV = itemView.findViewById(R.id.textViewMessage);
        }
        void bind(int position) {
            Message messageModel = list.get(position);
            messageTV.setText(messageModel.getMessage());
        }
    }

    private class MessageOutViewHolder extends RecyclerView.ViewHolder {

        TextView messageTV,dateTV;
        MessageOutViewHolder(final View itemView) {
            super(itemView);
            messageTV = itemView.findViewById(R.id.textViewMessage1);
        }
        void bind(int position) {
            Message messageModel = list.get(position);
            messageTV.setText(messageModel.getMessage());
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == MESSAGE_TYPE_IN) {
            return new MessageInViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_send, parent, false));
        }
        return new MessageOutViewHolder(LayoutInflater.from(context).inflate(R.layout.item_message_receive, parent, false));
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (list.get(position).getSenderId() == idSender) {


            MessageInViewHolder messageInViewHolder = (MessageInViewHolder)holder;
            messageInViewHolder.bind(position);
        } else if (list.get(position).getSenderId() != idSender){
            MessageOutViewHolder messageOutViewHolder = (MessageOutViewHolder)holder;
            messageOutViewHolder.bind(position);
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