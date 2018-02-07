package com.androidteam.playme.HelperModule;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

/**
 * Created by AJAY VERMA on 07/02/18.
 * Company : CACAO SOLUTIONS
 */

public class PermissionManager {

    public static boolean checkForStoragePermission(Context context){
        Activity activity = ((Activity)context);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if((ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(activity,new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                },1);
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }
}
