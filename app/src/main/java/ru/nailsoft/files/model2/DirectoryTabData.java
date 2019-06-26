package ru.nailsoft.files.model2;

public class DirectoryTabData extends AbsTabData {
    private final Tab tab;
    private final AbsDirectoryItem root;

    public DirectoryTabData(Tab tab, AbsDirectoryItem root) {

        this.tab = tab;
        this.root = root;
    }

    @Override
    public CharSequence title() {
        return null;
    }
}
