package com.adbstudio;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Method;

import org.json.JSONArray;
import org.json.JSONObject;

public class IconExtractor {

    private static final String ICON_CACHE_DIR = "/data/local/tmp/adbstudio/icons";
    private static final String TAG = "AdbStudioExtractor";

    private Object packageManager;
    private Method getPackageInfoMethod;
    private int sdkInt;

    public static void main(String[] args) {
        try {
            new File(ICON_CACHE_DIR).mkdirs();
            IconExtractor extractor = new IconExtractor();
            JSONArray results = new JSONArray();
            for (String pkgName : args) {
                try {
                    results.put(extractor.extract(pkgName));
                } catch (Exception e) {
                    Log.e(TAG, "Failed: " + pkgName, e);
                }
            }
            JSONObject output = new JSONObject();
            output.put("packageInfos", results);
            System.out.println(output.toString());
        } catch (Exception e) {
            Log.e(TAG, "Fatal error", e);
            System.exit(1);
        }
    }

    private JSONObject extract(String packageName) throws Exception {
        if (packageManager == null) initPackageManager();

        int flags = PackageManager.GET_ACTIVITIES;
        if (sdkInt >= 28) flags |= PackageManager.GET_SIGNING_CERTIFICATES;
        else flags |= PackageManager.GET_SIGNATURES;

        PackageInfo pkgInfo = (PackageInfo) getPackageInfoMethod.invoke(
            packageManager, packageName, (long) flags, 0);
        ApplicationInfo appInfo = pkgInfo.applicationInfo;
        String apkPath = appInfo.sourceDir;
        long apkSize = new File(apkPath).length();

        JSONObject info = new JSONObject();
        info.put("packageName", packageName);
        info.put("apkSize", apkSize);

        String cacheKey = packageName + "." + apkSize;
        String label = packageName;
        String iconPath = "";

        Resources resources = getResources(apkPath);

        if (appInfo.labelRes != 0) {
            try { label = resources.getString(appInfo.labelRes); }
            catch (Exception e) { Log.w(TAG, "No label for " + packageName); }
        }

        if (appInfo.icon != 0) {
            try {
                iconPath = ICON_CACHE_DIR + "/" + cacheKey + ".png";
                File iconFile = new File(iconPath);
                if (!iconFile.exists()) {
                    Drawable drawable = resources.getDrawable(appInfo.icon);
                    Bitmap bitmap = drawableToBitmap(drawable);
                    byte[] pngData = bitmapToPng(bitmap, 20);
                    FileOutputStream fos = new FileOutputStream(iconFile);
                    try { fos.write(pngData); } finally { fos.close(); }
                }
            } catch (Exception e) {
                Log.w(TAG, "No icon for " + packageName);
                iconPath = "";
            }
        }

        info.put("label", label);
        info.put("iconCachePath", iconPath);
        return info;
    }

    private void initPackageManager() throws Exception {
        sdkInt = Build.VERSION.SDK_INT;
        Class<?> smClass = Class.forName("android.os.ServiceManager");
        Method getService = smClass.getMethod("getService", String.class);
        IBinder binder = (IBinder) getService.invoke(null, "package");
        Class<?> stubClass = Class.forName("android.content.pm.IPackageManager$Stub");
        Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
        packageManager = asInterface.invoke(null, binder);
        getPackageInfoMethod = packageManager.getClass().getMethod(
            "getPackageInfo", String.class, Long.TYPE, Integer.TYPE);
    }

    private Resources getResources(String apkPath) throws Exception {
        AssetManager assetManager = (AssetManager) AssetManager.class.newInstance();
        Method addAssetPath = assetManager.getClass().getMethod("addAssetPath", String.class);
        addAssetPath.invoke(assetManager, apkPath);
        DisplayMetrics dm = new DisplayMetrics();
        dm.setToDefaults();
        Configuration config = new Configuration();
        config.setToDefaults();
        return new Resources(assetManager, dm, config);
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        int w = Math.max(drawable.getIntrinsicWidth(), 1);
        int h = Math.max(drawable.getIntrinsicHeight(), 1);
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setHasAlpha(true);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, w, h);
        drawable.draw(canvas);
        return bitmap;
    }

    private byte[] bitmapToPng(Bitmap bitmap, int quality) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, quality, stream);
        return stream.toByteArray();
    }
}
