package org.msrg.publiy.tests.roundbasedrouting;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.utils.FileExtensionFilter;

public class PublicationSet {

	final InetSocketAddress _address;
	final Set<Publication> _publications;
	final static Pattern pubFilenamePattern = Pattern.compile("PubFile_(.*)_(.*)\\.pub");
	
	PublicationSet(InetSocketAddress address, String filename) throws IOException {
		_address = address;
		_publications = new HashSet<Publication>();
		BufferedReader reader = new BufferedReader(new FileReader(filename));
		while (true) {
			String line = reader.readLine();
			if (line==null) break;
			Publication publication = Publication.decode(line);
			
			_publications.add(publication);
		}
	}
	
	static Set<PublicationSet> createPublicationSets(String dirname) throws IOException {
		Set<PublicationSet> publicationSets = new HashSet<PublicationSet>();
		File dir = new File(dirname);
		if (!dir.isDirectory())
			throw new IllegalArgumentException("Directory does not exist: " + dirname);
		
		FilenameFilter filter = new FileExtensionFilter("", "pub");
		File[] topFiles = dir.listFiles(filter);
		for (int i=0 ; i<topFiles.length ; i++) {
			String pubFilename = topFiles[i].getName();
			Matcher matcher = pubFilenamePattern.matcher(pubFilename);
			
			if (!matcher.matches())
				throw new IllegalStateException("Filename does not match pattern: " + pubFilename);
			
			String addressStr = matcher.group(1);
			String portStr = matcher.group(2);
			
			InetSocketAddress address = new InetSocketAddress(addressStr, new Integer(portStr));
			publicationSets.add(new PublicationSet(address, dirname + File.separator + pubFilename));
		}
		return publicationSets;
	}
}

