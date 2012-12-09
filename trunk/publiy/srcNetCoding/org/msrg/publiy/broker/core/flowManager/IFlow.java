package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.Sequence;

public interface IFlow {
	
	public InetSocketAddress getRemoteAddress();
	public Content getContent();
	public Sequence getContentSequence();
	public FlowPriority getFlowPriority();
	
	public FlowPriority degradeFlowPriority();
	public FlowPriority upgradeFlowPriority(FlowPriority flowPriority);
	public void setPiecesToSendBeforeBreak(int piecesToSendBeforeBreak);
	public void stop();
	public void fullstop();
	public boolean canEncodeAndSend(boolean change);
	public boolean isActive();
	public void deactivate();
	public FlowState getFlowState();
	public int getPiecesSent();
	public boolean canSendLimitless();
//	public void incrementPiecesSent();
	public boolean checkRequestedPiecesToBeSent();
	public void deferredSend();
}
