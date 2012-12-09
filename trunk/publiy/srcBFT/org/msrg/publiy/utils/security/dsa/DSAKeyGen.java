package org.msrg.publiy.utils.security.dsa;

import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;

public class DSAKeyGen {

	protected static DSAKeyGen _instance;
	
	private static final String PUB_KEY_EXT = ".pub";
	private static final String KEY_ALG = "DSA";
	private static final int KEY_SIZE = 1024;
	
	public static synchronized DSAKeyGen getInstance() {
		if(_instance == null)
			_instance = new DSAKeyGen();
		
		return _instance;
	}
	
	public KeyPair generateKeys(final String priFilename, final String pubFilename) {
		try {
			KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance(KEY_ALG);
			SecureRandom rand = SecureRandom.getInstance("SHA1PRNG");
			keyPairGen.initialize(KEY_SIZE, rand);
			KeyPair keyPair = keyPairGen.generateKeyPair();
			PrivateKey priKey = keyPair.getPrivate();
			byte[] priKeyBytes = priKey.getEncoded();
			writeToFile(priKeyBytes, priFilename);
			
			PublicKey pubKey = keyPair.getPublic();
			byte[] pubKeyBytes = pubKey.getEncoded();
			writeToFile(pubKeyBytes, pubFilename);
			
			return keyPair;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected void writeToFile(byte[] data, final String filename) throws IOException {
		if(filename == null)
			return;
		
		FileOutputStream foutStream = new FileOutputStream(filename);
		foutStream.write(data);
		foutStream.close();
	}
	
	public static void main(String[] argv) {
		// Arguments are private key path
		for(int i=0 ; i<argv.length ; i++) {
			String priKeyFilename = argv[i];
			String publicKeyFile = priKeyFilename + PUB_KEY_EXT;
			
			DSAKeyGen generator = new DSAKeyGen();
			KeyPair kp = generator.generateKeys(priKeyFilename, publicKeyFile);
			if(kp == null)
				System.err.println("Generating key might have failed: " + priKeyFilename);
		}
	}
}
