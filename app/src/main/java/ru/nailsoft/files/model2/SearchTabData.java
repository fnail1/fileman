package ru.nailsoft.files.model2;

import android.text.SpannableStringBuilder;
import android.text.style.ImageSpan;

import ru.nailsoft.files.R;

import static ru.nailsoft.files.App.app;

public class SearchTabData extends AbsTabData {
    private final Tab tab;
    private final AbsDirectoryItem root;
    private final SpannableStringBuilder title;

    public SearchTabData(Tab tab, AbsDirectoryItem root) {
        this.tab = tab;
        this.root = root;
        title = new SpannableStringBuilder("  " + root.title);
        title.setSpan(new ImageSpan(app(), R.drawable.ic_search), 0, 1, ImageSpan.ALIGN_BASELINE);
    }

    @Override
    public CharSequence title() {
        return title;
    }
}
