package com.example.musicfirebase.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicfirebase.adapters.LikedAdapter;
import com.example.musicfirebase.utils.OnRecyclerClickListener;
import com.example.musicfirebase.R;
import com.example.musicfirebase.databinding.FragmentLikedBinding;
import com.example.musicfirebase.models.Song;
import com.example.musicfirebase.utils.Anims;
import com.example.musicfirebase.viewmodels.SongViewModel;

import java.util.List;

public class LikedFragment extends Fragment {
    private final String TAG = "(mStream)";
    private FragmentLikedBinding B;
    private List<Song> likedSongs;
    private SongViewModel songVM;
    private LikedAdapter likedAdapter;
    private Window ui;

    // Navigate to PlayerFragment when a song is clicked
    private final OnRecyclerClickListener<Song> onRecyclerClickListener = new OnRecyclerClickListener<Song>() {
        @Override
        public void onItemClick(Song likedSong) {
            songVM.select(likedSong);

            // Navigates to PlayerFragment when a song is clicked
            NavController navController = NavHostFragment.findNavController(LikedFragment.this);
            navController.navigate(LikedFragmentDirections.NavigateToPlayerFromLiked());
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // View Binding. Removes the need for 'findViewById(id)'
        B = FragmentLikedBinding.inflate(inflater, container, false);

        // UI
        ui = this.requireActivity().getWindow();
        ui.setStatusBarColor(getResources().getColor(R.color.white, null));

        // ViewModels
        songVM = new ViewModelProvider(requireActivity()).get(SongViewModel.class);

        // If there is a change in LikedSongs, update the adapter
        songVM.getLikedSongs().observe(getViewLifecycleOwner(), songs -> {
            likedAdapter.updateWith(songs);
            Anims.recyclerFall(B.likedRecycler);
        });

        likedSongs = songVM.getLikedSongs().getValue();

        B.emptyListText.setVisibility(likedSongs.isEmpty() ? View.VISIBLE : View.GONE);

        // Standard RecyclerView config
        likedAdapter = new LikedAdapter(onRecyclerClickListener);
        B.likedRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        B.likedRecycler.setAdapter(likedAdapter);

        return B.getRoot();
    }
}
