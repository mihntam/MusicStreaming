package com.project.musicstreaming;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class MusicAdapter extends ArrayAdapter<ListData> {
    public MusicAdapter(@NonNull Context context, ArrayList<ListData> dataArrayList) {
        super(context, R.layout.music_item, dataArrayList);
    }
    @NonNull
    @Override
    public View getView(int position, @Nullable View view, @NonNull ViewGroup parent) {
        ListData listData = getItem(position);
        if (view == null){
            view = LayoutInflater.from(getContext()).inflate(R.layout.music_item, parent, false);
        }

        TextView songTitle = view.findViewById(R.id.song_title);
        TextView artistName = view.findViewById(R.id.artist_name);

        songTitle.setText(listData.musicTitle);
        artistName.setText(listData.artistName);

        return view;
    }
}