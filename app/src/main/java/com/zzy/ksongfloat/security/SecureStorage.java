package com.zzy.ksongfloat.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class SecureStorage {
    private static final String ANDROID_KEYSTORE="AndroidKeyStore";
    private static final String ALIAS="ksong_ai_api_key_aes";
    private static final String PREF="secure_ai";
    private static final String CIPHER="api_key_ciphertext";
    private static final String IV="api_key_iv";
    public static void saveApiKey(Context c, String key) throws Exception {
        if(key==null||key.trim().isEmpty()) { clearApiKey(c); return; }
        SecretKey sk=getOrCreateKey(); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,sk);
        byte[] enc=cipher.doFinal(key.getBytes(StandardCharsets.UTF_8)); byte[] iv=cipher.getIV();
        c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(CIPHER, Base64.encodeToString(enc,Base64.NO_WRAP)).putString(IV,Base64.encodeToString(iv,Base64.NO_WRAP)).apply();
    }
    public static String loadApiKey(Context c) throws Exception {
        SharedPreferences sp=c.getSharedPreferences(PREF,Context.MODE_PRIVATE); String e=sp.getString(CIPHER,""); String i=sp.getString(IV,""); if(e.isEmpty()||i.isEmpty()) return "";
        SecretKey sk=getOrCreateKey(); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.DECRYPT_MODE,sk,new GCMParameterSpec(128,Base64.decode(i,Base64.NO_WRAP)));
        byte[] dec=cipher.doFinal(Base64.decode(e,Base64.NO_WRAP)); return new String(dec,StandardCharsets.UTF_8);
    }
    public static boolean hasApiKey(Context c){ SharedPreferences sp=c.getSharedPreferences(PREF,Context.MODE_PRIVATE); return sp.contains(CIPHER)&&sp.contains(IV); }
    public static void clearApiKey(Context c){ c.getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().remove(CIPHER).remove(IV).apply(); }
    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore ks=KeyStore.getInstance(ANDROID_KEYSTORE); ks.load(null); if(ks.containsAlias(ALIAS)) return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
        KeyGenerator kg=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,ANDROID_KEYSTORE);
        kg.init(new KeyGenParameterSpec.Builder(ALIAS,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build());
        return kg.generateKey();
    }
}
