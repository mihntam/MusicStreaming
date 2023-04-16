package com.example.musicfirebase.utils;

/**
 * Use this interface whenever click functionality needs to be added to a {@link androidx.recyclerview.widget.RecyclerView}.
 */
public interface OnRecyclerClickListener<E> {
    void onItemClick(E item);
}