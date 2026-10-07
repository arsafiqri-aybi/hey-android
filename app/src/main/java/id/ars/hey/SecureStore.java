package id.ars.hey;
import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class SecureStore {
  private final SharedPreferences prefs;
  private final String alias="hey-device-v1";
  SecureStore(Context c){prefs=c.getSharedPreferences("hey.secure",Context.MODE_PRIVATE);}
  private SecretKey key() throws Exception {
    KeyStore s=KeyStore.getInstance("AndroidKeyStore");s.load(null);
    if(!s.containsAlias(alias)){KeyGenerator g=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
    return (SecretKey)s.getKey(alias,null);
  }
  synchronized void put(String name,String value) throws Exception {
    Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());byte[] iv=c.getIV(),data=c.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    String payload=Base64.encodeToString(iv,Base64.NO_WRAP)+":"+Base64.encodeToString(data,Base64.NO_WRAP);
    if(!prefs.edit().putString(name,payload).commit())throw new IllegalStateException("SECURE_PERSIST_FAILED");
  }
  synchronized String get(String name,String fallback){
    try{String p=prefs.getString(name,null);if(p==null)return fallback;String[] parts=p.split(":",2);Cipher c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(c.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return fallback;}
  }
  synchronized void remove(String name){prefs.edit().remove(name).commit();}
}
