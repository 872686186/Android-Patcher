package cn.jesse.patcher.lib.patcher;

import android.content.Context;

import java.io.File;

import cn.jesse.patcher.lib.util.PatcherLog;
import cn.jesse.patcher.loader.Constants;
import cn.jesse.patcher.loader.PatcherRuntimeException;
import cn.jesse.patcher.loader.util.PatchFileUtil;

/**
 * Created by jesse on 05/12/2016.
 */

public class PatcherInstaller {
    private static final String TAG = Constants.LOADER_TAG + "PatcherInstaller";
    /**
     * sample usage for native library
     *
     * @param context
     * @param relativePath such as lib/armeabi
     * @param libname      for the lib libTest.so, you can pass Test or libTest, or libTest.so
     * @return boolean
     * @throws UnsatisfiedLinkError
     */
    public static boolean loadLibraryFromPatcher(Context context, String relativePath, String libname) throws UnsatisfiedLinkError {
        final Patcher patcher = Patcher.with(context);

        libname = libname.startsWith("lib") ? libname : "lib" + libname;
        libname = libname.endsWith(".so") ? libname : libname + ".so";
        String relativeLibPath = relativePath + "/" + libname;

        //TODO we should add cpu abi, and the real path later
        if (patcher.isEnabledForNativeLib() && patcher.isPatcherLoaded()) {
            PatcherLoadResult loadResult = patcher.getPatcherLoadResultIfPresent();
            if (loadResult.libs != null) {
                for (String name : loadResult.libs.keySet()) {
                    if (name.equals(relativeLibPath)) {
                        String patchLibraryPath = loadResult.libraryDirectory + "/" + name;
                        File library = new File(patchLibraryPath);
                        if (library.exists()) {
                            //whether we check md5 when load
                            boolean verifyMd5 = patcher.isPatcherLoadVerify();
                            if (verifyMd5 && !PatchFileUtil.verifyFileMd5(library, loadResult.libs.get(name))) {
                                patcher.getLoadReporter().onLoadFileMd5Mismatch(library, Constants.TYPE_LIBRARY);
                            } else {
                                System.load(patchLibraryPath);
                                PatcherLog.i(TAG, "loadLibraryFromPatcher success:" + patchLibraryPath);
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    /**
     * you can use PatcherInstaller.loadLibrary replace your System.loadLibrary for auto update library!
     * only support auto load lib/armeabi library from patch.
     * for other library in lib/* or assets,
     * you can load through {@code PatcherInstaller#loadLibraryFromPatcher}
     */
    public static void loadArmLibrary(Context context, String libName) {
        if (libName == null || libName.isEmpty() || context == null) {
            throw new PatcherRuntimeException("libName or context is null!");
        }

        Patcher patcher = Patcher.with(context);
        if (patcher.isEnabledForNativeLib()) {
            if (PatcherInstaller.loadLibraryFromPatcher(context, "lib/armeabi", libName)) {
                return;
            }

        }
        System.loadLibrary(libName);
    }

    /**
     * you can use PatcherInstaller.loadArmV7Library replace your System.loadLibrary for auto update library!
     * only support auto load lib/armeabi-v7a library from patch.
     * for other library in lib/* or assets,
     * you can load through {@code PatcherInstaller#loadLibraryFromPatcher}
     */
    public static void loadArmV7Library(Context context, String libName) {
        if (libName == null || libName.isEmpty() || context == null) {
            throw new PatcherRuntimeException("libName or context is null!");
        }

        Patcher patcher = Patcher.with(context);
        if (patcher.isEnabledForNativeLib()) {
            if (PatcherInstaller.loadLibraryFromPatcher(context, "lib/armeabi-v7a", libName)) {
                return;
            }

        }
        System.loadLibrary(libName);
    }
}
