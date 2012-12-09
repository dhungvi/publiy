package org.msrg.publiy.client.multipath.subscriber;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.msrg.publiy.broker.PubForwardingStrategy;

import org.msrg.publiy.pubsub.core.packets.multipath.multicast.TMulticast_Publish_MP;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

class TimeDeliveryInfo{
	final long _gTime;
	final long _dTime;
	final int _pathLength;
	
	TimeDeliveryInfo(long gTime, long dTime, int pathLength){
		_gTime = gTime;
		_dTime = dTime;
		_pathLength = pathLength;
	}
	
	@Override
	public String toString(){
		return _gTime + " " + _dTime + " " + _pathLength;
	}
	
	public static String nullString(){
		return "XXX" + " " + "XXX" + " " + "XXX";
	}
}

class DeliveryInfo{
	
	private final TimeDeliveryInfo[] _timeDeliveryInfos = 
		new TimeDeliveryInfo[TMulticast_Publish_MP.getBundleSize()];
	private final Publication_MP_ID _pubID;
	
	DeliveryInfo(Publication_MP_ID pubID){
		if ( pubID == null )
			throw new NullPointerException();
		
		_pubID = pubID;
	}
	
	@Override
	public boolean equals(Object o){
		if ( Publication_MP_ID.class.isInstance(o) ){
			return ((Publication_MP_ID)o).equals(_pubID);
		}
		else if ( DeliveryInfo.class.isInstance(o) ){
			return ((DeliveryInfo)o)._pubID.equals(this._pubID);
		}
		else
			throw new UnsupportedOperationException(o  + " is not supported");
	}
	
	@Override
	public int hashCode(){
		return _pubID.hashCode();
	}
	
	boolean addDeliveredPublication(TMulticast_Publish_MP tmp_mp){
		Publication_MP_ID pubId = new Publication_MP_ID(tmp_mp);
		if ( !this.equals(pubId) )
			throw new IllegalArgumentException(tmp_mp + " is not a match for me: " + this);
		
		PubForwardingStrategy forwardingStrategy = tmp_mp.getPubForwardingStrategy();
		int index = forwardingStrategy.getByteCode();
		TimeDeliveryInfo existingDeliveryInfo = _timeDeliveryInfos[index-1];
		if ( existingDeliveryInfo != null ){
			System.out.println("DUPPP: " + pubId + " " + tmp_mp._pathLength);
			return false;
		}
		
		TimeDeliveryInfo tDeliveryInfo = new TimeDeliveryInfo(tmp_mp._gTime, tmp_mp._dTime, tmp_mp._pathLength);
		_timeDeliveryInfos[index-1] = tDeliveryInfo;
		
		for ( int i=0 ; i<_timeDeliveryInfos.length ; i++ )
			if ( _timeDeliveryInfos[i] == null )
				return false;
		
		return true;
	}
	
	@Override
	public String toString(){
		String str = "DEL_INFO " + _pubID + " ";
		for ( int i=0 ; i<_timeDeliveryInfos.length ; i++ )
			str += "< " +  PubForwardingStrategy.values()[i+1] + " " + ((_timeDeliveryInfos[i]==null)?(TimeDeliveryInfo.nullString()):_timeDeliveryInfos[i].toString()) + " >";
		
		return str;
	}
}

public class BundledPublication_MPDelivery implements ILoggerSource {

	final Map<Publication_MP_ID, DeliveryInfo> _deliveredSet =
		new HashMap<Publication_MP_ID, DeliveryInfo>();
	final InetSocketAddress _localAddress;
	
	BundledPublication_MPDelivery(InetSocketAddress localAddress){
		_localAddress = localAddress;
	}
	
	void publicationDelivered(TMulticast_Publish_MP tmp_mp){
		synchronized(_deliveredSet){
			Publication_MP_ID pubID = new Publication_MP_ID(tmp_mp);
			DeliveryInfo existingDeliveryInfo = _deliveredSet.get(pubID);
			if ( existingDeliveryInfo==null ){
				existingDeliveryInfo = new DeliveryInfo(pubID);
				_deliveredSet.put(pubID, existingDeliveryInfo);
			}
			
			boolean deliveryInfoComplete =
				existingDeliveryInfo.addDeliveredPublication(tmp_mp);
			if ( deliveryInfoComplete ){
				_deliveredSet.remove(pubID);
				writeDown(existingDeliveryInfo);
			}
		}
	}

	void flushAllRemaining(){
		synchronized(_deliveredSet){
			LoggerFactory.getLogger().info(
					this, "Flushing remaining delivered publications (" + _deliveredSet.size() + ") .. ");
			Iterator<Publication_MP_ID> deliveredSetIt =
				_deliveredSet.keySet().iterator();
			while ( deliveredSetIt.hasNext() ){
				DeliveryInfo delInfo = _deliveredSet.get(deliveredSetIt.next());
				writeDown(delInfo);
			}
			
			_deliveredSet.clear();
		}
	}
	
	private void writeDown(DeliveryInfo deliveryInfo){
		LoggerFactory.getLogger().info(this, deliveryInfo.toString());
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_PUB_MP_DELIVERY_INFO;
	}
	
}
