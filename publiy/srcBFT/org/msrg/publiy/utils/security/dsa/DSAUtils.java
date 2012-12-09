package org.msrg.publiy.utils.security.dsa;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

public class DSAUtils {

	private static DSAUtils _instance;
	private static final String SIG_ALG = "SHA1withDSA";
	private static final String KEY_ALG = "DSA";

	public boolean verifyDataFake(byte[] data, byte[] signedData, PublicKey pubKey) {
		int len = getSignedLen(data.length);
		if(len != signedData.length)
			return false;
		for(int i=0 ; i<len ; i++)
			if(data[i] != signedData[i])
				return false;
		return true;
	}
	
	public byte[] signDataFake(byte[] data, PrivateKey priKey) {
		int len = getSignedLen(data.length);
		byte[] ret = new byte[len];
		for(int i=0 ; i<len ; i++)
			ret[i] = data[i];
		return ret;
	}
	
	private int getSignedLen(int len) {
		return len < 32 ? len : 32;
	}

	public boolean verifyData(byte[] data, byte[] signedData, PublicKey pubKey) {
		boolean ret = verifyDataPrivate(data, signedData, pubKey);
		return ret;
	}
	
	private boolean verifyDataPrivate(byte[] data, byte[] signedData, PublicKey pubKey) {
		Signature dsaSig;
		try {
			dsaSig = Signature.getInstance(SIG_ALG);
			dsaSig.initVerify(pubKey);
			dsaSig.update(data);
			return dsaSig.verify(signedData);
		} catch (NoSuchAlgorithmException e) {
//			e.printStackTrace();
			return false;
		} catch (InvalidKeyException e) {
//			e.printStackTrace();
			return false;
		} catch (SignatureException e) {
//			e.printStackTrace();
			return false;
		}
	}
	
	public byte[] signData(byte[] data, PrivateKey priKey) {
		byte[] ret = signDataPrivate(data, priKey);
		return ret;
	}
	
	private byte[] signDataPrivate(byte[] data, PrivateKey priKey) {
		try {
			Signature dsaSig = Signature.getInstance(SIG_ALG);
			dsaSig.initSign(priKey);
			dsaSig.update(data);
			return dsaSig.sign();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeyException e) {
			e.printStackTrace();
			return null;
		} catch (SignatureException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public PublicKey loadPublicKey(final String pubFilename) {
		try {
			FileInputStream finStream = new FileInputStream(pubFilename);
			byte[] pubKeyBytes = new byte[finStream.available()];
			finStream.read(pubKeyBytes);
			finStream.close();
			X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(pubKeyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALG);
			PublicKey priKey = keyFactory.generatePublic(pubKeySpec);
			return priKey;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public PrivateKey loadPrivateKey(final String priFilename) {
		try {
			FileInputStream finStream = new FileInputStream(priFilename);
			byte[] priKeyBytes = new byte[finStream.available()];
			finStream.read(priKeyBytes);
			finStream.close();
			PKCS8EncodedKeySpec priKeySpec = new PKCS8EncodedKeySpec(priKeyBytes);
			KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALG);
			PrivateKey priKey = keyFactory.generatePrivate(priKeySpec);
			return priKey;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static synchronized DSAUtils getInstance() {
		if(_instance == null)
			_instance = new DSAUtils();
		
		return _instance;
	}
}
