package com.androidteam.playme.HelperModule;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by AJAY VERMA on 12/02/18.
 * Company : CACAO SOLUTIONS
 */

public class IntentHelper {

    public static void shareAudioWithOtherApps(Context context,String path){
        Uri uri = Uri.parse(path);
        String originalPath = getRealPathFromURI(context,uri);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("audio/*");
        share.putExtra(Intent.EXTRA_STREAM,originalPath );
        context.startActivity(Intent.createChooser(share, "Share"));
    }

    private static String getRealPathFromURI(Context context, Uri contentUri) {
        Cursor cursor = null;
        try {
            String[] proj = { MediaStore.Images.Media.DATA };
            cursor = context.getContentResolver().query(contentUri,  proj, null, null, null);
            if(null == cursor) return "";
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            return cursor.getString(column_index);
        }
        finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }
}
