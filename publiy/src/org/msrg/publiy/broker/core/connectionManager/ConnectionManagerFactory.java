package org.msrg.publiy.broker.core.connectionManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.msrg.publiy.broker.IBFTBrokerShadow;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.pubsub.core.IBFTSubscriptionManager;
import org.msrg.publiy.pubsub.core.IOverlayManager;
import org.msrg.publiy.pubsub.core.ISubscriptionManager;
import org.msrg.publiy.pubsub.core.messagequeue.IBFTMessageQueue;
import org.msrg.publiy.pubsub.core.messagequeue.IMessageQueue;
import org.msrg.publiy.pubsub.core.overlaymanager.IBFTOverlayManager;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery_Join;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;

import org.msrg.publiy.broker.core.BFTConnectionManagerJoin;
import org.msrg.publiy.broker.core.IConnectionManager;
import org.msrg.publiy.broker.core.IConnectionManagerJoin;
import org.msrg.publiy.broker.core.IConnectionManagerPS;
import org.msrg.publiy.broker.core.IConnectionManagerRecovery;
import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.networkcoding.connectionManager.client.ConnectionManagerNC_Client;
import org.msrg.publiy.component.IComponentListener;

public class ConnectionManagerFactory {

	public static IConnectionManagerPS getConnectionManagerBFT(IBrokerShadow brokerShadow, BFTConnectionManagerJoin conManJoin) {
		if(!brokerShadow.isBFT())
			throw new IllegalArgumentException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_BFT, "Creating a connectionManagerBFT from connectionManagerJoin.");
		IConnectionManagerPS conManPS = new BFTConnectionManager(conManJoin);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_BFT, "Created a connectionManagerBFT from connectionManagerJoin.");
	
		return conManPS;
	}
	
	public static IConnectionManagerPS getConnectionManagerPS(IBrokerShadow brokerShadow, IConnectionManagerJoin conManJoin) {
		if(!brokerShadow.isNormal())
			throw new IllegalArgumentException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_PS, "Creating a connectionManagerPS from connectionManagerJoin.");
		IConnectionManagerPS conManPS = new ConnectionManagerPS((ConnectionManagerJoin)conManJoin);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_PS, "Created a connectionManagerPS from connectionManagerJoin.");
	
		return conManPS;
	}
	
	public static IConnectionManagerPS getConnectionManagerPS(IBrokerShadow brokerShadow, IConnectionManagerRecovery conManRecovery) {
		if(!brokerShadow.isNormal())
			throw new IllegalArgumentException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_PS, "Creating a connectionManagerPS from connectionManagerRecovery.");
		IConnectionManagerPS conManPS = new ConnectionManagerPS((ConnectionManagerRecovery)conManRecovery);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_PS, "Created a connectionManagerPS from connectionManagerRecovery.");
		
		return conManPS;
	}
	
	public static AbstractConnectionManagerNC getConnectionManagerNC(IBrokerShadow brokerShadow, IConnectionManagerJoin conManJoin) {
		if(!brokerShadow.isNC())
			throw new IllegalStateException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_NC, "Creating a connectionManagerNC from connectionManagerJoin.");
		AbstractConnectionManagerNC conManNC = new ConnectionManagerNC((ConnectionManagerJoin)conManJoin);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_NC, "Created a connectionManagerNC from connectionManagerJoin.");
	
		return conManNC;
	}
	
	public static AbstractConnectionManagerNC getConnectionManagerNC_Client(IBrokerShadow brokerShadow, IConnectionManagerJoin conManJoin) {
		if(!brokerShadow.isNC())
			throw new IllegalStateException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_NC, "Creating a connectionManagerNC_Client from connectionManagerJoin.");
		AbstractConnectionManagerNC conManNC = new ConnectionManagerNC_Client((ConnectionManagerJoin)conManJoin);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_NC, "Created a connectionManagerNC_Client from connectionManagerJoin.");
	
		return conManNC;
	}
	
	public static AbstractConnectionManagerNC getConnectionManagerNC_Client(IBrokerShadow brokerShadow, IConnectionManagerRecovery conManRecovery) {
		if(!brokerShadow.isNC())
			throw new IllegalStateException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Creating a connectionManagerNC_Client from connectionManagerRecovery.");
		AbstractConnectionManagerNC conManNC = new ConnectionManagerNC_Client((ConnectionManagerRecovery)conManRecovery);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Created a connectionManagerNC_Client from connectionManagerRecovery.");
		
		return conManNC;
	}
	
	public static AbstractConnectionManagerNC getConnectionManagerNC(IBrokerShadow brokerShadow, IConnectionManagerRecovery conManRecovery) {
		if(!brokerShadow.isNC())
			throw new IllegalStateException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Creating a connectionManagerNC from connectionManagerRecovery.");
		AbstractConnectionManagerNC conManNC = new ConnectionManagerNC((ConnectionManagerRecovery)conManRecovery);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Created a connectionManagerNC from connectionManagerRecovery.");
		
		return conManNC;
	}
	
	public static IConnectionManagerMP getConnectionManagerMP(IBrokerShadow brokerShadow, IConnectionManagerJoin conManJoin) {
		if(!brokerShadow.isMP())
			throw new IllegalStateException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Creating a connectionManagerMP from connectionManagerJoin.");
		IConnectionManagerMP conManMP = new ConnectionManagerMP((ConnectionManagerJoin)conManJoin);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Created a connectionManagerMP from connectionManagerJoin.");
	
		return conManMP;
	}
	
	public static IConnectionManagerMP getConnectionManagerMP(IBrokerShadow brokerShadow, IConnectionManagerRecovery conManRecovery) {
		if(!brokerShadow.isMP())
			throw new IllegalStateException();
		
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Creating a connectionManagerMP from connectionManagerRecovery.");
		IConnectionManagerMP conManMP = new ConnectionManagerMP((ConnectionManagerRecovery)conManRecovery);
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_CON_MAN_MP, "Created a connectionManagerMP from connectionManagerRecovery.");
		
		return conManMP;
	}
	
	public static IConnectionManager getConnectionManager(
			ConnectionManagerTypes conManType,
			IBroker broker,
			IBrokerShadow brokerShadow,
			IComponentListener listener) throws IOException {
		LocalSequencer localSequencer = brokerShadow.getLocalSequencer();
		
		IOverlayManager overlayManager = brokerShadow.createOverlayManager();
		ISubscriptionManager subscriptionManager =
				brokerShadow.createSubscriptionManager(overlayManager, null, false);
		IMessageQueue messageQueue =
				brokerShadow.createMessageQueue(
						conManType, null, overlayManager, subscriptionManager);
		
		IConnectionManager conManager;
		switch(conManType) {
		case CONNECTION_MANAGER_JOIN:
			if(brokerShadow.isBFT())
				conManager = new BFTConnectionManagerJoin(
						broker,
						(IBFTBrokerShadow) brokerShadow,
						(IBFTOverlayManager) overlayManager,
						(IBFTSubscriptionManager)subscriptionManager,
						(IBFTMessageQueue) messageQueue);
			else
				conManager = new ConnectionManagerJoin(
						broker,
						brokerShadow,
						overlayManager,
						subscriptionManager,
						messageQueue);
			break;

		case CONNECTION_MANAGER_RECOVERY:
			TRecovery_Join[] trjs = readRecoveryTopology(
					localSequencer, brokerShadow.getRecoveryFileName());
			conManager = new ConnectionManagerRecovery(
					broker,
					brokerShadow,
					overlayManager,
					subscriptionManager,
					messageQueue,
					trjs);
			break;
			
		case CONNECTION_MANAGER_PUBSUB:
			conManager = new ConnectionManagerPS(
					brokerShadow,
					overlayManager,
					subscriptionManager,
					messageQueue);
			break;

		default:
			throw new IllegalArgumentException("Cannot instantiate connectionManager of type '" + conManType + "'.");
		}
		
		conManager.addNewComponentListener(listener);
		return conManager;
	}
	
	public static TRecovery_Join[] readRecoveryTopology(LocalSequencer localSequencer, String recoveryFileName) {
		try {
			List<TRecovery_Join> trjl = new LinkedList<TRecovery_Join>();
			FileReader freader = null;
			BufferedReader breader = null;
			try {
				freader = new FileReader(recoveryFileName);
				breader = new BufferedReader(freader);
				String line;
				while ( (line= breader.readLine()) != null) {
					TRecovery_Join trj = TRecovery_Join.getTRecoveryObject(localSequencer, line);
					if(trj != null)
						trjl.add(trj);
				}
			} catch(IOException iox) {
				iox.printStackTrace();
				if(freader!= null)
					freader.close();
				if(breader!=null)
					breader.close();
				return null;
			} finally {
				if(freader!= null)
					freader.close();
				if(breader!=null)
					breader.close();
			}
			TRecovery_Join []trjs = new TRecovery_Join[1];
			trjs = trjl.toArray(trjs);
			return trjs;		
		} catch(IOException iox) {
			return null;
		}
	}
}
