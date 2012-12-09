package org.msrg.publiy.tests.commons;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;

public class FileCommons {

	public static String[] getLines(String filename){
		List<String> lines = new LinkedList<String>();
		
		FileReader freader = null;
		BufferedReader breader = null;
		try{
			freader = new FileReader(filename);
			breader = new BufferedReader(freader);
			String line;
			while ( (line= breader.readLine()) != null )
				lines.add(line);
		}catch(IOException iox){
			iox.printStackTrace();
			try{
				if ( freader!= null )
					freader.close();
				if ( breader!=null )
					breader.close();
			}catch(IOException iox2){};
			return null;
		}
		
		return lines.toArray(new String[0]);
	}
	
	public static InetSocketAddress[] getAllInetAddressesFromLines(LocalSequencer localSequencer, String filename){
		String[] lines = getLines(filename);
		Set<InetSocketAddress> addressSet = new HashSet<InetSocketAddress>();
		
		for ( int i=0 ; i<lines.length ; i++ )
		{
			TRecovery_Join trj = TRecovery_Join.getTRecoveryObject(localSequencer, lines[i]);
			addressSet.add(trj.getJoiningNode());
			addressSet.add(trj.getJoinPoint());
		}
		
		return addressSet.toArray(new InetSocketAddress[0]);
	}
}
