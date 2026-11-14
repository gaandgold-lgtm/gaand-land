package kr.gaand.routine;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import java.io.File;
import java.io.FileNotFoundException;

/** 공유 카드 이미지를 다른 앱(카톡·인스타)에 넘겨주는 통로 */
public class ShareProvider extends ContentProvider {
    static File file(Context c) { return new File(c.getCacheDir(), "share.png"); }
    static Uri uri(Context c) { return Uri.parse("content://" + c.getPackageName() + ".share/share.png"); }

    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri u) { return "image/png"; }
    @Override public ParcelFileDescriptor openFile(Uri u, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(getContext()), ParcelFileDescriptor.MODE_READ_ONLY);
    }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) {
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{"가앤루틴.png", file(getContext()).length()});
        return c;
    }
    @Override public Uri insert(Uri u, ContentValues v) { return null; }
    @Override public int delete(Uri u, String s, String[] a) { return 0; }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { return 0; }
}
