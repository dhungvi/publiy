package org.msrg.publiy.broker.core.flowManager;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.log.casuallogger.coding.CasualContentLogger;

import org.msrg.publiy.broker.IBrokerShadow;
import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.sequence.Sequence;

enum RequestedPiecesToBeSent {
	FULL_STOP,
	STOP,
	SEND,
}

public class Flow implements IFlow {
	
	public FlowPriority _flowPriority;
	public final Content _content;
	public final IAllFlows _allFlows;
	protected FlowState _flowState;
	protected int _piecesSent = 0;
	protected int _piecesToSendBeforeBreak;
	protected RequestedPiecesToBeSent _requestedPiecesToSendBeforeBreak;
	protected final CasualContentLogger contentLogger;;
	
	Flow(Content content, IAllFlows allFlows) {
		_content = content;
		_allFlows = allFlows;
		_flowState = FlowState.ACTIVE;
		_requestedPiecesToSendBeforeBreak = RequestedPiecesToBeSent.SEND;
		setPiecesToSendBeforeBreak((int)(content.getRows() * 1.01));
		InetSocketAddress remote = getRemoteAddress();
		Sequence contentSequence = getContentSequence();
		IBrokerShadow brokerShadow = allFlows.getBrokerShadow();
		contentLogger = brokerShadow.getContentLogger();
		if(contentLogger != null)
			contentLogger.contentFlowAdded(contentSequence, remote);
	}
	
	protected void setFlowPriority(FlowPriority flowPriority) {
		_flowPriority = flowPriority;
	}
	
	@Override
	public Content getContent() {
		return _content;
	}
	
	@Override
	public Sequence getContentSequence() {
		return _content.getSourceSequence();
	}

	@Override
	public InetSocketAddress getRemoteAddress() {
		return _allFlows.getRemote();
	}

	@Override
	public FlowState getFlowState() {
		return _flowState;
	}

	@Override
	public boolean isActive() {
		return _flowState == FlowState.ACTIVE;
	}

	@Override
	public void deactivate() {
		if(_flowState == FlowState.INACTIVE)
			return;
		
		_flowState = FlowState.INACTIVE;
		InetSocketAddress remote = getRemoteAddress();
		Sequence contentSequence = getContentSequence();
		if(contentLogger != null)
			contentLogger.contentFlowRemoved(contentSequence, remote);
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!getClass().isAssignableFrom(obj.getClass()))
			return false;
		
		Flow flowObj = (Flow) obj;
		return _content == flowObj._content && 
				flowObj.getRemoteAddress().equals(getRemoteAddress());
	}
	
	@Override
	public FlowPriority getFlowPriority() {
		return _flowPriority;
	}
	
	@Override
	public int getPiecesSent() {
		return _piecesSent;
	}

	@Override
	public boolean canSendLimitless() {
		return _piecesToSendBeforeBreak > 3;
	}
	
	@Override
	public boolean checkRequestedPiecesToBeSent() {
		switch (_requestedPiecesToSendBeforeBreak) {
		case FULL_STOP:
		case STOP:
			return false;

		case SEND:
			if(_piecesToSendBeforeBreak < 0)
				throw new IllegalStateException(_piecesToSendBeforeBreak + " vs. " + _requestedPiecesToSendBeforeBreak);
			
			return true;
			
		default:
			throw new UnsupportedOperationException(
					"Unknown requeste: " + _requestedPiecesToSendBeforeBreak);
		}
	}

	@Override
	public void deferredSend() {
		_piecesSent--;
	}
	
	@Override
	public boolean canEncodeAndSend(boolean change) {
		if(_piecesToSendBeforeBreak <= 0)
			return false;
		
		int mainSeedPieces = _content.getMainSeedCodedPieceCount();
		
		if(mainSeedPieces == -1 || mainSeedPieces > _piecesSent) {
//			if(_piecesToSendBeforeBreak == -1) {
//				_piecesSent++;
//				return true;
//			}
			
			if(_piecesToSendBeforeBreak > 0) {
				if(change) {
					setPiecesToSendBeforeBreak(_piecesToSendBeforeBreak-1);
					_piecesSent++;
				}
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public void setPiecesToSendBeforeBreak(int piecesToSendBeforeBreak) {
//		if(!isActive())
//			throw new IllegalStateException();
		
		switch(_requestedPiecesToSendBeforeBreak) {
		case FULL_STOP:
			if(piecesToSendBeforeBreak != -1)
				throw new IllegalStateException("" + piecesToSendBeforeBreak);
			break;
			
		case STOP:
			if(piecesToSendBeforeBreak > 0)
				_requestedPiecesToSendBeforeBreak = RequestedPiecesToBeSent.SEND;
			break;
			
		default:
			break;
		}
		
		_piecesToSendBeforeBreak = piecesToSendBeforeBreak;
	}
	
	public void fullstop() {
		_requestedPiecesToSendBeforeBreak = RequestedPiecesToBeSent.FULL_STOP;
	}
	
	public void stop() {
		switch (_requestedPiecesToSendBeforeBreak) {
		case FULL_STOP:
			throw new IllegalStateException();

		case STOP:
			return;
			
		case SEND:
			_requestedPiecesToSendBeforeBreak = RequestedPiecesToBeSent.STOP;
			return;
			
		default:
			throw new UnsupportedOperationException("" + _requestedPiecesToSendBeforeBreak);
		} 
	}
	
	@Override
	public String toString() {
		return "Flow[" + _flowPriority + "." + _flowState + "_" + getContentSequence().toStringShort() + "_" + _allFlows.getLocalAddress().getPort() + "->" + _allFlows.getRemote().getPort() + "]";
	}
	
//	@Override
//	public void incrementPiecesSent() {
//		_piecesSent++;
//	}

	@Override
	public FlowPriority degradeFlowPriority() {
		if(!isActive())
			return null;
		
		switch(_flowPriority) {
		case ULTIMATE:
			_allFlows.changeFlowPriorityPrivately(this, FlowPriority.HIGH);
			break;

		case HIGH:
			_allFlows.changeFlowPriorityPrivately(this, FlowPriority.MEDIUM);
			break;
			
		case MEDIUM:
			_allFlows.changeFlowPriorityPrivately(this, FlowPriority.LOW);
			break;
			
		case LOW:
			break;
		}
		
		return _flowPriority;
	}

	@Override
	public FlowPriority upgradeFlowPriority(FlowPriority flowPriority) {
		_allFlows.changeFlowPriorityPrivately(this, flowPriority);
		
		return _flowPriority;
	}
}
