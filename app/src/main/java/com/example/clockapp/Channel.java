package com.example.clockapp;

public class Channel {
    public final String name;
    public final String url;
    public final String logo;
    public final String group;

    public Channel(String name, String url, String logo, String group) {
        this.name = name != null ? name : "";
        this.url = url != null ? url : "";
        this.logo = logo;
        this.group = group != null ? group : "";
    }
}
