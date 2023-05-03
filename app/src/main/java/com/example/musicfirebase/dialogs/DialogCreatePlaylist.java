package com.example.musicfirebase.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.musicfirebase.R;

public class DialogCreatePlaylist extends DialogFragment {
    private final DialogCreatePlaylistListener mDialogListener;

    public DialogCreatePlaylist(DialogCreatePlaylistListener listener) {
        mDialogListener = listener;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View v = inflater.inflate(R.layout.misc_dialog_create_playlist, null);

        EditText titleField = v.findViewById(R.id.dialogEnterNameField);

        /* Start building */

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setView(v)
                .setPositiveButton("Tạo",
                        (dialog, which) -> mDialogListener.onCreateSelected(titleField))
                .setNegativeButton("Thoát",
                        (dialog, which) -> dialog.dismiss())
                .setOnDismissListener(dialog -> titleField.getText().clear());

        /* End building */

        return builder.create();
    }

    public interface DialogCreatePlaylistListener {
        void onCreateSelected(EditText enteredText);
    }
}
