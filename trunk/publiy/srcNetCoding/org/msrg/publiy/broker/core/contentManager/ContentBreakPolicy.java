package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.List;

public class ContentBreakPolicy {
	
	public final int _minReceivedSlicesForBreak;
	public final int _minRemainingSenders;
	public final boolean _fullBreakWhenEnoughReceived;
	
	public ContentBreakPolicy(
			int minReceivedSlices, int minRemainingSenders, boolean fullBreakWhenEnoughReceived) {
		_minReceivedSlicesForBreak = minReceivedSlices;
		_minRemainingSenders = minRemainingSenders;
		_fullBreakWhenEnoughReceived = fullBreakWhenEnoughReceived;
	}
	
	public List<InetSocketAddress> sendBreak(Content content, boolean contentRecentlyChanged) {
		if(content == null)
			return null;
		
		if(content.isInversed())
			return content.getSendingRemotesToAll(true);
		
		if(!contentRecentlyChanged)
			return null;
		
		int availableSlices = content.getAvailableCodedPieceCount();
		if(_fullBreakWhenEnoughReceived) {
			int requiredSlices = content.getRequiredCodedPieceCount();
			if(availableSlices >= requiredSlices)
				return content.getSendingRemotesToAll(true);
		}
		
		if(availableSlices < _minReceivedSlicesForBreak)
			return null;
		
		int minRemainingSenders = _minRemainingSenders;
		if(content.getHaveNodes().size() > 0)
			minRemainingSenders = 0;
		
		List<InetSocketAddress> ret =
			content.getSendingRemotes(minRemainingSenders, false);
//		if(ret!=null) {
//			int remainingSendingRemotes = content.getSendingRemotesSize();
//			if(remainingSendingRemotes == 0  || remainingSendingRemotes < _minRemainingSenders)
//				throw new IllegalStateException(remainingSendingRemotes + " vs. " + _minRemainingSenders);
//		}
		
		return ret;
	}
}
