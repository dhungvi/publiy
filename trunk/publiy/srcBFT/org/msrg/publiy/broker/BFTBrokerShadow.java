package org.msrg.publiy.broker;

import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.types.TBFTMessageManipulator;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.BFTMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.overlaymanager.BFTOverlayManager;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.pubsub.core.subscriptionmanager.BFTSubscriptionManager;

import org.msrg.publiy.utils.FileUtils;
import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;
import org.msrg.publiy.utils.log.casuallogger.BFTDackLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTInvalidMessageLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTSuspicionLogger;

import org.msrg.publiy.broker.core.IBFTConnectionManager;
import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerTypes;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;
import org.msrg.publiy.broker.security.keys.KeyManager;


public class BFTBrokerShadow extends BrokerShadow implements IBFTBrokerShadow {

	protected boolean _reactToMisbehaviors = true;
	protected final KeyManager _keyman;
	protected final Properties _arguments;
	protected IBFTSuspectedRepo _suspectedRepo;
	protected IBFTIssuerProxyRepo _iProxyRepo;
	protected BFTDackLogger _bftDackLogger;
	protected BFTSuspicionLogger _bftSuspecionLogger;
	protected BFTInvalidMessageLogger _bftInvalidMessageLogger;
	protected BFTDSALogger _bftDSALogger;
	
	public BFTBrokerShadow(NodeTypes nodeType, int delta, InetSocketAddress localAddress, String basedirname, String outputdirname, String identityfilename, Properties arguments) {
		super(nodeType, delta, localAddress, Broker.getUDPListeningSocket(localAddress), basedirname == null ? Broker.BASE_DIR : basedirname, outputdirname == null ? Broker.OUTPUT_DIR : outputdirname, identityfilename);
		
		_arguments = arguments;
		String keysdir = PropertyGrabber.getStringProperty(_arguments, PropertyGrabber.PROPERTY_KEYS_DIR, null);
		_keyman = new KeyManager(keysdir, getBrokerIdentityManager());
		
		super.setBFT(true);
	}

	@Override
	public IBFTVerifiable getVerifiable() {
		throw new UnsupportedOperationException();
	}

	@Override
	public KeyManager getKeyManager() {
		return _keyman;
	}
	
	@Override
	public int getNeighborhoodRadius() {
		return 3 * getDelta() + 1;
	}
	
	@Override
	public int getMinVerificationRequied() {
		return getDelta() + 1;
	}

	public BFTBrokerShadow setBFTSuspectedRedpo(IBFTSuspectedRepo suspectedRepo) {
		if(_suspectedRepo != null)
			throw new IllegalStateException();
		
		_suspectedRepo = suspectedRepo;
		return this;
	}
	
	@Override
	public IBFTSuspectedRepo getBFTSuspectedRepo() {
		return _suspectedRepo;
	}
	
	@Override
	public BFTBrokerShadow setMP(boolean mp) {
		if(mp)
			throw new UnsupportedOperationException();
		
		return this;
	}
	
	@Override
	public BFTBrokerShadow setNCRows(int rows) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public BFTBrokerShadow setNCServePolicy(int ncServePolicy) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public BFTBrokerShadow setNCCols(int cols) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public BFTBrokerShadow setNC(boolean coding) {
		if(coding)
			throw new UnsupportedOperationException();
		
		return this;
	}
	
	@Override
	public IBFTOverlayManager createOverlayManager() {
		return new BFTOverlayManager(this);
	}

	@Override
	public IBFTSubscriptionManager createSubscriptionManager(IOverlayManager overlayManager, String dumpFileName, boolean shouldLog) {
		if(coveringEnabled())
			throw new UnsupportedOperationException();
		
		return new BFTSubscriptionManager(this, getBrokerSubDumpFileName(), true);
	}
	
	@Override
	public IBFTIssuerProxyRepo getIssuerProxyRepo() {
		return _iProxyRepo;
	}
	
	public BFTBrokerShadow setIssuerProxyRepo(IBFTIssuerProxyRepo iProxyRepo) {
		if(_iProxyRepo != null)
			throw new IllegalStateException(); 
			
		_iProxyRepo = iProxyRepo;
		return this;
	}
	
	@Override
	public BFTBrokerShadow setBFT(boolean bft) {
		if(!bft)
			throw new IllegalArgumentException();
		
		return this;
	}

	protected final List<TBFTMessageManipulator> _bftMessageManipulators = new LinkedList<TBFTMessageManipulator>();

	@Override
	public IRawPacket applyAllBFTMessageManipulators(
			IRawPacket raw, ISession session, IBFTOverlayManager overlay) {
		IRawPacket manipulatedRaw = raw;
		for(TBFTMessageManipulator bftMessageManipulator : _bftMessageManipulators) {
			manipulatedRaw = bftMessageManipulator.manipulate(raw, session, overlay);
			if(manipulatedRaw == null) {
				BrokerInternalTimer.inform(bftMessageManipulator.toString());
				return manipulatedRaw;
			}
		}
		
		return manipulatedRaw;
	}
	
	public BFTBrokerShadow setBFTMessageManipulator(TBFTMessageManipulator bftMesssageManipulator) {
		BrokerInternalTimer.inform("New BFT message manipulator: " + bftMesssageManipulator);
		_bftMessageManipulators.add(bftMesssageManipulator);
		return this;
	}
	
	@Override
	public IMessageQueue createMessageQueue(
			ConnectionManagerTypes conManType,
			IConnectionManager connectionManager,
			IOverlayManager overlayManager,
			ISubscriptionManager subscriptionManager) {
		if(isBFT())
			return new BFTMessageQueue(
					(IBFTBrokerShadow) this,
					(IBFTConnectionManager) connectionManager,
					(IBFTOverlayManager) overlayManager,
					(IBFTSubscriptionManager) subscriptionManager);
		else
			return super.createMessageQueue(
					conManType, connectionManager,
					overlayManager, subscriptionManager);
	}
	
	public void setBFTDackLogger(BFTDackLogger bftDackLogger) {
		if(_bftDackLogger != null)
			throw new IllegalStateException();
		
		_bftDackLogger = bftDackLogger;
	}
	
	@Override
	public BFTDackLogger getBFTDackLogger() {
		return _bftDackLogger;
	}

	@Override
	public String getBFTDackLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".bftdack";
	}

	@Override
	public BFTSuspicionLogger getBFTSuspecionLogger() {
		return _bftSuspecionLogger;
	}

	@Override
	public String getBFTSuspicionLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".suspect";
	}
	
	public BFTBrokerShadow setBFTSuspicionLogger(BFTSuspicionLogger bftSuspeicionLogger){
		if(_bftSuspecionLogger != null)
			throw new IllegalStateException();
		
		_bftSuspecionLogger = bftSuspeicionLogger;
		return this;
	}

	public void setBFTInvalidMessageLogger(
			BFTInvalidMessageLogger bftInvalidMessageLogger) {
		_bftInvalidMessageLogger = bftInvalidMessageLogger;
	}
	
	public String getBFTInvalidMessageLogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".invalid";
	}

	@Override
	public BFTInvalidMessageLogger getBFTIInvalidMessageLogger() {
		return _bftInvalidMessageLogger;
	}

	@Override
	public String getBFTDSALogFilename() {
		return getOutputDir() + FileUtils.separatorChar + getBrokerID() + ".dsa";
	}

	@Override
	public BFTDSALogger getBFTDSALogger() {
		return _bftDSALogger;
	}
	
	public BFTBrokerShadow setBFTDSALogger(BFTDSALogger bftDSALogger) {
		if(_bftDSALogger != null)
			throw new IllegalStateException();
		
		_bftDSALogger = bftDSALogger;
		return this;
	}

	@Override
	public boolean reactToMisbehaviors() {
		return _reactToMisbehaviors;
	}
	
	public void setReactToMisbehaviors(boolean reactToMisbehaviors) {
		_reactToMisbehaviors = reactToMisbehaviors;
	}
}
