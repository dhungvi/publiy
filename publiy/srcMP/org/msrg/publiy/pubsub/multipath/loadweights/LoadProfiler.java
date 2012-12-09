package org.msrg.publiy.pubsub.multipath.loadweights;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.msrg.publiy.broker.Broker;

import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.HistoryType;
import org.msrg.publiy.utils.IHistoryRepository;
import org.msrg.publiy.utils.LimittedSizeList;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;

public class LoadProfiler implements IHistoryRepository {
	
	private final int _profilerCacheSize;
	private final int _bucketRefreshRate;
	private final Map<InetSocketAddress, LimittedSizeList<ProfilerBucket>> _bucketsLists = new HashMap<InetSocketAddress, LimittedSizeList<ProfilerBucket>>();
	private final LimittedSizeList<ProfilerBucket> _loclaBucketList;
	
	public LoadProfiler(int profilerCacheSize, int bucketRefreshRate) {
		_profilerCacheSize = profilerCacheSize;
		_bucketRefreshRate = bucketRefreshRate;
		_loclaBucketList = new LimittedSizeList<ProfilerBucket>(new ProfilerBucket[_profilerCacheSize]);
	}
	
	private final ElementTotal<LimittedSizeList<ProfilerBucket>> getTotal_Privately(Map<InetSocketAddress, LimittedSizeList<ProfilerBucket>> map, int index, InetSocketAddress remote, boolean butlast){
		synchronized(map){
			LimittedSizeList<ProfilerBucket> bucketList = (remote==null?_loclaBucketList:map.get(remote));
			if ( bucketList == null )
				return null;
			
			getValidBucket(bucketList);
			if (butlast)
				return bucketList.getElementsTotalButLast(index);
			else
				return bucketList.getElementsTotal(index);
		}
	}

	@Override
	public ElementTotal<LimittedSizeList<ProfilerBucket>> getHistoryOfType(HistoryType hType, InetSocketAddress remote){
		switch(hType) {
		case HIST_T_PUB_IN_BUTLAST:
		case HIST_T_PUB_OUT_BUTLAST:
			return getTotal_Privately(_bucketsLists,ProfilerBucketIndex.PUBLICATIONS._index, remote, true);
			
		case HIST_T_SUB_IN_BUTLAST:
		case HIST_T_SUB_OUT_BUTLAST:
			throw new UnsupportedOperationException();
//			return getTotal_Privately(_bucketsLists, ProfilerBucketIndex.SUBSCRIPTIONS._index, remote, true);
			
		case HIST_T_PUB_IN_WITHLAST:
		case HIST_T_PUB_OUT_WITHLAST:
			return getTotal_Privately(_bucketsLists, ProfilerBucketIndex.PUBLICATIONS._index, remote, false);
			
		case HIST_T_SUB_IN_WITHLAST:
		case HIST_T_SUB_OUT_WITHLAST:
			throw new UnsupportedOperationException();
//			return getTotal_Privately(_bucketsLists, ProfilerBucketIndex.SUBSCRIPTIONS._index, remote, false);
			
		default:
			throw new UnsupportedOperationException("Unknown history type: " + hType);
		}
	}
	
	protected ProfilerBucket createNewBucket() {
		return new ProfilerBucket(ProfilerBucketIndex.values().length);
	}
	
	protected ProfilerBucket getValidBucket(LimittedSizeList<ProfilerBucket> bucketList) {
		ProfilerBucket bucket = bucketList.getLastElement();
		if(bucket == null) {
			bucket = createNewBucket();
			bucketList.append(bucket);
		}
		else {
			int refreshedBucketsCount =
					(int)(SystemTime.currentTimeMillis()/_bucketRefreshRate)
					-
					(int)(bucket.getCreationTime()/_bucketRefreshRate);
			for (int i=0 ; i<refreshedBucketsCount && i<bucketList.getSize() ; i++) {
				bucket.invalidate();
				bucket = createNewBucket();
				bucketList.append(bucket);
			}
		}
		
		return bucket;
	}

	@Override
	public void logMessage(HistoryType hType, InetSocketAddress remote, int bytes){
		int traffic = 0;
		switch(Broker.BW_METRIC) {
		case BW_METRIC_IN_BYTES:
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_OUT_BYTES:
			traffic = bytes;
			break;
		
		case BW_METRIC_IN_MSG_COUNT:
		case BW_METRIC_INOUT_MSG_COUNT:
		case BW_METRIC_OUT_MSG_COUNT:
			traffic = 1;
			break;
			
		default:
			break;
		}
		
		synchronized(_bucketsLists){
			if (remote!=null) {
				LimittedSizeList<ProfilerBucket> bucketList = _bucketsLists.get(remote);
				if ( bucketList == null ){
					bucketList = new LimittedSizeList<ProfilerBucket>(new ProfilerBucket[_profilerCacheSize]);
					_bucketsLists.put(remote, bucketList);
				}
				
				logMessagePrivately(hType, bucketList, traffic);
			}
			
			logMessagePrivately(hType, _loclaBucketList, traffic);
		}
	}
	
	public void logMessagePrivately(HistoryType hType, LimittedSizeList<ProfilerBucket> bucketList, int bytes){
		ProfilerBucket validBucket = getValidBucket(bucketList);
		switch(hType){
		case HIST_T_PUB_IN_BUTLAST:
		case HIST_T_PUB_IN_WITHLAST:
		case HIST_T_PUB_OUT_BUTLAST:
		case HIST_T_PUB_OUT_WITHLAST:
			validBucket.addToBucket(ProfilerBucketIndex.PUBLICATIONS._index, bytes);
			break;
			
		case HIST_T_JOIN_IN_BUTLAST:
		case HIST_T_JOIN_IN_WITHLAST:
		case HIST_T_JOIN_OUT_BUTLAST:
		case HIST_T_JOIN_OUT_WITHLAST:
			throw new UnsupportedOperationException();
//			validBucket.addToBucket(ProfilerBucketIndex.JOINS._index, bytes);
//			break;
				
		case HIST_T_SUB_IN_BUTLAST:
		case HIST_T_SUB_IN_WITHLAST:
		case HIST_T_SUB_OUT_BUTLAST:
		case HIST_T_SUB_OUT_WITHLAST:
			throw new UnsupportedOperationException();
//			validBucket.addToBucket(ProfilerBucketIndex.SUBSCRIPTIONS._index, bytes);
//			break;
				
		case HIST_T_CONFS_IN_BUTLAST:
		case HIST_T_CONFS_IN_WITHLAST:
		case HIST_T_CONFS_OUT_BUTLAST:
		case HIST_T_CONFS_OUT_WITHLAST:
			throw new UnsupportedOperationException();
//			validBucket.addToBucket(ProfilerBucketIndex.CONFS._index, bytes);
//			break;
		}
	}
	
	public void writeBucketLists(Writer ioWriter, HistoryType hType) throws IOException {
		switch(hType) {
		case HIST_T_PUB_IN_BUTLAST:
		case HIST_T_PUB_OUT_BUTLAST:
			writeBucketLists(ioWriter, _bucketsLists);
			return;
			
		default:
			throw new UnsupportedOperationException("Unsupported history type: " + hType);
		}
	}
	
	protected void writeBucketLists(Writer ioWriter, Map<InetSocketAddress, LimittedSizeList<ProfilerBucket>> bucketMaps) throws IOException {
		synchronized(bucketMaps){
			ioWriter.write("ALL=" + _loclaBucketList.toStringShort());
			
			Iterator<Map.Entry<InetSocketAddress, LimittedSizeList<ProfilerBucket>>> it = bucketMaps.entrySet().iterator();
			while ( it.hasNext() ){
				Map.Entry<InetSocketAddress, LimittedSizeList<ProfilerBucket>> entry = it.next();
				InetSocketAddress remote = entry.getKey();
				LimittedSizeList<ProfilerBucket> bucketList = entry.getValue();
				getValidBucket(bucketList);
				
				ioWriter.write(" " + Writers.write(remote) + "=" + bucketList.toStringShort());
			}
		}
	}
	
	public String toString() {
		StringWriter writer = new StringWriter();
		try {
			writeBucketLists(writer, _bucketsLists);
		}catch(IOException iox) {
			return "ERROR occured: " + iox;
		}
		return writer.toString();
	}
}
