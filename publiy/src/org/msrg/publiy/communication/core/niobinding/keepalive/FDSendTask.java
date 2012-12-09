package org.msrg.publiy.communication.core.niobinding.keepalive;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.timer.BrokerTimerTask;
import org.msrg.publiy.utils.timer.BrokerTimerTaskType;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.THeartBeat;

public class FDSendTask extends BrokerTimerTask implements ILoggerSource {

	protected final IConInfoNLKeepAlive _conInfoNLKA;
	protected final LocalSequencer _localSequencer;
	
	public FDSendTask(LocalSequencer localSequencer, IConInfoNLKeepAlive conInfoNLKA){
		super(BrokerTimerTaskType.BTimerTask_FDSend);
		_conInfoNLKA = conInfoNLKA;
		_localSequencer = localSequencer;
		super.logCreation();
	}
	
	@Override
	public void run() {
		super.run();
		try{
			if (!_conInfoNLKA.isConnected())
				return;
			
			// So the connection is connected, we send a new FDPacketableObject and reschedule a new send
			
			THeartBeat hb = new THeartBeat(_conInfoNLKA.getRemoteAddress());
			IRawPacket rawHB = PacketFactory.wrapObject(_localSequencer, hb);
			_conInfoNLKA.getNIOBinding().send(rawHB, _conInfoNLKA);
			
			_conInfoNLKA.updateNextSendTime();
		}catch(Exception ex){
			LoggerFactory.getLogger().infoX(this, ex, "Exception in '" + this + "'");
		}
	}
	
	@Override
	public String toStringDetails() {
		return "" + _conInfoNLKA;
	}
}
