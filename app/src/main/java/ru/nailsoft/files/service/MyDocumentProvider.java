package ru.nailsoft.files.service;

import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract;
import android.provider.DocumentsProvider;
import android.support.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ru.nailsoft.files.R;
import ru.nailsoft.files.model.FileItem;
import ru.nailsoft.files.model.TabData;
import ru.nailsoft.files.toolkit.ThreadPool;
import ru.nailsoft.files.toolkit.collections.Query;

import static ru.nailsoft.files.App.data;
import static ru.nailsoft.files.diagnostics.Logger.trace;

public class MyDocumentProvider extends DocumentsProvider {
    private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
            DocumentsContract.Root.COLUMN_ROOT_ID,
            DocumentsContract.Root.COLUMN_MIME_TYPES,
            DocumentsContract.Root.COLUMN_FLAGS,
            DocumentsContract.Root.COLUMN_ICON,
            DocumentsContract.Root.COLUMN_TITLE,
            DocumentsContract.Root.COLUMN_SUMMARY,
            DocumentsContract.Root.COLUMN_DOCUMENT_ID,
            DocumentsContract.Root.COLUMN_AVAILABLE_BYTES,};

    private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED,
            DocumentsContract.Document.COLUMN_FLAGS,
            DocumentsContract.Document.COLUMN_SIZE,};

    private final static TabDataFriend tabDataFriend = new TabDataFriend();

    @Override
    public Cursor queryRoots(String[] projection) throws FileNotFoundException {
        trace();

        if (data().tabs.isEmpty())
            throw new FileNotFoundException();

        if (projection == null)
            projection = DEFAULT_ROOT_PROJECTION;

        MatrixCursor cursor = new MatrixCursor(projection);

        for (TabData tab : data().tabs) {
            TabData.AbsHistoryItem historyItem = tab.getPath();
            if (!(historyItem instanceof TabData.DirectoryHistoryItem))
                continue;

            File path = ((TabData.DirectoryHistoryItem) historyItem).path;

            MatrixCursor.RowBuilder row = cursor.newRow();
            row.add(DocumentsContract.Root.COLUMN_ROOT_ID, historyItem.id());
            row.add(DocumentsContract.Root.COLUMN_MIME_TYPES, "*/*");
            row.add(DocumentsContract.Root.COLUMN_FLAGS,
                    DocumentsContract.Root.FLAG_SUPPORTS_CREATE |
                            DocumentsContract.Root.FLAG_SUPPORTS_RECENTS |
                            DocumentsContract.Root.FLAG_SUPPORTS_SEARCH);
            row.add(DocumentsContract.Root.COLUMN_ICON, R.drawable.ic_folder);
            row.add(DocumentsContract.Root.COLUMN_TITLE, tab.title);
            row.add(DocumentsContract.Root.COLUMN_SUMMARY, path.getParent());
            row.add(DocumentsContract.Root.COLUMN_DOCUMENT_ID, path.getAbsolutePath());
            row.add(DocumentsContract.Root.COLUMN_AVAILABLE_BYTES, path.getFreeSpace());

        }

        return cursor;
    }

    @Override
    public Cursor queryDocument(String documentId, String[] projection) throws FileNotFoundException {
        trace(documentId);

        if (projection == null)
            projection = DEFAULT_DOCUMENT_PROJECTION;

        MatrixCursor cursor = new MatrixCursor(projection);
        FileItem doc = getFileForDocId(documentId);
        if (doc == null)
            throw new FileNotFoundException(documentId);

        MatrixCursor.RowBuilder row = cursor.newRow();
        row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, doc.file.getAbsolutePath());
        row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, doc.mimeType);
        row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, doc.name);
        row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0);
        row.add(DocumentsContract.Document.COLUMN_FLAGS, 0);
        row.add(DocumentsContract.Document.COLUMN_SIZE, doc.size);

        return cursor;
    }

    @Nullable
    private FileItem getFileForDocId(String documentId) {
        File file = new File(documentId);
        return Query.query(data().tabs).extract(t -> t.getFiles(tabDataFriend)).first(f -> f.file.equals(file));
    }

    @Override
    public Cursor queryChildDocuments(String parentDocumentId, String[] projection, String sortOrder) throws FileNotFoundException {
        trace(parentDocumentId);

        if (projection == null)
            projection = DEFAULT_DOCUMENT_PROJECTION;

        MatrixCursor cursor = new MatrixCursor(projection);
        for (TabData tab : data().tabs) {
            if (tab.getPath().id().equals(parentDocumentId)) {
                for (FileItem file : tab.getFiles(tabDataFriend)) {
                    MatrixCursor.RowBuilder row = cursor.newRow();
                    row.add(DocumentsContract.Document.COLUMN_DOCUMENT_ID, file.file.getAbsolutePath());
                    row.add(DocumentsContract.Document.COLUMN_MIME_TYPE, file.mimeType);
                    row.add(DocumentsContract.Document.COLUMN_DISPLAY_NAME, file.name);
                    row.add(DocumentsContract.Document.COLUMN_LAST_MODIFIED, 0);
                    row.add(DocumentsContract.Document.COLUMN_FLAGS, 0);
                    row.add(DocumentsContract.Document.COLUMN_SIZE, file.size);
                }

                break;
            }
        }

        if (cursor.getCount() == 0)
            throw new FileNotFoundException();


        return cursor;
    }

    @Override
    public ParcelFileDescriptor openDocument(String documentId, String mode, @Nullable CancellationSignal signal) throws FileNotFoundException {
        trace(documentId);

        FileItem file = getFileForDocId(documentId);
        if (file == null)
            throw new FileNotFoundException();

        int accessMode = ParcelFileDescriptor.parseMode(mode);

        final boolean isWrite = (mode.indexOf('w') != -1);
        if (isWrite) {
            // Attach a close listener if the document is opened in write mode.
            try {
                return ParcelFileDescriptor.open(file.file, accessMode, ThreadPool.UI,
                        e -> {
                            trace();
                            if (e != null)
                                e.printStackTrace();
                        });
            } catch (IOException e) {
                throw new FileNotFoundException("Failed to open document with id"
                        + documentId + " and mode " + mode);
            }
        } else {
            return ParcelFileDescriptor.open(file.file, accessMode);
        }
    }

    @Override
    public boolean onCreate() {
        trace();
        return false;
    }

    /**
     * simulate C++ <b>friend class<b/>
     */
    public static class TabDataFriend {
        private TabDataFriend() {
        }
    }
}
