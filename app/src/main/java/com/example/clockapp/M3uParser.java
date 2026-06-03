package com.example.clockapp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3uParser {

    public static List<Channel> parse(String m3uUrl) throws Exception {
        List<Channel> channels = new ArrayList<>();

        HttpURLConnection conn = openConnection(m3uUrl, 5);
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "UTF-8"));

        String line;
        String pendingName = null;
        String pendingLogo = null;
        String pendingGroup = null;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.startsWith("#EXTINF")) {
                pendingName = attr(line, "tvg-name");
                if (pendingName == null || pendingName.isEmpty()) {
                    int comma = line.lastIndexOf(',');
                    pendingName = comma >= 0 ? line.substring(comma + 1).trim() : "Canal";
                }
                pendingLogo = attr(line, "tvg-logo");
                pendingGroup = attr(line, "group-title");
            } else if (!line.startsWith("#") && !line.isEmpty() && pendingName != null) {
                channels.add(new Channel(pendingName, line, pendingLogo, pendingGroup));
                pendingName = null;
                pendingLogo = null;
                pendingGroup = null;
            }
        }

        reader.close();
        conn.disconnect();
        return channels;
    }

    private static HttpURLConnection openConnection(String urlStr, int maxRedirects) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setInstanceFollowRedirects(false);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("User-Agent", "Mozilla/5.0");

        int status = conn.getResponseCode();
        if ((status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == 307 || status == 308) && maxRedirects > 0) {
            String location = conn.getHeaderField("Location");
            conn.disconnect();
            return openConnection(location, maxRedirects - 1);
        }
        return conn;
    }

    private static String attr(String line, String name) {
        Matcher m = Pattern.compile(name + "=\"([^\"]*)\"").matcher(line);
        return m.find() ? m.group(1) : null;
    }
}
