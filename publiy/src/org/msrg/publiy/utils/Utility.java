package org.msrg.publiy.utils;

import java.net.InetSocketAddress;

public class Utility {

	public static InetSocketAddress stringToInetSocketAddress(String str) {
		
		if ( str == null )
			return null;
		
		if ( str.equalsIgnoreCase("") )
			return null;
		
		if ( str.equalsIgnoreCase("null") )
			return null;
		
		str = str.trim();
		String ip;
		if ( str.charAt(0) == '/')
			ip = str.substring(1, str.indexOf(":"));
		else
			ip = str.substring(0, str.indexOf(":"));
		String p = str.substring(str.indexOf(":")+1, str.length());
		Integer port = new Integer(p);
		
		return new InetSocketAddress(ip, port);
	}
	
	public static long max(int a, int b) { return a > b ? a : b; }
	public static long max(long a, long b) { return a > b ? a : b; }
	public static double max(double a, double b) { return a > b ? a : b; }
	public static double min(double a, double b) { return a < b ? a : b; }
}
