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

public class TvChannelAdapter extends RecyclerView.Adapter<TvChannelAdapter.ViewHolder> {

    public interface OnChannelClick {
        void onClick(Channel channel, int position);
    }

    private final List<Channel> channels = new ArrayList<>();
    private int selectedIndex = -1;
    private int focusedIndex = -1;
    private boolean sidebarMode = false;
    private final OnChannelClick listener;

    public TvChannelAdapter(OnChannelClick listener) {
        this.listener = listener;
    }

    public void setChannels(List<Channel> list) {
        channels.clear();
        channels.addAll(list);
        notifyDataSetChanged();
    }

    public void setSidebarMode(boolean sidebar) {
        sidebarMode = sidebar;
        notifyDataSetChanged();
    }

    public void setSelectedIndex(int index) {
        int old = selectedIndex;
        selectedIndex = index;
        if (old >= 0 && old < channels.size()) notifyItemChanged(old);
        if (index >= 0 && index < channels.size()) notifyItemChanged(index);
    }

    public void setFocusedIndex(int index) {
        int old = focusedIndex;
        focusedIndex = index;
        if (old >= 0 && old < channels.size()) notifyItemChanged(old);
        if (index >= 0 && index < channels.size()) notifyItemChanged(index);
    }

    public int getCount() {
        return channels.size();
    }

    public Channel getChannel(int index) {
        return (index >= 0 && index < channels.size()) ? channels.get(index) : null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_channel_tv, parent, false);
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

        if (sidebarMode) {
            // Sidebar: foco manual, itens não focáveis pelo sistema
            h.itemView.setFocusable(false);
            if (pos == focusedIndex) {
                h.itemView.setBackgroundColor(0xCCe94560);
            } else if (pos == selectedIndex) {
                h.itemView.setBackgroundColor(0xFF0f3460);
            } else {
                h.itemView.setBackgroundColor(0x00000000);
            }
        } else {
            // Lista principal: foco natural pelo D-pad + destaque do canal atual
            h.itemView.setFocusable(true);
            if (pos == selectedIndex) {
                h.itemView.setBackgroundColor(0xFF0f3460);
            } else {
                h.itemView.setBackgroundResource(R.drawable.tv_item_selector);
            }
        }

        final int p = pos;
        h.itemView.setOnClickListener(v -> listener.onClick(ch, p));
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
