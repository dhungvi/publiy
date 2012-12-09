package org.msrg.publiy.broker.core.contentManager;

import java.util.List;

public interface IContentLifecycle {

	public long getCreationTime();
	public long getDecodeStartTime();
	public long getDecodeCompleteTime();
	public int getDecodeAttempts();
	
	public List<ContentLifecycleEvent> getMetadataArrivedTimes();
	public List<ContentLifecycleEvent> getMetadataSentTimes();
	public List<ContentLifecycleEvent> getPiecesArrivalTimes();
	public List<ContentLifecycleEvent> getPiecesSentTimes();
}
