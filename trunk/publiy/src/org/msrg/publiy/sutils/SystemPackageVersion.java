package org.msrg.publiy.sutils;

public class SystemPackageVersion {
	public static void main(String[] argv){
		String version = SystemPackageVersion.getVersion();
		System.out.print(version);
	}
	
	public static String getVersion(){
		Package pack = SystemPackageVersion.class.getPackage();
		String version = pack.getImplementationVersion();
		if ( version == null )
			return "-1";
		else
			return version;
	}
}
