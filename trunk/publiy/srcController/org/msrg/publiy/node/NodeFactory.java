package org.msrg.publiy.node;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.IBroker;
import org.msrg.publiy.broker.IBrokerOpStateListener;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.client.subscriber.SimpleFileSubscriber;
import org.msrg.publiy.component.IComponent;
import org.msrg.publiy.component.IComponentListener;

import org.msrg.publiy.utils.PropertyGrabber;
import org.msrg.publiy.utils.log.ILoggerSource;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;




public class NodeFactory implements IBrokerOpStateListener, IComponentListener, ILoggerSource {
	
	public static final NodeInstantiationData<Broker> BROKER_INIT_DATA1 = 
		new NodeInstantiationData<Broker>(Broker.class, Broker.bAddress1, BrokerOpState.BRKR_PUBSUB_JOIN, null, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0, null);

	public static final NodeInstantiationData<SimpleFileSubscriber> SUBSCRIBER_INIT_DATA1 = 
		new NodeInstantiationData<SimpleFileSubscriber>(SimpleFileSubscriber.class, SimpleFileSubscriber.sAddress1, BrokerOpState.BRKR_PUBSUB_JOIN, Broker.bAddress1, PubForwardingStrategy.PUB_FORWARDING_STRATEGY_0, SimpleFileSubscriber.SIMPLE_PROPERTIES);
	
	public static IBroker createNewNodeInstance(NodeInstantiationData<?> nodeInstantiationData, IBrokerOpStateListener brokerOpStateListener, IComponentListener componentListener) 
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_NODE_FACTORY, "Instantiating a class '" + nodeInstantiationData.getXClass() + "' with data: " + nodeInstantiationData);
		Class<?> xClass = nodeInstantiationData.getXClass();
		Class<?>[] classArgs = nodeInstantiationData.getInstantiationArgTypes();
		Object[] constructorArgs = nodeInstantiationData.getInstantiationData();
		
		String joinPointStr = nodeInstantiationData.getProperty(PropertyGrabber.PROPERTY_JOINPOINT_ADDRESS);
		if ( joinPointStr != null && !joinPointStr.equalsIgnoreCase("null") ){
			InetSocketAddress joinPointAddress = new InetSocketAddress(joinPointStr.split(":")[0], new Integer(joinPointStr.split(":")[1]).intValue());
			if ( joinPointAddress != null )
				constructorArgs[NodeInstantiationData.JOINPOINT_ADDRESS_INDEX] = joinPointAddress;
		}
		
		Constructor<?> constructor = xClass.getConstructor(classArgs);
		Object newInstance = constructor.newInstance(constructorArgs);
		IBroker newBrokerInstance = (IBroker) newInstance;
		newBrokerInstance.registerBrokerOpStateListener(brokerOpStateListener);
		newBrokerInstance.addNewComponentListener(componentListener);
		return newBrokerInstance;
	}
	
	@Override
	public void brokerOpStateChanged(IBroker broker) {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_NODE_FACTORY, "Yep!! " + broker.getBrokerID() + ":" + broker.getBrokerOpState());
	}
	
	@Override
	public void componentStateChanged(IComponent component) {
		LoggerFactory.getLogger().info(LoggingSource.LOG_SRC_NODE_FACTORY, "Yoop!! " + component.getComponentName() + ":" + component.getComponentState());
	}
	
	public static void main(String[] argv) throws Exception {
		NodeFactory nodeFactory = new NodeFactory();

//		IBroker broker = NodeFactory.createNewNodeInstance(BROKER_INIT_DATA1, nodeFactory, nodeFactory);
//		broker.prepareAndStart();

		SimpleFileSubscriber subscriber = (SimpleFileSubscriber)NodeFactory.createNewNodeInstance(SUBSCRIBER_INIT_DATA1, nodeFactory, nodeFactory);
		subscriber.prepareToStart();
	}

	@Override
	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_NODE_FACTORY;
	}
}
