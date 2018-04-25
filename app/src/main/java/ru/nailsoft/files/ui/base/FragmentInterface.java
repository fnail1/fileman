package ru.nailsoft.files.ui.base;

public interface FragmentInterface {
    /**
     * Handles system back button.
     *
     * @return true if consumed, false otherwise
     */
    boolean onBackPressed();

    /**
     * Handles toolbar up button.
     *
     * @return true if consumed, false otherwise
     */
    boolean onUpPressed();
}
