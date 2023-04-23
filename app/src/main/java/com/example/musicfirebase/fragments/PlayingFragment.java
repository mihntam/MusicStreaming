package com.example.musicfirebase.fragments;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.SeekBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.musicfirebase.MainActivity;
import com.example.musicfirebase.R;
import com.example.musicfirebase.adapters.PlayerAdapter;
import com.example.musicfirebase.utils.OnRecyclerClickListener;
import com.example.musicfirebase.dialogs.DialogAddToPlaylist;
import com.example.musicfirebase.dialogs.DialogCreatePlaylist;
import com.example.musicfirebase.utils.Anims;
import com.example.musicfirebase.utils.Misc;
import com.example.musicfirebase.utils.PopupBuilder;
import com.example.musicfirebase.viewmodels.PlaylistViewModel;
import com.example.musicfirebase.databinding.FragmentPlayerBinding;
import com.example.musicfirebase.models.Song;
import com.example.musicfirebase.viewmodels.PlayingViewModel;
import com.example.musicfirebase.viewmodels.SongViewModel;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.Query;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.util.List;
import java.util.Objects;


public class PlayingFragment extends Fragment {
    private final String TAG = "(mStream)";

    private MediaSessionCompat mMediaSessionCompat;
    private FragmentPlayerBinding B;
    private SongViewModel songVM;
    private PlayingViewModel playerVM;
    private PlaylistViewModel playlistVM;
    private BottomNavigationView mNavBar;
    private PlayerAdapter playerAdapter;
    private View mBottomSheet;
    private Handler handler;
    private Window ui;

    // Listens to liking/un-liking song events
    private final SongViewModel.OnLikedListener onLikedListener = new SongViewModel.OnLikedListener() {
        @Override
        public void onLike(Song song) {
            B.btnLike.setImageResource(R.drawable.svg_heart_selected);
            Misc.toast(requireView(), "'" + song.getTitle() + "' đã thích!");
        }

        @Override
        public void onUnlike(Song song) {
            B.btnLike.setImageResource(R.drawable.svg_heart_unselected);
            Misc.toast(requireView(), "'" + song.getTitle() + "' bỏ thích!");
        }
    };

    // Listens to touch events on SeekBar
    private final SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                B.elapsedTimeText.setText(getTimeFormat(progress));
            }
        }

        // Stops updating time labels and SeekBar even when song is playing
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            handler.removeCallbacks(updatingSeekBar);
        }

        // Updates time labels when finger is lifted off SeekBar thumb; also restarts recursive callbacks
        // will also update at that position
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            playerVM.moveTo(seekBar.getProgress());
            handler.postDelayed(updatingSeekBar, 500);
        }
    };

    // Create PopupMenu with options
    private final PopupBuilder.OnPlayerMenuItemSelected onPlayerMenuItemSelected = new PopupBuilder.OnPlayerMenuItemSelected() {
        // Listens to 'Add to New Playlists' selection in the popup menu
        @Override
        public void onCreatePlaylistSelected() {
            DialogCreatePlaylist dialog = new DialogCreatePlaylist(textInput -> {
                String title = textInput.getText().toString();
                Song currentSong = playerVM.getCurrentSong();
                String songId = currentSong.getId();
                String songTitle = currentSong.getTitle();

                // Dialog automatically closes on choice click; no need to manually do it
                if (title.isEmpty()) {
                    Misc.toast(requireView(), "Không được bỏ trống tên!");
                } else {
                    playlistVM.createNewPlaylist(title, songId);
                    Misc.toast(requireView(), title + " đã được tạo '" + songTitle + "' thêm vào!");
                }
            });

            MainActivity.hideKeyboardIn(PlayingFragment.this.requireView());
            dialog.show(getParentFragmentManager(), "OK");
        }

        // Listens to 'Add to Existing Playlist' selection in the popup menu
        @Override
        public void onAddToExistingPlaylistSelected() {
            boolean playlistsAreLoaded = !playlistVM.getPlaylists().getValue().isEmpty();
            playlistVM.getPlaylistsFromDb(Query.Direction.ASCENDING);

            if (playlistsAreLoaded) {
                playlistVM.getPlaylists().getValue();
            } else {
                playlistVM.getPlaylistsFromDb(Query.Direction.ASCENDING);
            }

            DialogAddToPlaylist dialog = new DialogAddToPlaylist(playlist -> {
                Song thisSong = playerVM.getCurrentSong();
                String songId = thisSong.getId();
                String songTitle = thisSong.getTitle();

                playlistVM.addSongToExistingPlaylist(playlist.getId(), songId);
                Misc.toast(requireView(), "'" + songTitle + " được thêm vào " + playlist.getTitle() + "!");
            });

            dialog.show(getParentFragmentManager(), "OK");
        }
    };

    // Listens to specific events defined in PlayerViewModel
    private final PlayingViewModel.Listener playerListener = new PlayingViewModel.Listener() {
        @Override
        public void onSongPrepared() {
            B.progressBar.setMax(playerVM.getDuration());
            B.remainingTimeText.setText(getTimeFormat(playerVM.getDuration()));
            setLikedBtnBasedOnFlag();
        }

        // updates elapsed/remaining time when next/previous is pressed
        @Override
        public void onNext() {
            playerVM.disableLoop();
        }

        @Override
        public void onPrevious() {
            playerVM.disableLoop();
        }

        @Override
        public void onShuffled(List<Song> songs) {
            playerAdapter.updateWith(songs);
        }

        @Override
        public void onUnshuffled(List<Song> songs) {
            playerAdapter.updateWith(songs);
        }

        // automatically plays the next song
        @Override
        public void onSongCompletion() {
            handler.removeCallbacks(updatingSeekBar);
            playerVM.playNext();
        }
    };

    // PlayerAdapter
    private final OnRecyclerClickListener<Song> onRecyclerClickListener = new OnRecyclerClickListener<Song>() {
        @Override
        public void onItemClick(Song song) {
            songVM.select(song);
        }
    };

    @SuppressLint({"ClickableViewAccessibility", "FragmentLiveDataObserve"})
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // View binding
        B = FragmentPlayerBinding.inflate(inflater, container, false);
        handler = new Handler();

        // ViewModels
        songVM = new ViewModelProvider(requireActivity()).get(SongViewModel.class);
        playerVM = new ViewModelProvider(requireActivity()).get(PlayingViewModel.class);
        playlistVM = new ViewModelProvider(requireActivity()).get(PlaylistViewModel.class);

        // Initialises the queue
        playerVM.updateQueue(songVM.getSongs().getValue());

        // Observers
        songVM.getSongs().observe(this, songs -> {
            playerAdapter.updateWith(songs);
            B.playerRecycler.scrollToPosition(0);
            Anims.recyclerFall(B.playerRecycler);
        });
        songVM.selectedSong.observe(this, this::onCurrentSongChange);
        playerVM.currentSong.observe(this, this::updateUiFromSong);
        playerVM.isPlaying.observe(this, this::onIsPlayingChange);
        playerVM.isLooping.observe(this, this::onIsLoopingChange);
        playerVM.isShuffled.observe(this, this::onIsShuffledChange);

        // Listeners
        songVM.setOnLikedListener(onLikedListener);
        playerVM.setPlayerListener(playerListener);
        B.progressBar.setOnSeekBarChangeListener(onSeekBarChangeListener);

        // Separate functions from animations to reduce unnecessary clutter
        B.btnPlaylistOptions.setOnClickListener(view -> PopupBuilder.forPlayer(view, onPlayerMenuItemSelected));
        B.btnPlayPause.setOnClickListener(view -> playerVM.togglePlayPause());
        B.btnNext.setOnClickListener(view -> playerVM.playNext());
        B.btnPrevious.setOnClickListener(view -> playerVM.playPrevious());
        B.btnLoop.setOnClickListener(view -> playerVM.toggleLoop());
        B.btnShuffle.setOnClickListener(view -> playerVM.toggleShuffle());
        B.btnBackPress.setOnClickListener(this::onBackPressed);
        B.btnLike.setOnClickListener(this::toggleLikeOnSong);

        // Button animations
        B.btnPlaylistOptions.setOnTouchListener(Anims::smallShrink);
        B.btnPlayPause.setOnTouchListener(Anims::smallShrink);
        B.btnNext.setOnTouchListener(Anims::smallShrink);
        B.btnPrevious.setOnTouchListener(Anims::smallShrink);
        B.btnLoop.setOnTouchListener(Anims::smallShrink);
        B.btnShuffle.setOnTouchListener(Anims::smallShrink);
        B.btnBackPress.setOnTouchListener(Anims::smallShrink);
        B.btnLike.setOnTouchListener(Anims::smallShrink);

        // PlayerAdapter
        B.playerRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        playerAdapter = new PlayerAdapter(onRecyclerClickListener);
        B.playerRecycler.setAdapter(playerAdapter);

        String playingFromWhat = songVM.getSelectedPlaylist().getValue().getTitle();
        B.playingFrom.setText(playingFromWhat.isEmpty() ? "Thư viện" : playingFromWhat);

        return B.getRoot();
    }

    private void onCurrentSongChange(Song song) {
        playerVM.prepareSong(song);
        playerVM.play();
    }

    private void onIsPlayingChange(boolean isPlaying) {
        if (isPlaying) {
            handler.postDelayed(updatingSeekBar, 500);
            B.btnPlayPause.setImageResource(R.drawable.svg_btn_pause);
        } else {
            handler.removeCallbacks(updatingSeekBar);
            B.btnPlayPause.setImageResource(R.drawable.svg_btn_play);
        }
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        mMediaSessionCompat = new MediaSessionCompat(requireContext(), "PlayingFragment");

        // Enable callbacks from MediaButtons and TransportControls
        mMediaSessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);

        // Set an initial PlaybackState with ACTION_PLAY, so media buttons can start the player
        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder().setActions(PlaybackStateCompat.ACTION_PLAY);
        mMediaSessionCompat.setPlaybackState(stateBuilder.build());

        // MySessionCallback has methods that handle callbacks from a media controller
        mMediaSessionCompat.setCallback(new MySessionCallback());

        // Set the session's token so that client activities can communicate with it
        MediaSessionCompat.Token token = mMediaSessionCompat.getSessionToken();
        ((MainActivity)requireActivity()).setMediaSessionCompatToken(token);
    }

    private class MySessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            // Play the media player
        }

        @Override
        public void onPause() {
            // Pause the media player
        }

        @Override
        public void onStop() {
            // Stop the media player
        }

        @Override
        public void onSkipToNext() {
            // Skip to the next media item
        }

        @Override
        public void onSkipToPrevious() {
            // Skip to the previous media item
        }

        @Override
        public void onSeekTo(long pos) {
            // Seek to a specific position in the media player
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMediaSessionCompat.release();
    }

    private void onIsLoopingChange(boolean isLooping) {
        B.btnLoop.setColorFilter(ContextCompat.getColor(
                requireActivity(),
                isLooping ? R.color.mauve : R.color.white)
        );
    }

    private void onIsShuffledChange(boolean isShuffling) {
        B.btnShuffle.setColorFilter(ContextCompat.getColor(
                requireActivity(),
                isShuffling ? R.color.mauve : R.color.white
        ));
    }

    private void onBackPressed(View v) {
        requireActivity().onBackPressed();
    }

    // Find bottom UI from activity
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ui = requireActivity().getWindow();
        mNavBar = requireActivity().findViewById(R.id.navBar);
//        mBottomSheet = requireActivity().findViewById(R.id.bottomSheet);
    }

    // Make bottom UI visible
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mNavBar.setVisibility(View.VISIBLE);
//        mBottomSheet.setVisibility(View.VISIBLE);
    }

    // No bottom UI while view exists
    @Override
    public void onResume() {
        super.onResume();
        mNavBar.setVisibility(View.GONE);
//        mBottomSheet.setVisibility(View.GONE);
    }

    private void updateUiFromSong(Song song) {
        String songTitle = song.getTitle();
        String songArtist = song.getArtist();
        String songCoverArt = song.getCoverUrl();

        B.songHolder.setText(songTitle);
        B.artistHolder.setText(songArtist);
        Picasso.get().load(songCoverArt).into(B.coverArtHolder);
        Picasso.get().load(songCoverArt).into(dominantSwatchProcessor);
    }

    // If the current song is already liked, remove it from liked and vice versa.
    private void toggleLikeOnSong(View v) {
        Song currentSong = playerVM.getCurrentSong();
        if (currentSong.isLiked) {
            currentSong.setLiked(false);
            songVM.removeFromLiked(currentSong);
        } else {
            currentSong.setLiked(true);
            songVM.addToLiked(currentSong);
        }
    }

    // Updates the like button's ImageView based on the song's liked flag. Nothing much.
    private void setLikedBtnBasedOnFlag() {
        Song song = playerVM.getCurrentSong();
        B.btnLike.setImageResource(song.isLiked
                ? R.drawable.svg_heart_selected
                : R.drawable.svg_heart_unselected);
    }

    // For time labels
    private String getTimeFormat(int duration) {
        int min = duration / 1000 / 60;
        int sec = duration / 1000 % 60;

        return min + ":" + ((sec < 10) ? "0" + sec : sec);
    }

    // Update SeekBar and time labels every 0.5s
    private final Runnable updatingSeekBar = new Runnable() {
        @Override
        public void run() {
            B.progressBar.setProgress(playerVM.getCurrentPos());
            B.elapsedTimeText.setText(getTimeFormat(playerVM.getCurrentPos()));
            handler.postDelayed(this, 500);
        }
    };

    // Sets bg to dominant swatch of the cover art
    private final Target dominantSwatchProcessor = new Target() {
        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Palette.from(bitmap).generate(palette -> {
                assert palette != null : "Could not find palette";

                int dominantSwatch = Objects.requireNonNull(palette.getDominantSwatch()).getRgb();
                B.motionLayout.setBackgroundColor(dominantSwatch);
                ui.setStatusBarColor(dominantSwatch);
            });
        }

        @Override
        public void onBitmapFailed(Exception e, Drawable errorDrawable) {
            Log.d("(Palette)", "Failed to load bitmap.");
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
        }



    };
}