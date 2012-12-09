package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.concurrent.locks.Lock;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;

import org.msrg.publiy.utils.MyReentrantLock;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.communication.core.niobinding.keepalive.FDTimer;

public abstract class NIOBinding extends Thread implements INIOBinding, ILoggerSource {
	protected Selector _selector;
	protected InetSocketAddress _ccSockAddress;
	protected Lock _selectorLock = new MyReentrantLock(true);
	protected FDTimer _fdTimer;
	protected final IBrokerShadow _brokerShadow;

	/*
	 * If we cannot Selector.open(), then we cannot really do anything -> throw exception!
	 */
	NIOBinding(IBrokerShadow brokerShadow, InetSocketAddress ccSockAddress) throws IOException{
		super("THREAD-" + "NIOBinding");
		setPriority(Broker.getNIOThreadPriority());
		_ccSockAddress = ccSockAddress;
		_selector = Selector.open();
		_fdTimer = new FDTimer(this);
		_brokerShadow = brokerShadow;
	}
	
	abstract void updateAcceptingCount(int i);
	abstract void updateReaderCount(int i);
	abstract void updateConnectingCount(int i);
	abstract void updateWriterCount(int i);
	
	void interestedInReads(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_READ);
		}
		_selectorLock.unlock();
	}
	
	void uninterestedInReads(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() & ~SelectionKey.OP_READ);
		}
		_selectorLock.unlock();
	}
	
	void interestedInWrites(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
		}
		_selectorLock.unlock();
	}
	
	void uninterestedInWrites(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
		}
		_selectorLock.unlock();
	}
	
	void interestedInConnects(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_CONNECT);
		}
		_selectorLock.unlock();
	}
	
	void uninterestedInConnects(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT);
		}
		_selectorLock.unlock();
	}
	
	void interestedInAccepts(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() | SelectionKey.OP_ACCEPT);
		}
		_selectorLock.unlock();
	}
	
	void uninterestedInAccepts(SelectionKey key) {
		if(key == null)
			return;
		
		_selectorLock.lock();
		synchronized(key)
		{
			key.interestOps(key.interestOps() & ~SelectionKey.OP_ACCEPT);
		}
		_selectorLock.unlock();
	}
	
	@Override
	public String toString() {
		return "NIOBinding " + _ccSockAddress;
	}
	
	@Override
	public FDTimer getFDTimer() {
		return _fdTimer;
	}
	
	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_NIO;
	}

	public abstract InetSocketAddress getLocalAddress();

	public abstract LocalSequencer getLocalSequencer();

	@Override
	public IBrokerShadow getBrokerShadow() {
		return _brokerShadow;
	}
}
