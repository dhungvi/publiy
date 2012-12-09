/*
 * @ 12/04/2008 
 * 
 * This class implements a workaround for the known Selector bug:
 * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6403933
 * http://www.nabble.com/NIO-100--CPU-usage-td19969288.html
 * 
 * Workaround:	--	replaceAllKeysOfChannelInfos()
 * 	Once the faulty conditions are detected, i.e., _selector.select() 
 * returns 0 and for ISSUE_FILTER_COUNTER times in an ISSUE_TIMEOUT 
 * interval -- this is detected in NIOBindingImp().makeSureThisIsAnIssue(), 
 * we handleIssue() here in this class by replacing all previously  
 * registered keys with a new Selector, updating Key.attachment() and 
 * ChannelInfo.key information and discarding the broken Selector.
 */
package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.HashSet;
import java.util.Iterator;

import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.utils.log.LoggerFactory;

import org.msrg.publiy.communication.core.niobinding.ChannelInfo;
import org.msrg.publiy.communication.core.niobinding.NIOBindingImp;


public class NIOBindingImp_SelectorBugWorkaround extends NIOBindingImp {

	protected NIOBindingImp_SelectorBugWorkaround(IBrokerShadow brokerShadow) throws IOException {
		super(brokerShadow);
	}

	private void replaceListeningchannel(Selector newSelector, ChannelInfo<?> chInfo) throws IOException {
		SelectionKey key = chInfo._key;
		if(key == null)
			return;
		
		int ops = chInfo.getInterest();
		
		if ( ChannelInfoListening.class.isInstance(chInfo) )
		{
			LoggerFactory.getLogger().info(this, "Issue: Replacing (A) listening channel '" + chInfo + "' with interest (" + ops + ").");
			
			key.cancel();

			SelectableChannel listeningChannel = chInfo._channel;
//			listeningChannel.configureBlocking(false);
			SelectionKey newKey = listeningChannel.register(newSelector, ops, chInfo);
			chInfo.setKey(newKey);
		}
	
		else if ( ChannelInfoNonListening.class.isInstance(chInfo) )
		{
			LoggerFactory.getLogger().info(this, "Issue: Replacing (" + ops + ") nonlistening channel '" + chInfo + "' with interest (" + ops + ").");
			
			key.cancel();
			
			SelectableChannel nonlisteningChannel = chInfo._channel;
//			SocketChannel nonlisteningChannel = (SocketChannel.open();
//			nonlisteningChannel.configureBlocking(false);
			SelectionKey newKey = nonlisteningChannel.register(newSelector, ops, chInfo);
			chInfo.setKey(newKey);
		}
		
		else
			throw new UnsupportedOperationException("Donno how to handle this... " + chInfo);
	}
	
	private void replaceAllKeysOfChannelInfos(Selector newSelector)
	{
		Iterator<SelectionKey> keysIt = new HashSet<SelectionKey>(_selector.keys()).iterator();
		while ( keysIt.hasNext() ){
			SelectionKey key = keysIt.next();
			ChannelInfo<?> chInfo = (ChannelInfo<?>) key.attachment();
			try
			{
				replaceListeningchannel(newSelector, chInfo);
			}catch(IOException iox){
				iox.printStackTrace();
				LoggerFactory.getLogger().error(this, "Issue: Replacing listening channel failed. " + iox);
			}
		}
	}

	@Override
	protected void handleIssue(){
		super.handleIssue();
		LoggerFactory.getLogger().info(this, "Handling issue ...");
		
//		_selectorLock.lock();
//		{
//			Selector newSelector;
//			try{
//				newSelector = Selector.open();
//			}catch(IOException iox){
//				iox.printStackTrace();
//				throw new UnknownError();
//			}
//			
//			//////////////////////////////////////////
//			replaceAllKeysOfChannelInfos(newSelector);
//			resetEverything();
//			//////////////////////////////////////////
//			
//			Selector oldSelector = _selector;
//			_selector = newSelector;
//			try{
//				oldSelector.selectNow();
//				oldSelector.close();
//			}catch(Exception x){
//				x.printStackTrace();
//			}
//		}
//		_selectorLock.unlock();
	}
}
