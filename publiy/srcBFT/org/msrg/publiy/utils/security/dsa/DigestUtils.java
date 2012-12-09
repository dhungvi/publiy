package org.msrg.publiy.utils.security.dsa;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

	protected final int _digestByteLength;
	protected static DigestUtils _instance;
	
	public DigestUtils(int digestSize) {
		_digestByteLength = digestSize;
	}
	
	public DigestUtils() {
		this(32);
	}
	
	public int getDigestByteLength() {
		return _digestByteLength;
	}
	
	public byte[] getDigest(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-" + (_digestByteLength * 8));
			return md.digest(data);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static synchronized DigestUtils getInstance() {
		if(_instance == null)
			_instance = new DigestUtils();
			
		return _instance;
	}
}
