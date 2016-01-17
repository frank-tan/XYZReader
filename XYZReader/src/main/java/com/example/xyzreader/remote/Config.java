package com.example.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;

    static {
        String urlString = "https://dl.dropboxusercontent.com/u/231329/xyzreader_data/data.json";
        URL url = null;
        try {
            url = new URL(urlString);
        } catch (MalformedURLException ignored) {
            Log.e("XYZReader","Cannot resolve url: " + urlString);
        }

        BASE_URL = url;
    }
}
