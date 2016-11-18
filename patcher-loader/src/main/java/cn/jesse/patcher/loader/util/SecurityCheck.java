package cn.jesse.patcher.loader.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import cn.jesse.patcher.loader.Constants;
import cn.jesse.patcher.loader.PatcherRuntimeException;

/**
 * Created by jesse on 15/11/2016.
 */
public class SecurityCheck {
    private static final String TAG = Constants.LOADER_TAG + "SecurityCheck";
    /**
     * static to faster
     * public key
     */
    private static       PublicKey mPublicKey = null;

    private final Context                 mContext;
    private final HashMap<String, String> metaContentMap;
    private       HashMap<String, String> packageProperties;

    public SecurityCheck(Context context) {
        mContext = context;
        metaContentMap = new HashMap<>();

        if (mPublicKey == null) {
            init(mContext);
        }
    }

    public HashMap<String, String> getMetaContentMap() {
        return metaContentMap;
    }

    /**
     * Nullable
     *
     * @return HashMap<String, String>
     */
    public HashMap<String, String> getPackagePropertiesIfPresent() {
        if (packageProperties != null) {
            return packageProperties;
        }

        String property = metaContentMap.get(Constants.PACKAGE_META_FILE);

        if (property == null) {
            return null;
        }

        String[] lines = property.split("\n");
        for (final String line : lines) {
            if (line == null || line.length() <= 0) {
                continue;
            }
            //it is comment
            if (line.startsWith("#")) {
                continue;
            }
            final String[] kv = line.split("=", 2);
            if (kv == null || kv.length < 2) {
                continue;
            }
            if (packageProperties == null) {
                packageProperties = new HashMap<>();
            }
            packageProperties.put(kv[0].trim(), kv[1].trim());
        }
        return packageProperties;
    }

    public boolean verifyPatchMetaSignature(File path) {
        if (path == null || !path.isFile() || !path.exists() || path.length() == 0) {
            return false;
        }
        JarFile jarFile = null;
        try {
            jarFile = new JarFile(path);
            final Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry jarEntry = entries.nextElement();
                // no code
                if (jarEntry == null) {
                    continue;
                }

                final String name = jarEntry.getName();
                if (name.startsWith("META-INF/")) {
                    continue;
                }
                //for faster, only check the meta.txt files
                //we will check other files's mad5 written in meta files
                //只校验meta.txt结尾的文件的签名
                if (!name.endsWith(Constants.META_SUFFIX)) {
                    continue;
                }
                //将meta.txt文件内容存入内存,供外部验证或加载.
                metaContentMap.put(name, PatchFileUtil.loadDigestes(jarFile, jarEntry));
                Certificate[] certs = jarEntry.getCertificates();
                if (certs == null) {
                    return false;
                }
                if (!check(path, certs)) {
                    return false;
                }
            }
        } catch (Exception e) {
            throw new PatcherRuntimeException(
                    String.format("ShareSecurityCheck file %s, size %d verifyPatchMetaSignature fail", path.getAbsolutePath(), path.length()), e);
        } finally {
            try {
                if (jarFile != null) {
                    jarFile.close();
                }
            } catch (IOException e) {
                Log.e(TAG, path.getAbsolutePath(), e);
            }
        }
        return true;
    }


    // verify the signature of the Apk
    private boolean check(File path, Certificate[] certs) {
        if (certs.length > 0) {
            for (int i = certs.length - 1; i >= 0; i--) {
                try {
                    certs[i].verify(mPublicKey);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, path.getAbsolutePath(), e);
                }
            }
        }
        return false;
    }

    @SuppressLint("PackageManagerGetSignatures")
    private void init(Context context) {
        ByteArrayInputStream stream = null;
        try {
            PackageManager pm = context.getPackageManager();
            String packageName = context.getPackageName();
            PackageInfo packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
            stream = new ByteArrayInputStream(packageInfo.signatures[0].toByteArray());
            X509Certificate cert = (X509Certificate) certFactory.generateCertificate(stream);
            mPublicKey = cert.getPublicKey();
        } catch (Exception e) {
            throw new PatcherRuntimeException("ShareSecurityCheck init public key fail", e);
        } finally {
            PatchFileUtil.closeQuietly(stream);
        }
    }
}
