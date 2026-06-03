package com.example.clockapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ViewHolder> {

    public interface OnChannelClick {
        void onClick(Channel channel);
    }

    private final List<Channel> channels = new ArrayList<>();
    private final OnChannelClick listener;

    public ChannelAdapter(OnChannelClick listener) {
        this.listener = listener;
    }

    public void setChannels(List<Channel> list) {
        channels.clear();
        channels.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Channel ch = channels.get(pos);
        h.number.setText(String.valueOf(pos + 1));
        h.name.setText(ch.name);
        h.group.setText(ch.group);

        if (ch.logo != null && !ch.logo.isEmpty()) {
            Picasso.get()
                    .load(ch.logo)
                    .placeholder(android.R.drawable.ic_menu_slideshow)
                    .error(android.R.drawable.ic_menu_slideshow)
                    .into(h.logo);
        } else {
            h.logo.setImageResource(android.R.drawable.ic_menu_slideshow);
        }

        h.itemView.setOnClickListener(v -> listener.onClick(ch));
    }

    @Override
    public int getItemCount() {
        return channels.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView number, name, group;
        ImageView logo;

        ViewHolder(View v) {
            super(v);
            number = v.findViewById(R.id.channelNumber);
            name = v.findViewById(R.id.channelName);
            group = v.findViewById(R.id.channelGroup);
            logo = v.findViewById(R.id.channelLogo);
        }
    }
}
