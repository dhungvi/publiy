package org.msrg.publiy.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.msrg.publiy.utils.security.dsa.DSAKeyGen;
import org.msrg.publiy.broker.security.keys.KeyManager;

public class FileUtils {

	public final static char separatorChar = '/'; //File.separatorChar;
	
	public static boolean directoryExists(String dirname) {
		File d = new File(dirname);
		return d.exists() && d.isDirectory();
	}
	
	public static boolean fileExists(String filename) {
		File f = new File(filename);
		return f.exists() && f.isFile();
	}
	
	public static boolean createDirectory(String dirname) {
		File d = new File(dirname);
		if(!d.exists())
			return d.mkdirs();
		
		return true;
	}
	
	public static boolean deleteDirectory(String dirname) {
		File d = new File(dirname);
		if(!d.exists())
			return true;
		if(d.isFile())
			return d.delete();
		for(String content : d.list()) {
			if(!deleteDirectory(dirname + FileUtils.separatorChar + content))
				return false;;
		}
		return d.delete();
	}
	
	public static boolean prepareTopologyDirectory(String basedir, String keysdir, int numNodes, String nodeNamePrefix, String ip, int portOffset) {
		if(!FileUtils.createDirectory(basedir))
			return false;
		
		if(!prepareKeysDirectory(keysdir, numNodes, nodeNamePrefix))
			return false;
		
		// Write identity file
		StringBuilder identityFileContent = new StringBuilder();
		String jpid = "";
		for(int i=0 ; i<numNodes ;i++) {
			String id = nodeNamePrefix + i;
			identityFileContent.append((i>0?"\n":"") + id + "\t" + new InetSocketAddress(ip, i+portOffset) + "\t" + jpid);
			jpid = id;
		}

		String identityfilename = basedir + FileUtils.separatorChar + "identityfile";
		try {
			OutputStream oStream = new FileOutputStream(identityfilename);
			oStream.write(identityFileContent.toString().getBytes());
			oStream.close();
			return true;
		} catch (IOException iox) {
			return false;
		}
	}

	public static boolean prepareKeysDirectory(String keysdir, int numNodes, String nodeNamePrefix) {
		if(!FileUtils.createDirectory(keysdir))
			return false;
		
		DSAKeyGen generator = new DSAKeyGen();
		
		// Generate key files (both pub&pri)
		for(int i=0 ; i<numNodes ; i++) {
			String priFilename = keysdir + FileUtils.separatorChar + nodeNamePrefix + i;
			String pubFilename = priFilename + KeyManager.PUB_KEY_EXT;
			if(generator.generateKeys(priFilename, pubFilename) == null)
				return false;
		}
		
		return true;
	}
}
