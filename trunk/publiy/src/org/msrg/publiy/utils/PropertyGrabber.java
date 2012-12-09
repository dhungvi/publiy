package org.msrg.publiy.utils;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.msrg.publiy.broker.PubForwardingStrategy;

import org.msrg.publiy.broker.core.nodes.OverlayNodeFactory;
import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.gui.failuretimeline.FailureTimelineEvents;
import org.msrg.publiy.node.NodeTypes;


public class PropertyGrabber {
	
	public static final String PROPERTY_FILE_EXTENSION = ".properties";
	public static final String PROPERTY_REMOVED_FILE_EXTENSION = "._properties";
	
	public static final String PROPERTY_GUI_WIDTH = "w";
	public static final String PROPERTY_GUI_HEIGHT = "h";
	public static final String PROPERTY_GUI_X = "x";
	public static final String PROPERTY_GUI_Y = "y";
	public static final String PROPERTY_KEYS_DIR = "keysdir";
	
	public static final String PROPERTY_STRATEGY = "Strategy";
	public static final String PROPERTY_NODE_NAME = "NodeName";
	public static final String PROPERTY_NODE_ADDRESS = "NodeAddress";
	public static final String PROPERTY_NODE_TYPE = "NodeType";
	public static final String PROPERTY_JOINPOINT_NAME = "JoinPointName";
	public static final String PROPERTY_JOINPOINT_ADDRESS = "JoinPointAddress";
	public static final String PROPERTY_FAILURE_PROFILE = "FailureProfile";
	public static final String PROPERTY_PUB_FILENAMES = "PubFilenames";
	public static final String PROPERTY_SUB_FILENAMES = "SubFilenames";
	public static final String PROPERTY_DELAY = "Delay";
	
	public static final int DEFAULT_PUBLISHER_DELAY = 1000;
	public static final int DEFAULT_SUBSCRIBER_DELAY = 1;

	public final static String DEFAULT_PUBLICATIONS_FILE_EXTENTION = ".sub";
	public final static String[] DEFAULT_SUBSCRIPTION_FILENAMES = PropertyGrabber.getFileNamesRelative("." + FileUtils.separatorChar + "data", DEFAULT_PUBLICATIONS_FILE_EXTENTION);
	public final static String PROPERTY_BFT_MANIPULAE = "BFTMessageManipulate";

	public static void setNodeAddress(Properties prop, InetSocketAddress newNodeAddress){
		if ( newNodeAddress == null )
			prop.put(PROPERTY_NODE_ADDRESS, "null");
		else
			prop.put(PROPERTY_NODE_ADDRESS, newNodeAddress.toString());
	}
	
	public static InetSocketAddress getNodeAddress(Properties prop){
		String nodeAddressStr = prop.getProperty(PROPERTY_NODE_ADDRESS);
		InetSocketAddress nodeAddress = stringToInetSocketAddress(nodeAddressStr);
		
		return nodeAddress;
	}
	
	public static void setStrategy(Properties prop, String newNodeName){
		prop.put(PROPERTY_STRATEGY, newNodeName);
	}

	public static PubForwardingStrategy getStrategy(Properties prop){
		String strategyStr = prop.getProperty(PROPERTY_STRATEGY);
		return PubForwardingStrategy.getBrokerForwardingStrategy(strategyStr);
	}
	
	public static void setNodeName(Properties prop, String newNodeName){
		prop.put(PROPERTY_NODE_NAME, newNodeName);
	}

	public static String getNodeName(Properties prop){
		String nodeNameStr = prop.getProperty(PROPERTY_NODE_NAME);
		return nodeNameStr;
	}

	public static void setJoinPointName(Properties prop, String newJoinPointName){
		prop.put(PROPERTY_JOINPOINT_NAME, newJoinPointName);
	}
	
	public static String getJoinPointName(Properties prop){
		String nodeNameStr = prop.getProperty(PROPERTY_JOINPOINT_NAME);
		if ( nodeNameStr == null || nodeNameStr.equalsIgnoreCase("null") )
			return null;
		
		return nodeNameStr;
	}

	public static String getPubFileNames(Properties prop){
		String filenameStr = prop.getProperty(PROPERTY_PUB_FILENAMES);
		return filenameStr;
	}
	
	public static void setPubFileNames(Properties prop, String filename){
		prop.setProperty(PROPERTY_PUB_FILENAMES, filename);
	}
	
	public static String getSubFileName(Properties prop){
		String filenameStr = prop.getProperty(PROPERTY_SUB_FILENAMES);
		return filenameStr;
	}
	
	public static void setSubFileNames(Properties prop, String filename){
		prop.setProperty(PROPERTY_SUB_FILENAMES, filename);
	}
	
	public static int getDelay(Properties prop){
		String filenameStr = prop.getProperty(PROPERTY_DELAY);
		return new Integer(filenameStr).intValue();
	}
	
	public static void setPublisherDelay(Properties prop){
		setDelay(prop, DEFAULT_PUBLISHER_DELAY);
	}
	
	public static void setSubscriberDelay(Properties prop){
		setDelay(prop, DEFAULT_SUBSCRIBER_DELAY);
	}
	
	public static void setDelay(Properties prop, int delay){
		prop.setProperty(PROPERTY_DELAY, "" + delay);
	}
	
	public static void setNodeType(Properties prop, NodeTypes newNodeType){
		prop.put(PROPERTY_NODE_TYPE, newNodeType.name());
	}
	
	public static boolean isPublisher(Properties prop){
		NodeTypes nodeType = getNodeType(prop);
		if(nodeType==null)
			return false;
		
		return nodeType.isPublisher();
	}
	
	public static boolean isSubscriber(Properties prop){
		NodeTypes nodeType = getNodeType(prop);
		if(nodeType==null)
			return false;
		
		return nodeType.isSubscriber();
	}
	
	public static NodeTypes getNodeType(Properties prop){
		String nodeNameStr = prop.getProperty(PROPERTY_NODE_TYPE);
		NodeTypes nodeType = NodeTypes.getNodeType(nodeNameStr);
		return nodeType;
	}
	
	public static InetSocketAddress stringToInetSocketAddress(String addressStr){
		if ( addressStr == null )
			return null;
		try {
			addressStr = addressStr.trim();
			String portStr = addressStr.split(":")[1];
			int inetPort = new Integer(portStr).intValue();
			InetAddress inetAddress = InetAddress.getByName(addressStr.split(":")[0].replace("/", ""));
			InetSocketAddress address = new InetSocketAddress(inetAddress, inetPort);
			
			return address;
			
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	@Deprecated
	public static boolean saveProperties(String filename, Set<Properties> propsSet){
		try{
			File file = new File(filename);
			if ( !file.exists() )
				file.createNewFile();
			
			OutputStream outStream = new FileOutputStream(filename);
			Iterator<Properties> propsIt = propsSet.iterator();
			while ( propsIt.hasNext() ){
				Properties props = propsIt.next();
				props.store(outStream, null);
			}
			outStream.close();

		}catch(IOException iox){
			iox.printStackTrace();
			return false;
		}
		return true;
	}
	
	public static boolean savePropertiesInDir(String dirname, Collection<Properties> props) {
		String dirnameEnding = dirname + FileUtils.separatorChar;
		
//		try{
			File dir = new File(dirnameEnding);
			if ( !dir.exists() )
				dir.mkdir();
			
			if ( !dir.isDirectory() )
				return false;
			
			for ( Iterator<Properties> propsIt = props.iterator() ; propsIt.hasNext() ; ){
				Properties prop = propsIt.next();
				String nodeName = PropertyGrabber.getNodeName(prop);
				String filename = dirnameEnding + nodeName + PropertyGrabber.PROPERTY_FILE_EXTENSION;
				PropertyGrabber.saveProperties(filename, prop);
			}
//		}catch(IOException iox){
//			return false;
//		}
		
		return true;
	}
	
	public static boolean saveProperties(String filename, Properties props){
		File file = new File(filename);
		
		try{
			if ( !file.exists() )
				file.createNewFile();
			
			OutputStream outStream = new FileOutputStream(filename);
			props.store(outStream, null);
			outStream.close();
		}catch(IOException iox){
			return false;
		}
		
		return true;
	}
	
	public static Properties[] grabProperties(String[] filenames){
		TreeMap<OverlayNodeId, Properties> sortedProperties =
			new TreeMap<OverlayNodeId, Properties>();
		
		for (int i=0 ; i<filenames.length ; i++ ) {
			Properties prop = grabProperties(filenames[i]);
			String nodeName = getNodeName(prop);
			OverlayNodeFactory nodeFactory = OverlayNodeFactory.getInstance();
			OverlayNodeId nodeId = nodeFactory.getNodeId(nodeName);
			sortedProperties.put(nodeId, prop);
		}
		
		Properties[] props = new Properties[filenames.length];
		for (int i=0 ; i<filenames.length ; i++ ) {
			Entry<OverlayNodeId, Properties> entry = sortedProperties.firstEntry();
			sortedProperties.remove(entry.getKey());
			props[i] = entry.getValue();
		}
		
		return props;
	}
	
	public static Properties grabProperties(String filename){
		try{
			Properties props = new Properties();
			InputStreamReader inStreamReader = new InputStreamReader(new FileInputStream(filename), "UTF-8");
			props.load(inStreamReader);
			return props;
		}catch(IOException iox){
//			iox.printStackTrace();
		}
		
		return null;
	}
	
	public static String[] getFileNamesRelative(String dirname, String extension){
		File dir = new File(dirname);
		if ( !dir.exists() )
			return null;
		
		if ( !dir.isDirectory() )
			return null;
		
		File[] allFiles = dir.listFiles();
		
		int count = 0;
		for ( int i=0 ; i<allFiles.length ; i++ )
			if ( !allFiles[i].isDirectory() && allFiles[i].getName().endsWith(extension) )
				count++;
		
		int index = 0;		
		String[] retListings = new String[count];
		for ( int i=0 ; i<allFiles.length ; i++ )
			if ( !allFiles[i].isDirectory() && allFiles[i].getName().endsWith(extension) )
				retListings[index++] = dirname + "/" + allFiles[i].getName();
		
		return retListings;

	}
	
	public static String[] getFileNames(String dirname, String extension){
		File dir = new File(dirname);
		if ( !dir.exists() )
			return null;
		
		if ( !dir.isDirectory() )
			return null;
		
		File[] allFiles = dir.listFiles();
		
		int count = 0;
		for ( int i=0 ; i<allFiles.length ; i++ )
			if ( !allFiles[i].isDirectory() && allFiles[i].getName().endsWith(extension) )
				count++;
		
		int index = 0;		
		String[] retListings = new String[count];
		for ( int i=0 ; i<allFiles.length ; i++ )
			if ( !allFiles[i].isDirectory() && allFiles[i].getName().endsWith(extension) )
				retListings[index++] = allFiles[i].getAbsolutePath();
		
		return retListings;
	}
	
	public static Properties duplicateProperties(Properties prop){
		Properties newProp = new Properties();
		
		Set<Entry<Object, Object>> entries = prop.entrySet();
		Iterator<Entry<Object, Object>> entriesIt = entries.iterator();
		while ( entriesIt.hasNext() ){
			Entry<Object, Object> entry = entriesIt.next();
			Object key = entry.getKey();
			Object value = entry.getValue();
			
			newProp.put(key, value);
		}
		
		return newProp;
	}
	
	public static void setTimelineEvent(Properties prop, FailureTimelineEvents event, int tick){
		String eventsStr = prop.getProperty(PROPERTY_FAILURE_PROFILE);
		int length = tick + 1;
		if ( eventsStr != null && eventsStr.length() > tick + 1 )
			length = eventsStr.length();
		
		FailureTimelineEvents[] fts = FailureTimelineEvents.getEvents(eventsStr, length);
		fts[tick] = event;
		
		String newEventsStr = FailureTimelineEvents.getString(fts);
		prop.setProperty(PROPERTY_FAILURE_PROFILE, newEventsStr);
	}
	
	public static int getIntProperty(Properties prop, String propertyName, int defaultValue) {
		if(prop == null)
			return defaultValue;
		return (prop.containsKey(propertyName) ? Integer.valueOf(prop.getProperty(propertyName)) : defaultValue);
	}

	public static float getFloatProperty(Properties prop, String propertyName, float defaultValue) {
		if(prop == null)
			return defaultValue;
		return (prop.containsKey(propertyName) ? Float.valueOf(prop.getProperty(propertyName)) : defaultValue);
	}
	

	public static String getStringProperty(Properties prop, String propertyName, String defaultValue) {
		if(prop == null)
			return defaultValue;
		return (prop.containsKey(propertyName) ? prop.getProperty(propertyName) : defaultValue);
	}
	
	public static void main(String[] argv){
		
		Properties DEFAULT_PROPERTIES = PropertyGrabber.
			grabProperties("C:\\Users\\Reza\\Code\\workspace3\\Scripts\\Deployments\\nodes100+\\template.props"); //OK, in main()
		
		String propfilename = "D:\\Temp\\sl\\compare500\\p0.properties";
		Properties props = PropertyGrabber.grabProperties(propfilename);
		String nodeName = props.getProperty(PropertyGrabber.PROPERTY_NODE_NAME);
		String nodeFailureProfile = props.getProperty(PropertyGrabber.PROPERTY_FAILURE_PROFILE);

		
		System.out.println(props); // OK
		System.out.println(">" + nodeName + "<"); // OK
		System.out.println(">" + nodeFailureProfile + "<"); // OK
//		String[] allListings = getFileNames("c:\\temp\\nodes\\", ".properties");
//		for ( int i=0 ; i<allListings.length ; i++ ){
//			String propfilename = allListings[i];
//			Properties props = grabProperties(propfilename);
//			String s1 = props.getProperty("Delay");
//			String s2 = props.getProperty("NodeType");
//			String s3 = props.getProperty("SubscriptionFilename");
//			String s4 = props.getProperty("FailureProfile");
//			String s5 = props.getProperty("NodeAddress");
//			String s6 = props.getProperty("JoinPointAddress");
//			System.out.println(propfilename);
//			System.out.println("\t" + props);
//		}
	}
	
}
