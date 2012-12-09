package org.msrg.publiy.client.networkcoding.publisher;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;


import org.msrg.raccoon.matrix.bulk.SliceMatrix;



import org.msrg.publiy.networkcodes.engine.CodingEngineLogger;
import org.msrg.publiy.node.NodeTypes;
import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.pubsub.core.ITMConfirmationListener;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.casuallogger.StatisticsLogger;
import org.msrg.publiy.utils.log.casuallogger.coding.CasualContentLogger;

import org.msrg.publiy.broker.BrokerOpState;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.PubForwardingStrategy;
import org.msrg.publiy.broker.core.connectionManager.ConnectionManagerNC;
import org.msrg.publiy.broker.core.sequence.Sequence;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSBulkMatrix;
import org.msrg.publiy.broker.networkcodes.matrix.bulk.PSSourceCodedBatch;
import org.msrg.publiy.client.multipath.publisher.FilePublisher_MP;
import org.msrg.publiy.component.ComponentStatus;

public class FilePublisher_NC extends FilePublisher_MP {
	
	final static int _rows = new Integer(System.getProperty("NC.ROWS", "100"));
	final static int _cols = new Integer(System.getProperty("NC.COLS", "100"));
	final InetSocketAddress _clientsJoiningBrokerAddress;
	
	final static SliceMatrix[] CONTNET_SLICES;
	static {
		PSBulkMatrix psBM = PSBulkMatrix.createBulkMatixIncrementalData(null, _rows, _cols);
		CONTNET_SLICES = psBM.getSlices();
	}
	
	public FilePublisher_NC(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, String filename,
			int delay, Properties arguments) throws IOException {
		super(localAddress, opState, null, brokerForwardingStrategy,
				filename, delay, arguments);
		
		_clientsJoiningBrokerAddress = joinPointAddress;
		
		if(!_brokerShadow.isNC())
			throw new IllegalStateException();
		
		if(!_brokerShadow.isMP())
			throw new IllegalStateException();
		
		_contentLogger = new CasualContentLogger((BrokerShadow)_brokerShadow);
		_codingEngineLogger = new CodingEngineLogger((BrokerShadow)_brokerShadow);
	}
	
	public FilePublisher_NC(InetSocketAddress localAddress,
			BrokerOpState opState, InetSocketAddress joinPointAddress,
			PubForwardingStrategy brokerForwardingStrategy, Properties props)
			throws IOException {
		super(localAddress, opState, null, brokerForwardingStrategy, props);
		
		_clientsJoiningBrokerAddress = joinPointAddress;
		
		if(!_brokerShadow.isNC())
			throw new IllegalStateException();
		
		if(!_brokerShadow.isMP())
			throw new IllegalStateException();
		
		_contentLogger = new CasualContentLogger((BrokerShadow)_brokerShadow);
		_codingEngineLogger = new CodingEngineLogger((BrokerShadow)_brokerShadow);
	}

	@Override
	public void prepareToStart() {
		_casualLoggerEngine.registerCasualLogger(_contentLogger);
		_casualLoggerEngine.registerCasualLogger(_codingEngineLogger);
		super.prepareToStart();
	}
	
	@Override
	public TMulticast_Publish[] publish(Publication[] publications, ITMConfirmationListener tmConfirmationListener) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void publishNext() {
		if(_paused || _componentStatus != ComponentStatus.COMPONENT_STATUS_RUNNING) {
			LoggerFactory.getLogger().debug(this, "Returning from publishNext because:" + _paused + "_" + _componentStatus);
			return;
		}
		
		scheduleNextTimer();
		
		int nonNullPublicationCount = 0;
		Publication[] publications = createPublicationBatch(getPublicationBatchSize(), false);
		if(publications == null)
			return;
		
		for(Publication publication : publications) {
			publication.addStringPredicate("PUBLISH-TIME", "" + SystemTime.currentTimeMillis());
			PSSourceCodedBatch psSourceCodedBatch = createPSSourceCodedBatch(publication);
			if(psSourceCodedBatch == null)
				continue;
			
			nonNullPublicationCount++;
			((ConnectionManagerNC)_connectionManager).publishContent(psSourceCodedBatch);
			_pubGenerationLogger.publicationGenerated(
					psSourceCodedBatch.getSourceSequence(), TMulticastTypes.T_MULTICAST_PUBLICATION_NC);
		}
		
		StatisticsLogger statLogger = _brokerShadow.getStatisticsLogger();
		if(statLogger != null)
			statLogger.newPublish(nonNullPublicationCount);
	}

	protected PSSourceCodedBatch createPSSourceCodedBatch(Publication publication) {
		if(publication == null)
			return null;
		
		Sequence sourceSeq = _brokerShadow.getLocalSequencer().getNext();
		PSBulkMatrix psBM = new PSBulkMatrix(sourceSeq, _rows, _cols);
		for(int i=0 ; i<_rows ; i++)
			psBM.add(i, CONTNET_SLICES[i]);
		
		PSSourceCodedBatch sourceContnet = new PSSourceCodedBatch(sourceSeq, publication, psBM);
		return sourceContnet;
	}
	
	@Override
	public NodeTypes getNodeType() {
		return NodeTypes.NODE_NC_PUBLISHER;
	}
	
	@Override
	public InetSocketAddress getClientsJoiningBrokerAddress() {
		return _clientsJoiningBrokerAddress;
	}
}
