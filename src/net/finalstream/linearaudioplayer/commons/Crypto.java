package net.finalstream.linearaudioplayer.commons;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Collections;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import android.annotation.SuppressLint;
import android.util.Base64;

public class Crypto {
/**
* Encrypt plain string and encode to Base64
*
* @param seed
* @param plain
* @return
* @throws Exception
*/
	
public static String encrypt(String seed, String plain) throws Exception {
byte[] rawKey = getRawKey(seed.getBytes());
byte[] encrypted = encrypt(rawKey, plain.getBytes());
return Base64.encodeToString(encrypted, Base64.DEFAULT);
}

/**
* Decrypt Base64 encoded encrypted string
*
* @param seed
* @param encrypted
* @return
* @throws Exception
*/
public static String decrypt(String seed, String encrypted)
throws Exception {
byte[] rawKey = getRawKey(seed.getBytes());
byte[] enc = Base64.decode(encrypted.getBytes(), Base64.DEFAULT);
byte[] result = decrypt(rawKey, enc);
return new String(result);
}

private static byte[] getRawKey(byte[] seed) throws Exception {
KeyGenerator keygen = KeyGenerator.getInstance("AES");
//SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
SecureRandom random = null;
if (android.os.Build.VERSION.SDK_INT >= 17) {
	// 4.2 or higher
	random = SecureRandom.getInstance("SHA1PRNG", "Crypto");
} else {
random = SecureRandom.getInstance("SHA1PRNG");
}
random.setSeed(seed);
keygen.init(128, random); // 192 and 256 bits may not be available
SecretKey key = keygen.generateKey();
byte[] raw = key.getEncoded();
return raw;
}

private static byte[] encrypt(byte[] raw, byte[] plain) throws Exception {
SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
Cipher cipher = Cipher.getInstance("AES");
cipher.init(Cipher.ENCRYPT_MODE, keySpec);
byte[] encrypted = cipher.doFinal(plain);
//byte[] encrypted = cipher.doFinal();
return encrypted;
}

private static byte[] decrypt(byte[] raw, byte[] encrypted)
throws Exception {
SecretKeySpec keySpec = new SecretKeySpec(raw, "AES");
Cipher cipher = Cipher.getInstance("AES");
cipher.init(Cipher.DECRYPT_MODE, keySpec);
cipher.update(encrypted);
byte[] decrypted = cipher.doFinal();
//byte[] decrypted = cipher.doFinal(encrypted);
return decrypted;
}

/*
@SuppressLint("NewApi")
public static String encrypt(final String symKeyHex, final String plainMessage
        ) {
    byte[] symKeyData = null;
    
    SecretKeyFactory factory = null;
	try {
		factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	} catch (NoSuchAlgorithmException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
    KeySpec spec = new PBEKeySpec(symKeyHex.toCharArray(), new byte[]{2}, 65536, 256);
    try {
		SecretKey tmp = factory.generateSecret(spec);
		symKeyData = tmp.getEncoded();
    } catch (InvalidKeySpecException e3) {
		// TODO Auto-generated catch block
		e3.printStackTrace();
	}
    
	try {
		byte[] bytes = new byte[32]; 
		symKeyData = Base64.encode(symKeyHex.getBytes("UTF-8"), Base64.DEFAULT);
		symKeyData = Arrays.copyOfRange(symKeyData, 0, 32);
	} catch (UnsupportedEncodingException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}

    byte[] encodedMessage = null;
	try {
		encodedMessage = plainMessage.getBytes("UTF-8");
	} catch (UnsupportedEncodingException e1) {
		// TODO Auto-generated catch block
		e1.printStackTrace();
	}
    try {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final int blockSize = cipher.getBlockSize();

        // create the key
        final SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

        // generate random IV using block size (possibly create a method for
        // this)
        final byte[] ivData = new byte[blockSize];
        final SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
        rnd.nextBytes(ivData);
        final IvParameterSpec iv = new IvParameterSpec(ivData);

        cipher.init(Cipher.ENCRYPT_MODE, symKey, iv);

        final byte[] encryptedMessage = cipher.doFinal(encodedMessage);

        // concatenate IV and encrypted message
        final byte[] ivAndEncryptedMessage = new byte[ivData.length
                + encryptedMessage.length];
        System.arraycopy(ivData, 0, ivAndEncryptedMessage, 0, blockSize);
        System.arraycopy(encryptedMessage, 0, ivAndEncryptedMessage,
                blockSize, encryptedMessage.length);

        final String ivAndEncryptedMessageBase64 = Base64
        		.encodeToString(ivAndEncryptedMessage, Base64.DEFAULT);

        return ivAndEncryptedMessageBase64;
    } catch (InvalidKeyException e) {
        throw new IllegalArgumentException(
                "key argument does not contain a valid AES key");
    } catch (GeneralSecurityException e) {
        throw new IllegalStateException(
                "Unexpected exception during encryption", e);
    }
}*/

/*
@SuppressLint("NewApi")
public static String decrypt(final String symKeyHex, final String ivAndEncryptedMessageBase64
        ) {
	
    byte[] symKeyData = Base64.encode(symKeyHex.getBytes(), Base64.DEFAULT);
    symKeyData = Arrays.copyOfRange(symKeyData, 0, 32);
    
    /*
	byte[] symKeyData = null;
	SecretKeyFactory factory = null;
	try {
		factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
	} catch (NoSuchAlgorithmException e2) {
		// TODO Auto-generated catch block
		e2.printStackTrace();
	}
    KeySpec spec = new PBEKeySpec(symKeyHex.toCharArray(), new byte[]{2}, 65536, 256);
    try {
		SecretKey tmp = factory.generateSecret(spec);
		symKeyData = tmp.getEncoded();
    } catch (InvalidKeySpecException e3) {
		// TODO Auto-generated catch block
		e3.printStackTrace();
	}
	
	
    final byte[] ivAndEncryptedMessage = Base64.encode(ivAndEncryptedMessageBase64.getBytes(), Base64.DEFAULT);
    try {
        final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        final int blockSize = cipher.getBlockSize();

        // create the key
        final SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

        // retrieve random IV from start of the received message
        final byte[] ivData = new byte[blockSize];
        System.arraycopy(ivAndEncryptedMessage, 0, ivData, 0, blockSize);
        final IvParameterSpec iv = new IvParameterSpec(ivData);

        // retrieve the encrypted message itself
        final byte[] encryptedMessage = new byte[ivAndEncryptedMessage.length
                - blockSize];
        System.arraycopy(ivAndEncryptedMessage, blockSize,
                encryptedMessage, 0, encryptedMessage.length);

        cipher.init(Cipher.DECRYPT_MODE, symKey, iv);

        final byte[] encodedMessage = cipher.doFinal(encryptedMessage);

        // concatenate IV and encrypted message
        final String message = new String(encodedMessage, "UTF-8");

        return message;
    } catch (InvalidKeyException e) {
        throw new IllegalArgumentException(
                "key argument does not contain a valid AES key");
    } catch (BadPaddingException e) {
        // you'd better know about padding oracle attacks
        return null;
    } catch (GeneralSecurityException e) {
        throw new IllegalStateException(
                "Unexpected exception during decryption", e);
    } catch (UnsupportedEncodingException e) {
		// TODO Auto-generated catch block
		return null;
	}
}*/


public static String toHex(String arg) {
	  return String.format("%x", new BigInteger(1, arg.getBytes()));
}

}


