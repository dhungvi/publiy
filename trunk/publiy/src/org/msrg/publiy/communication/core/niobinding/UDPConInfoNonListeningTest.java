package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.communication.core.listener.INIO_A_Listener;
import org.msrg.publiy.communication.core.listener.INIO_R_Listener;
import org.msrg.publiy.communication.core.listener.INIO_W_Listener;
import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.TText;
import org.msrg.publiy.communication.core.sessions.ISession;
import org.msrg.publiy.component.ComponentStatus;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;
import org.msrg.publiy.node.NodeTypes;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

import junit.framework.TestCase;

public class UDPConInfoNonListeningTest extends TestCase
		implements INIO_A_Listener, INIO_R_Listener, INIO_W_Listener, IComponentListener {
	
	protected final int TEST_MAX_DURATION_SECS = 900;
	protected final Timer _timer = new Timer("NIOBindingImpTestTimer");

	protected INIOBinding _nioBinding;
	protected UDPConInfoNonListening _nlUdpConnection;
	protected IConInfoListening<DatagramChannel> _lUdpConnection;
	long _sendInterval = 0;
	long _resendInterval = 5;
	protected ComponentStatus _nioState = ComponentStatus.COMPONENT_STATUS_UNINITIALIZED;

	protected final InetSocketAddress _localAddress = new InetSocketAddress("127.0.0.1", 2001);
	protected final InetSocketAddress 	_udpServerAddress = new InetSocketAddress("127.0.0.1", 2001);
	protected final IBrokerShadow _brokerShadow = new BrokerShadow(NodeTypes.NODE_BROKER, _localAddress);
	
	protected final int _packetsToSend = 100000;
	protected final int _contentSize = 3000;
	protected final String _textToSend = createTextToSend(_contentSize, (char) 'r');
	
	protected static String createTextToSend(int size, char content) {
		char[] bytes = new char[size];
		for(int i=0 ; i<size ; i++)
			bytes[i] = content;
		
		return String.valueOf(bytes);
	}

	protected int _receivedPacketsCount = 0;
	protected Set<IPacketable> _receivedPackets = null; // new HashSet<IPacketable>();
	protected Object _receivedLock = new Object();
	
	@Override
	public void setUp() throws IOException {
		BrokerInternalTimer.start();
		
		_nioBinding = new NIOBindingImpUDP_ForTest(_brokerShadow);
		_nioBinding.addNewComponentListener(this);
		_nioBinding.prepareToStart();
		_nioBinding.startComponent();
		waitForNioBindingToReachState(ComponentStatus.COMPONENT_STATUS_RUNNING);
	}
	
	@Override
	public void tearDown() throws InterruptedException{
		waitForAllPacketsToCome(TEST_MAX_DURATION_SECS);
		_nioBinding.stopComponent();
		waitForNioBindingToReachState(ComponentStatus.COMPONENT_STATUS_STOPPED);
	}

	protected boolean waitForNioBindingToReachState(ComponentStatus state) {
		int WAIT_SEC = 10;
		for(int i=0 ; i<WAIT_SEC * 10 ; i++) {
			if(_nioState == state)
				return true;
			else
				try {
					Thread.sleep(100);
				} catch (InterruptedException itx) {
					return false;
			}
		}
		
		return false;
	}
	
	protected void waitForAllPacketsToCome(int waitSec) {
		int j=0;
		for(int i=0 ; i<waitSec * 10 ; i++) {
			boolean allReceived = _packetsToSend == _receivedPacketsCount;
			
			if(allReceived) {
				BrokerInternalTimer.inform("All received...\t->>>\t" + _packetsToSend + "," + _receivedPacketsCount);
				return;
			} else {
				try {
					Thread.sleep(100);
					if(j++ % 10 == 0)
						BrokerInternalTimer.inform("@" + BrokerInternalTimer.read() + "\t->>>\t" + _packetsToSend + "," + _receivedPacketsCount);

				} catch (InterruptedException itx) {
					itx.printStackTrace();
					fail("Thread interrupted!");
				}
			}
		}
		
		fail("Not all messages received within " + waitSec + " secs.");
	}

	public void testUDPConnection() {
		_lUdpConnection =
			_nioBinding.makeIncomingDatagramConnection(this, this, this, _udpServerAddress);
		
		ISession session = ISession.createDummySession(_nioBinding.getBrokerShadow());
		_nlUdpConnection =
			_nioBinding.makeOutgoingDatagramConnection(session, this, this, this, _udpServerAddress);
		
		for (int i=0 ; i<_packetsToSend ; i++) {
			UDPSendTask sendTask = new UDPSendTask(this, i);
			_timer.schedule(sendTask, _sendInterval * i);
		}
	}
		
	@Override
	public void componentStateChanged(IComponent component) {
		if(component == _nioBinding) {
			ComponentStatus state = component.getComponentState();
			BrokerInternalTimer.inform("NIOBinding has state: " + state);
			_nioState = state;
		}
	}		

	@Override
	public void becomeMyAcceptingListener(IConInfoListening<?> conInfo) {
		return;
	}

	@Override
	public void newIncomingConnection(IConInfoListening<?> conInfoL,
			IConInfoNonListening<?> newConInfoNL) {
		BrokerInternalTimer.inform("New incoming connection: " + newConInfoNL + ", on " + conInfoL);
	}

	@Override
	public void becomeMyListener(IConInfo<?> conInfo) {
		BrokerInternalTimer.inform("I am a listener for: " + conInfo);
	}

	@Override
	public void conInfoUpdated(IConInfo<?> conInfo) {
		BrokerInternalTimer.inform("Status updated: " + conInfo);
	}

	@Override
	public void becomeMyReaderListener(IConInfoNonListening<?> conInfo) {
		return;
	}

	@Override
	public void conInfoGotFirstDataItem(IConInfoNonListening<?> conInfo) {
		while(true) {
			IRawPacket raw = conInfo.getNextIncomingData();
			if(raw == null)
				return;
			IPacketable packet = PacketFactory.unwrapObject(_brokerShadow, raw);
			
			synchronized(_receivedLock) {
				if(_receivedPackets != null)
					_receivedPackets.add(packet);
				_receivedPacketsCount++;
			}
			
			TText receivedText = (TText) packet;
			assertTrue(_textToSend.equals(receivedText.getString()));
		}
	}

	@Override
	public void becomeMyWriteListener(IConInfoNonListening<?> conInfo) {
		return;
	}

	@Override
	public void conInfoGotEmptySpace(IConInfoNonListening<?> conInfo) {
		return;
	}

}

class UDPSendTask extends TimerTask {

	final UDPConInfoNonListeningTest _tester;
	final int _i;
	
	UDPSendTask(UDPConInfoNonListeningTest tester, int i) {
		_tester = tester;
		_i = i;
	}
	
	@Override
	public void run() {
		TText tText = new TText(_tester._textToSend);
		IRawPacket raw = PacketFactory.wrapObject(_tester._nioBinding.getBrokerShadow().getLocalSequencer(), tText);

		boolean packetWillSend = _tester._nlUdpConnection.sendPacket(_tester._nioBinding, raw);
		if(!packetWillSend)
			_tester._timer.schedule(new UDPSendTask(_tester, _i), _tester._resendInterval);
	}
}

class NIOBindingImpUDP_ForTest extends NIOBindingImp_SelectorBugWorkaround {

	protected NIOBindingImpUDP_ForTest(IBrokerShadow brokerShadow) throws IOException {
		super(brokerShadow);
	}
}