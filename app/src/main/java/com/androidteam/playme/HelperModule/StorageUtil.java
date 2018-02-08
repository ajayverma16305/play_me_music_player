package com.androidteam.playme.HelperModule;

import android.content.Context;
import android.content.SharedPreferences;
import com.androidteam.playme.MusicProvider.MusicContent;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;

/**
 * Created by AJAY VERMA on 05/02/18.
 * Company : CACAO SOLUTIONS
 */
public class StorageUtil {

    private final String STORAGE_AUDIO_AVAILABLE = "com.androidteam.playme.STORAGE_AVAILABLE";
    private final String STORAGE_AUDIO_LIST = "com.androidteam.playme.STORAGE_AUDIO_LIST";
    private final String STORAGE_INDEX = "com.androidteam.playme.STORAGE_INDEX";
    private final String STORAGE_PROGRESS = "com.androidteam.playme.STORAGE_PROGRESS";
    private final String STORAGE_CURRENT_TIME = "com.androidteam.playme.STORAGE_CURRENT_TIME";
    private final String STORAGE_TOTAL_TIME  = "com.androidteam.playme.STORAGE_TOTAL_TIME";
    private final String STORAGE_SHUFFLE  = "com.androidteam.playme.STORAGE_SHUFFLE";
    private final String STORAGE_REPEAT  = "com.androidteam.playme.STORAGE_REPEAT";
    private final String STORAGE_TAG  = "com.androidteam.playme.STORAGE_TAG";
    private final String STORAGE_CURRENT_SEEK_POSITION  = "com.androidteam.playme.STORAGE_CURRENT_SEEK_POSITION";
    private SharedPreferences preferences;
    private Context context;

    public StorageUtil(Context context) {
        this.context = context;
    }

    public void storeAudio(ArrayList<MusicContent> arrayList) {
        preferences = context.getSharedPreferences(STORAGE_AUDIO_LIST, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(arrayList);
        editor.putString("audioArrayList", json);
        editor.apply();
    }

    public ArrayList<MusicContent> loadAudio() {
        preferences = context.getSharedPreferences(STORAGE_AUDIO_LIST, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList", null);
        Type type = new TypeToken<ArrayList<MusicContent>>() {
        }.getType();
        return gson.fromJson(json, type);
    }

    public void storeAudioIndex(int index) {
        preferences = context.getSharedPreferences(STORAGE_INDEX, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex", index);
        editor.apply();
    }

    public void storeAudioProgress(int value) {
        preferences = context.getSharedPreferences(STORAGE_PROGRESS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioProgress", value);
        editor.apply();
    }

    public void storeAudioCurrentSeekPosition(int value) {
        preferences = context.getSharedPreferences(STORAGE_CURRENT_SEEK_POSITION, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioSeekPosition", value);
        editor.apply();
    }

    public void storeAudioShuffle(boolean isShuffled) {
        preferences = context.getSharedPreferences(STORAGE_SHUFFLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("isShuffle", isShuffled);
        editor.apply();
    }

    public void storeAvailable(boolean available) {
        preferences = context.getSharedPreferences(STORAGE_AUDIO_AVAILABLE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("available", available);
        editor.apply();
    }

    public void storeAudioCurrentTime(String currentTime) {
        preferences = context.getSharedPreferences(STORAGE_CURRENT_TIME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("audioCurrentTime", currentTime);
        editor.apply();
    }

    public void storeAudioTotalTime(String totalTime) {
        preferences = context.getSharedPreferences(STORAGE_TOTAL_TIME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("audioTotalTime", totalTime);
        editor.apply();
    }

    public void storeAudioRepeatOne(boolean repeatOne) {
        preferences = context.getSharedPreferences(STORAGE_REPEAT, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean("audioRepeatOne", repeatOne);
        editor.apply();
    }

    public boolean loadAudioIsRepeatOne() {
        preferences = context.getSharedPreferences(STORAGE_REPEAT, Context.MODE_PRIVATE);
        return preferences.getBoolean("audioRepeatOne", false); //return -1 if no data found
    }

    public void storeAudioIconTag(String tag) {
        preferences = context.getSharedPreferences(STORAGE_TAG, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("audioIconTag", tag);
        editor.apply();
    }

    public String loadAudioIconTag() {
        preferences = context.getSharedPreferences(STORAGE_TAG, Context.MODE_PRIVATE);
        return preferences.getString("audioIconTag", "pause"); //return -1 if no data found
    }

    public String loadAudioTotalTime() {
        preferences = context.getSharedPreferences(STORAGE_TOTAL_TIME, Context.MODE_PRIVATE);
        return preferences.getString("audioTotalTime", "00:00"); //return -1 if no data found
    }

    public String loadAudioCurrentTime() {
        preferences = context.getSharedPreferences(STORAGE_CURRENT_TIME, Context.MODE_PRIVATE);
        return preferences.getString("audioCurrentTime", "00:00"); //return -1 if no data found
    }

    public boolean loadAudioShuffledState() {
        preferences = context.getSharedPreferences(STORAGE_SHUFFLE, Context.MODE_PRIVATE);
        return preferences.getBoolean("isShuffle", false); //return -1 if no data found
    }

    public boolean loadAvailable() {
        preferences = context.getSharedPreferences(STORAGE_AUDIO_AVAILABLE, Context.MODE_PRIVATE);
        return preferences.getBoolean("available", false); //return -1 if no data found
    }

    public int loadAudioProgress() {
        preferences = context.getSharedPreferences(STORAGE_PROGRESS, Context.MODE_PRIVATE);
        return preferences.getInt("audioProgress", 0);//return -1 if no data found
    }

    public int loadAudioSeekPosition() {
        preferences = context.getSharedPreferences(STORAGE_CURRENT_SEEK_POSITION, Context.MODE_PRIVATE);
        return preferences.getInt("audioSeekPosition", 0);//return -1 if no data found
    }

    public int loadAudioIndex() {
        preferences = context.getSharedPreferences(STORAGE_INDEX, Context.MODE_PRIVATE);
        return preferences.getInt("audioIndex", 0);//return -1 if no data found
    }

    public void clearCachedAudioPlaylist() {
        preferences = context.getSharedPreferences(STORAGE_AUDIO_LIST, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    }
}