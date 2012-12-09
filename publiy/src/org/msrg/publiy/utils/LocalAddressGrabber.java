package org.msrg.publiy.utils;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class LocalAddressGrabber {

	public static Set<String> grabNetworkInterfaceAddressesAsStrings(){
		Set<String> stringAddresses = new HashSet<String>();
		Set<InetAddress> addresses = grabNetworkInterfaceAddresses();
		for ( Iterator<InetAddress> it=addresses.iterator() ; it.hasNext() ; )
			stringAddresses.add(it.next().getHostAddress());
		
		return stringAddresses;
	}
	
	public static Set<InetAddress> grabNetworkInterfaceAddresses(){
		try{
			Enumeration<NetworkInterface> iNetworkEnum = NetworkInterface.getNetworkInterfaces();
			Set<InetAddress> iNetAddressSet = new HashSet<InetAddress>();
			while ( iNetworkEnum.hasMoreElements() ){
				NetworkInterface iNetwork = iNetworkEnum.nextElement();
				Enumeration<InetAddress> iNetAddressEnum = iNetwork.getInetAddresses();
				while (iNetAddressEnum.hasMoreElements() ){
					InetAddress iNetAddress = iNetAddressEnum.nextElement();
					if ( Inet4Address.class.isInstance(iNetAddress) )
						iNetAddressSet.add(iNetAddress);
				}
			}

			return iNetAddressSet;
		}catch(SocketException sockEx){
			return null;
		}
	}
	
	public static Set<Inet4Address> grabAllUp4LoopbackAddresses(){
		Set<Inet4Address> allSet = grabAll4Addresses(true);
		if ( allSet == null )
			return null;
		
		Set<Inet4Address> retSet = new HashSet<Inet4Address>();
		Iterator<Inet4Address> allIt = allSet.iterator();
		while ( allIt.hasNext() ){
			Inet4Address address = allIt.next();
			if ( address.isLoopbackAddress() )
				retSet.add(address);
		}
		
		return retSet;
	}
	
	public static Set<Inet4Address> grabAllUp4NonLoopbackAddresses(){
		Set<Inet4Address> allSet = grabAll4Addresses(true);
		if ( allSet == null )
			return null;
		
		Set<Inet4Address> retSet = new HashSet<Inet4Address>();
		Iterator<Inet4Address> allIt = allSet.iterator();
		while ( allIt.hasNext() ){
			Inet4Address address = allIt.next();
			if ( !address.isLoopbackAddress() )
				retSet.add(address);
		}
		
		return retSet;
	}
	
	public static boolean isLocal(InetSocketAddress remote) {
		return grabAll4Addresses(true).contains(remote.getAddress());
	}
	
	public static boolean isLocal(String remote) {
		return grabAll4Addresses(true).contains(remote);
	}
	
	public static Set<Inet4Address> grabAll4Addresses(boolean up){
		Set<InetAddress> allAddressSet = grabNetworkInterfaceAddresses();
		if ( allAddressSet == null )
			return null;
		
		Set<Inet4Address> allUp4Set = new HashSet<Inet4Address>();
		Iterator<InetAddress> allAddressIt = allAddressSet.iterator();
		while ( allAddressIt.hasNext() )
		{
			InetAddress address = allAddressIt.next();
			if ( !Inet4Address.class.isInstance(address) )
				continue;
			
			try{
				NetworkInterface iNetwork = NetworkInterface.getByInetAddress(address);
				if ( !up || iNetwork.isUp() )
					allUp4Set.add((Inet4Address)address);
			}catch(SocketException sockEx){
				continue;
			}
			
		}
		return allUp4Set;
	}

	public static void main(String[] argv){
//		Set<Inet4Address> addressSet = grabAll4Addresses(true);
		Set<Inet4Address> addressSet = grabAll4Addresses(false);
		Inet4Address addresses[] = addressSet.toArray(new Inet4Address[0]);
		for ( int i=0 ; i<addresses.length ; i++ )
			System.out.println(addresses[i]); // OK
		
//		Set<Inet4Address> nonLoopbackAddresses = grabAllUp4NonLoopbackAddresses();
//		System.out.println(nonLoopbackAddresses);
		
//		Set<Inet4Address> loopbackAddresses = grabAllUp4LoopbackAddresses();
//		System.out.println(loopbackAddresses);
	}
}	
