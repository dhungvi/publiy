package org.msrg.publiy.broker.core.contentManager;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;


import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.IPSCodedBatch;
import org.msrg.publiy.networkcodes.PSCodedPiece;
import org.msrg.publiy.publishSubscribe.Publication;


public abstract class Content {
	
	protected static final int MS_INTERVAL_BETWEEN_METADATA_REQ = 1000 * 10;
	protected static byte[][] DEFAULT_CONTENT = createDefaultContent(Broker.ROWS, Broker.COLS);
	
	public static void initDefaultContent(int rows, int cols) {
		DEFAULT_CONTENT = createDefaultContent(rows, cols);
	}

	public static void initDefaultContent() {
		initDefaultContent(Broker.ROWS, Broker.COLS);
	}

	public static byte[][] createDefaultContent(int rows, int cols) {
		int val=0;
		byte[][] bArray = new byte[rows][];
		for(int i=0 ; i<bArray.length ; i++) {
			bArray[i] = new byte[cols];
			for(int j=0 ; j<bArray[i].length ; j++)
				bArray[i][j] = (byte)val++;
		}
		return bArray;
	}
	
	protected IPSCodedBatch _codeBatch;
	protected int _mainSeedReceivedCount;
	protected int _totalSentCount;
	
	protected final Set<InetSocketAddress> _needRemotes =
		new HashSet<InetSocketAddress>();
	protected final Set<InetSocketAddress> _haveRemotes =
		new HashSet<InetSocketAddress>();
	protected final Set<InetSocketAddress> _sendingRemotesSet =
		new HashSet<InetSocketAddress>();
	protected final List<InetSocketAddress> _sendingRemotesList =
		new LinkedList<InetSocketAddress>();
	protected final ContentLifecycle _contentLifecycle;
	protected final static Random _rand = new Random();
	
	protected Content(IPSCodedBatch codeBatch) {
		_codeBatch = codeBatch;
		_contentLifecycle = new ContentLifecycle(this);
		
		_haveRemotes.add(getSource());
	}
	
	public IContentLifecycle getContentLifecycle() {
		return _contentLifecycle;
	}
	
	public void addSendingNode(InetSocketAddress node) {
		if(node == null)
			return;
	
		if(_sendingRemotesSet.add(node)) {
			_sendingRemotesList.add(node);
		}
	}
	
	public boolean addHaveNode(
			InetSocketAddress haveNode) {
		if(haveNode == null)
			return false;

		_needRemotes.remove(haveNode);
		if(_haveRemotes.add(haveNode))
			BrokerInternalTimer.inform("Node " + haveNode + " has " + getSourceSequence());
		return true;
	}

	public void addNeedNodes(Collection<InetSocketAddress> needNodes) {
		if(needNodes == null)
			return;
		for(InetSocketAddress needNode : needNodes)
			addNeedNode(needNode);
	}
	
	public boolean removeNeedNode(InetSocketAddress node) {
		if(node == null)
			return false;
		
		return _needRemotes.remove(node);
	}
	
	public boolean addNeedNode(InetSocketAddress needNode) {
		if(needNode == null)
			return false;
		
		if(_haveRemotes.contains(needNode))
			return false;
		
		if(needNode.equals(getSource()))
			return false;
		
		if(_needRemotes.add(needNode))
			BrokerInternalTimer.inform("Node " + needNode + " needs " + getSourceSequence());
		
		return true;
	}
	
	public Set<InetSocketAddress> getNeedNodes() {
		return _needRemotes;
	}
	
	public Set<InetSocketAddress> getHaveNodes() {
		return _haveRemotes;
	}
	
	public InetSocketAddress getOneRandomNeddNode() {
		return getOneRandomNodePrivately(_needRemotes);
	}

	public InetSocketAddress getOneRandomHaveNode() {
		return getOneRandomNodePrivately(_haveRemotes);
	}
	

	public InetSocketAddress getOneRandomSendingNode() {
		return getOneRandomNodePrivately(_sendingRemotesSet);
	}
	
	public void getRandomSendingNodes(
			int count, Set<InetSocketAddress> resultsset) {
		getRandomNodesPrivately(
				_sendingRemotesSet, count, resultsset);
	}

	public void getRandomNeedNodes(
			int count, Set<InetSocketAddress> resultsset) {
		getRandomNodesPrivately(
				_needRemotes, count, resultsset);
	}
	
	public void getRandomHaveNodes(
			int count, Set<InetSocketAddress> resultsset) {
		getRandomNodesPrivately(
				_haveRemotes, count, resultsset);
	}
	
	protected static InetSocketAddress getOneRandomNodePrivately(
			Set<InetSocketAddress> inputSet) {
		int size = inputSet.size();
		for(InetSocketAddress remote : inputSet)
			if(_rand.nextInt(size) == 0)
				return remote;
		return null;
	}

	protected static int getRandomNodesPrivately(
			Set<InetSocketAddress> inputSet, int count,
			Set<InetSocketAddress> resultsSet) {
		if(resultsSet.size() >= count)
			return 0;
		
		int remainingCount = count - resultsSet.size();
		int insize = inputSet.size();
		if(remainingCount >= insize) {
			resultsSet.addAll(inputSet);
			return resultsSet.size() + remainingCount - count;
		}

		InetSocketAddress[] inNodesArray =
			inputSet.toArray(new InetSocketAddress[0]);
		for(int i=0 ; i<remainingCount ; i++) {
			int rand = _rand.nextInt(insize);
			resultsSet.add(inNodesArray[rand]);
		}
		
		return resultsSet.size() + remainingCount - count;
	}

	public void addCodedPiece(
			InetSocketAddress senderRemote, PSCodedPiece psCodedPiece) {
		_contentLifecycle.newPieceArrived(senderRemote);
		addSendingNode(senderRemote);
		
		if(_codeBatch.isInversed())
			return;
		
		_codeBatch.addCodedSlice(psCodedPiece);
		if(psCodedPiece._fromMainSeed)
			_mainSeedReceivedCount++;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(getClass().isAssignableFrom(obj.getClass())) {
			Content contentObj = (Content) obj;
			return getSourceSequence().equalsExact(contentObj.getSourceSequence());
		} 
		
		if(Sequence.class.isAssignableFrom(obj.getClass())) {
			return ((Sequence)obj).equalsExact(getSourceSequence());
		}
		
		return false;
	}
	
	@Override
	public int hashCode() {
		return _codeBatch.getSourceSequence().hashCode();
	}
	
	public Sequence getSourceSequence() {
		return _codeBatch.getSourceSequence();
	}
	
	public Publication getPublication() {
		return _codeBatch.getPublication();		
	}
	
	public boolean loadPublicationForFirstTime(InetSocketAddress senderRemote, Publication publication) {
		if(publication != null)
			_contentLifecycle.metadataArrived(senderRemote);
			
		return _codeBatch.loadPublicationForFirstTime(publication);
	}

	public int getSize() {
		return _codeBatch.getSize();
	}
	
	public int getCols() {
		return _codeBatch.getCols();
	}

	public int getRows() {
		return _codeBatch.getRows();
	}

	public void verifyAll(
			Sequence sequence, Publication publication, int rows, int cols) {
		verify(sequence);
		verify(rows, cols);
		verify(publication);
	}

	public void verify(Publication publication) {
		if(publication != null)
			if(!publication.equals(this.getPublication()))
				throw new IllegalStateException();
	}

	public void verify(Sequence sequence) {
		if(!this.getSourceSequence().equalsExact(sequence))
			throw new IllegalStateException();		
	}
	
	public void verify(int rows, int cols) {
		if(this.getSize() != rows * cols)
			throw new IllegalStateException();

		if(this.getRows() != rows)
			throw new IllegalStateException();

		if(this.getCols() != cols)
			throw new IllegalStateException();
	}
	
	public final boolean isInversed() {
		return _codeBatch.isInversed();
	}
	
	public boolean isSolved() {
		return _codeBatch.isSolved();
	}

	public boolean hasMetadata() {
		return getPublication() != null;
	}

	public int getRequiredCodedPieceCount() {
		return _codeBatch.getRequiredCodedPieceCount();
	}
	
	public int getAvailableCodedPieceCount() {
		return _codeBatch.getAvailableCodedPieceCount();
	}
	
	public int getMainSeedCodedPieceCount() {
		if(_codeBatch.isSolved())
			return -1;
		else
			return _mainSeedReceivedCount;	
	}
	
	public InetSocketAddress getRemoteToRequestMetadataFrom() {
//		if(!_contentLifecycle.requestMetaData(MS_INTERVAL_BETWEEN_METADATA_REQ))
//			return null;
		
		if(_sendingRemotesList.isEmpty())
			return null;
		
		InetSocketAddress sendingRemote = _sendingRemotesList.remove(0);
		if(!_sendingRemotesSet.contains(sendingRemote))
			throw new IllegalStateException(
					"Remote not presnet: " + sendingRemote);
		
		_sendingRemotesList.add(sendingRemote);
		return sendingRemote;
	}
	
	public List<InetSocketAddress> getSendingRemotesToAll(boolean remove) {
		return getSendingRemotes(1.0, remove);
	}
	
	public List<InetSocketAddress> getSendingRemotes(double removeFromSendingRemotes, boolean remove) {
		int removeCount = (int) (removeFromSendingRemotes * _sendingRemotesList.size());
		return getSendingRemotesPrivately(removeCount, remove);
	}
	
	public List<InetSocketAddress> getSendingRemotes(int remainingRemoteSenders, boolean remove) {
		int removeCount = _sendingRemotesList.size() - remainingRemoteSenders;
		List<InetSocketAddress> ret = getSendingRemotesPrivately(removeCount, remove);
		if(ret == null)
			return null;
		
		if(_sendingRemotesList.size() < remainingRemoteSenders)
			throw new IllegalStateException("ERROR: " + _sendingRemotesList + " vs. " + ret + " vs. [" + remainingRemoteSenders + "]");
		return ret;
	}
	
	protected List<InetSocketAddress> getSendingRemotesPrivately(int removeCount, boolean remove) {
		if(removeCount <= 0)
			return null;
		
		int remotesListSize = _sendingRemotesList.size();
		List<InetSocketAddress> ret = new LinkedList<InetSocketAddress>();
		for(int i=0 ; i<removeCount && i<remotesListSize; i++) {
			InetSocketAddress removedRemote = _sendingRemotesList.remove(0);
//			if(!_sendingRemotesSet.remove(removedRemote))
//				throw new IllegalStateException("Remote did not exist: " + removedRemote);
			
			if(!remove)
				_sendingRemotesList.add(removedRemote);
			ret.add(removedRemote);
		}
		return ret;
	}
	
	public boolean hasContent(InetSocketAddress remote) {
		return _haveRemotes.contains(remote);
	}
	
	public boolean decodeComplete() {
		boolean result = _contentLifecycle.setDecodeCompleteTime();
		return result;
	}
	
	public InetSocketAddress getSource() {
		return getSourceSequence().getAddress();
	}
	
	public abstract boolean canPotentiallyBeSolved();

	public int getSendingRemotesSize() {
		return _sendingRemotesList.size();
	}
	
	@Override
	public String toString() {
		return "Content: HAVE{" + _haveRemotes + "}_NEED{" + _needRemotes + "}_SENT{" + _sendingRemotesSet + "}";
	}
	
	public final IPSCodedBatch getPSCodedBatch() {
		return _codeBatch;
	}

	public abstract void loadDefaultSourcePSContent();
	
	public PSCodedPiece code() {
		PSCodedPiece psCodedPiece = (PSCodedPiece) _codeBatch.code();
		return psCodedPiece;
	}
	
	public boolean decode() {
		return _codeBatch.decode();
	}

	public boolean isSendingRemote(InetSocketAddress remote) {
		return _sendingRemotesSet.contains(remote);
	}
	
	public void pieceSent() {
		_totalSentCount++;
	}
	
	public int getPiecesSent() {
		return _totalSentCount;
	}
}
