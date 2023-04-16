package com.example.musicfirebase.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.musicfirebase.adapters.DialogAdapter;
import com.example.musicfirebase.utils.OnRecyclerClickListener;
import com.example.musicfirebase.models.Playlist;
import com.example.musicfirebase.R;
import com.example.musicfirebase.utils.Anims;
import com.example.musicfirebase.viewmodels.PlaylistViewModel;

public class DialogAddToPlaylist extends DialogFragment {
    private final String TAG = "(DialogFragment)";
    private PlaylistViewModel playlistVM;
    private RecyclerView mDialogRecycler;
    private DialogAdapter mDialogAdapter;
    private final OnRecyclerClickListener<Playlist> mOnRecyclerClickListener;

    /**
     * Inflates the dialog's view.
     * @param listener an interface that should be implemented in a parent fragment of
     *                 this {@link DialogFragment}.
     */
    public DialogAddToPlaylist(OnRecyclerClickListener<Playlist> listener) {
        mOnRecyclerClickListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.misc_dialog_add_to_playlist, null);

        playlistVM = new ViewModelProvider(requireParentFragment()
                .requireActivity())
                .get(PlaylistViewModel.class);

        mDialogRecycler = v.findViewById(R.id.dialogRecycler);

        /* Start building */

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(v)
                .setNegativeButton("CLOSE",
                        (dialog, which) -> dialog.dismiss());

        /* End building */

        mDialogAdapter = new DialogAdapter(mOnRecyclerClickListener);
        mDialogRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        mDialogRecycler.setAdapter(mDialogAdapter);

        // If there is a change in playlists, update DialogAdapter
        playlistVM.getPlaylists().observe(requireParentFragment().getViewLifecycleOwner(), playlists -> {
            mDialogAdapter.updateAdapter(playlists);
            Anims.recyclerFall(mDialogRecycler);
        });

        return builder.create();
    }
}
