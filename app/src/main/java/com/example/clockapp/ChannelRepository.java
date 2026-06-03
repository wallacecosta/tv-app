package com.example.clockapp;

import java.util.ArrayList;
import java.util.List;

public class ChannelRepository {

    private static final ChannelRepository INSTANCE = new ChannelRepository();

    private final List<Channel> channels = new ArrayList<>();
    private int currentIndex = -1;

    private ChannelRepository() {}

    public static ChannelRepository get() {
        return INSTANCE;
    }

    public List<Channel> getChannels() {
        return channels;
    }

    public void setChannels(List<Channel> list) {
        channels.clear();
        channels.addAll(list);
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        currentIndex = index;
    }

    public boolean isEmpty() {
        return channels.isEmpty();
    }
}
