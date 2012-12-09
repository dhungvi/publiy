package org.msrg.publiy.broker;

import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;
import org.msrg.publiy.utils.log.casuallogger.BFTDackLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTInvalidMessageLogger;
import org.msrg.publiy.utils.log.casuallogger.BFTSuspicionLogger;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxyRepo;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.security.keys.KeyManager;

public interface IBFTBrokerShadow extends IBrokerShadow {

	public int getMinVerificationRequied();

	public IBFTVerifiable getVerifiable();
	public KeyManager getKeyManager();
	public IBFTSuspectedRepo getBFTSuspectedRepo();
	public IBFTIssuerProxyRepo getIssuerProxyRepo();
	
	public IRawPacket applyAllBFTMessageManipulators(IRawPacket raw, ISession session, IBFTOverlayManager overlay);
	public BFTDackLogger getBFTDackLogger();
	public String getBFTDackLogFilename();
	
	public BFTSuspicionLogger getBFTSuspecionLogger();
	public String getBFTSuspicionLogFilename();

	public String getBFTInvalidMessageLogFilename();
	public BFTInvalidMessageLogger getBFTIInvalidMessageLogger();

	public String getBFTDSALogFilename();
	public BFTDSALogger getBFTDSALogger();

	public boolean reactToMisbehaviors();
	
}
