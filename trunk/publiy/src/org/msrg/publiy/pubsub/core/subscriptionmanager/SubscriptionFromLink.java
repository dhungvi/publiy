package org.msrg.publiy.pubsub.core.subscriptionmanager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.msrg.publiy.broker.BrokerIdentityManager;

import org.msrg.publiy.broker.core.nodes.OverlayNodeId;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.publishSubscribe.Subscription;

public class SubscriptionFromLink {
	
	private static int _lastSeqAssigned = 0;

	public final OverlayNodeId _srcId;
	public final OverlayNodeId _fromId;
	public final Subscription _sub;
	public final int _seqAssigned = ++_lastSeqAssigned;
	
	public SubscriptionFromLink(OverlayNodeId srcId, OverlayNodeId fromId, Subscription sub) {
		_srcId = srcId;
		_fromId = fromId;
		_sub = sub;
		
		if(_sub == null)
			throw new IllegalArgumentException();
		
		if(_fromId == null)
			throw new IllegalArgumentException();
	}
	
	@Override
	public String toString() {
		return 
			_srcId + ":" + "0" + ":" + _seqAssigned +
			"\t" +
			_fromId +
			"\t" +
			"(" + Subscription.encode(_sub) + ")";
	}
	
	static Pattern SubscriptionEntryDecodePattern =
		Pattern.compile("[\\s]*" +
						"([\\S\\d\\-]*):([\\d]*):([\\d]*)" +	/* SRC_SEQ */
						"[\\s]*" +
						"([\\S]*)" +					/* FROM */
//						".*");
						"[\\s]*" +
						"\\(([^\\(]*)\\)" +		/* SUB */
						"[\\s]*");

	public static SubscriptionEntry decode(String line, BrokerIdentityManager idManager) throws IllegalArgumentException {
		line = line.trim();
		if(line.equalsIgnoreCase(""))
			return null;
		
		Matcher matcher = SubscriptionEntryDecodePattern.matcher(line);
		if(!matcher.matches())
			throw new IllegalArgumentException(line);
		if(matcher.groupCount()!=5)
			throw new IllegalArgumentException(line);
		
		int i = 1;
		String seqSrcStr = matcher.group(i++);
		String seqEpochStr = matcher.group(i++);
		String seqOrderStr = matcher.group(i++);
		String fromStr = matcher.group(i++);
		String subStr = matcher.group(i++);
		
		OverlayNodeId srcId = idManager.getBrokerId(seqSrcStr);
		long seqEpoch = new Long(seqEpochStr);
		int seqOrder = new Integer(seqOrderStr);
		OverlayNodeId fromId = idManager.getBrokerId(fromStr);
		Subscription sub = Subscription.decode(subStr);
		
		if(idManager.isLocal(srcId))
			return new LocalSubscriptionEntry(
					new Sequence(srcId.getNodeAddress(), seqEpoch, seqOrder),
					sub,
					fromId.getNodeAddress());
		else
			return new SubscriptionEntry(
				new Sequence(srcId.getNodeAddress(), seqEpoch, seqOrder),
				sub,
				fromId.getNodeAddress(),
				true);
	}

}
