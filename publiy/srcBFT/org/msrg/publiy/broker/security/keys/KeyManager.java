package org.msrg.publiy.broker.security.keys;

import java.io.File;
import java.io.FilenameFilter;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

import org.msrg.publiy.broker.BrokerIdentityManager;

import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.security.dsa.DSAUtils;

public class KeyManager {

	public final static String PUB_KEY_EXT = ".pub";
	protected final String _keydir;
	protected final Map<String, KeyPair> _idKeysMap;
	protected final Map<InetSocketAddress, KeyPair> _addrKeysMap;
	
	public KeyManager(String keydir, BrokerIdentityManager idMan) {
		_keydir = keydir;
		_idKeysMap = new HashMap<String, KeyPair>();
		_addrKeysMap = new HashMap<InetSocketAddress, KeyPair>();

		loadKeysFromDir(_keydir, idMan);
	}
	
	public boolean loadKeysFromDir(String keydir, BrokerIdentityManager idMan) {
		if(keydir == null)
			return false;
		if(idMan == null)
			return false;
		
		File dir = new File(_keydir);
		String[] priKeyFilenames = dir.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return !name.endsWith(PUB_KEY_EXT);
			}
		});
		
		for(String priKeyFilename : priKeyFilenames) {
			priKeyFilename = _keydir + FileUtils.separatorChar + priKeyFilename;
			String pubKeyFilenames = priKeyFilename + PUB_KEY_EXT;
			PrivateKey priKey = DSAUtils.getInstance().loadPrivateKey(priKeyFilename);
			PublicKey pubKey = DSAUtils.getInstance().loadPublicKey(pubKeyFilenames);
			KeyPair kp = new KeyPair(pubKey, priKey);
			
			String id = getNodeIdFromKeyFilename(priKeyFilename);
			_idKeysMap.put(id, kp);
			InetSocketAddress addr = idMan.getNodeAddress(id);
			_addrKeysMap.put(addr, kp);
		}
		
		return true;
	}
	
	private String getNodeIdFromKeyFilename(String keyfile) {
		int i = keyfile.lastIndexOf(FileUtils.separatorChar);
		return i>0 ? keyfile.substring(i+1) : keyfile;
	}

	public synchronized PublicKey getPublicKey(String id) {
		KeyPair kp = _idKeysMap.get(id);
		return kp == null ? null : kp.getPublic();
	}
	
	public synchronized PublicKey getPublicKey(InetSocketAddress addr) {
		KeyPair kp = _addrKeysMap.get(addr);
		return kp == null ? null : kp.getPublic();
	}

	public synchronized PrivateKey getPrivateKey(String id) {
		KeyPair kp = _idKeysMap.get(id);
		return kp == null ? null : kp.getPrivate();
	}
	
	public synchronized PrivateKey getPrivateKey(InetSocketAddress addr) {
		KeyPair kp = _addrKeysMap.get(addr);
		return kp == null ? null : kp.getPrivate();
	}
	
	public synchronized KeyPair addKeyPair(InetSocketAddress remote, KeyPair kp) {
		return _addrKeysMap.put(remote, kp);
	}
	
	public DSAUtils getDSAUtils()  {
		return DSAUtils.getInstance();
	}
}
