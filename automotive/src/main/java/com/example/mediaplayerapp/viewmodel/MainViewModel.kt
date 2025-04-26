package com.example.mediaplayerapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel


open class MainViewModel(private val application: Application) : AndroidViewModel(application) {

    /**
     * Sets the bass level for the currently selected profile.
     *
     * This function launches a coroutine to perform the following steps:
     * 1. Retrieves the index of the currently selected profile.
     * 2. Ensures that the profile exists. If it does not, logs an error and returns early.
     * 3. Sets the bass level for the profile in the repository.
     *
     * @param bass The new bass level to apply.
     */
    fun setBass(bass: Int) {

    }

    /**
     * Sets the mid level for the currently selected profile.
     *
     * This function launches a coroutine to perform the following steps:
     * 1. Retrieves the index of the currently selected profile.
     * 2. Ensures that the profile exists. If it does not, logs an error and returns early.
     * 3. Sets the mid level for the profile in the repository.
     *
     * @param mid The new mid level to apply.
     */
    fun setMid(mid: Int) {

    }

    /**
     * Sets the treble level for the currently selected profile.
     *
     * This function launches a coroutine to perform the following steps:
     * 1. Retrieves the index of the currently selected profile.
     * 2. Ensures that the profile exists. If it does not, logs an error and returns early.
     * 3. Sets the treble level for the profile in the repository.
     *
     * @param treble The new treble level to apply.
     */
    fun setTreble(treble: Int) {

    }
}