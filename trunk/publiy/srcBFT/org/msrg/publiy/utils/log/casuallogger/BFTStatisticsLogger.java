package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.io.Writer;
import java.net.InetSocketAddress;

import org.msrg.publiy.utils.Writers;

import org.msrg.publiy.broker.BFTBrokerShadow;
import org.msrg.publiy.broker.core.sequence.IBFTVerifiable;
import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class BFTStatisticsLogger extends StatisticsLogger {

	private int _matchingSetInconsistencies = 0;
	
	private int _numberOfInputBFTPublications = 0;
	private int _numberOfMatchingAnchorsForInputBFTPublications = 0;
	private int _numberOfSequencePairsInInputBFTPublications = 0;
	private int _numberOfOutputBFTPublications = 0;
	private int _numberOfSequencePairsInOutputBFTPublications = 0;
	private int _numberOfLocalSequencePairsInOutputBFTPublications =0;
	
	private int _numberOfInputBFTDackPublications = 0;
	private int _numberOfMatchingAnchorsForInputBFTDackPublications = 0;
	private int _numberOfSequencePairsInInputBFTDackPublications = 0;
	private int _numberOfOutputBFTDackPublications = 0;
	private int _numberOfSequencePairsInOutputBFTDackPublications = 0;
	private int _numberOfLocalSequencePairsInOutputBFTDackPublications =0;

	public BFTStatisticsLogger(BFTBrokerShadow broker) {
		super(broker);
	}

	public void addInputBFTPublicationInformation(
			int numberOfMatchingAnchorsForInputBFTPublications, int numberOfSequencePairsInInputBFTPublications) {
		synchronized(_lock) {
			_numberOfInputBFTPublications += 1;
			
			if(numberOfMatchingAnchorsForInputBFTPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfMatchingAnchorsForInputBFTPublications);
			_numberOfMatchingAnchorsForInputBFTPublications += numberOfMatchingAnchorsForInputBFTPublications;

			if(numberOfSequencePairsInInputBFTPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfSequencePairsInInputBFTPublications);
			_numberOfSequencePairsInInputBFTPublications += numberOfSequencePairsInInputBFTPublications;
		}
	}
	
	public void addOutputBFTPublicationInformation(int numberOfSequencePairsInOutputBFTPublications, int numberOfLocalSequencePairsInOutputBFTPublications) {
		synchronized(_lock) {
			_numberOfOutputBFTPublications += 1;
			
			if(numberOfSequencePairsInOutputBFTPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfSequencePairsInOutputBFTPublications);
			_numberOfSequencePairsInOutputBFTPublications += numberOfSequencePairsInOutputBFTPublications;

			if(numberOfLocalSequencePairsInOutputBFTPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfLocalSequencePairsInOutputBFTPublications);
			_numberOfLocalSequencePairsInOutputBFTPublications += numberOfLocalSequencePairsInOutputBFTPublications;
		}
	}

	public void addInputBFTDackPublicationInformation(
			int numberOfMatchingAnchorsForInputBFTDackPublications, int numberOfSequencePairsInInputBFTDackPublications) {
		synchronized(_lock) {
			_numberOfInputBFTDackPublications += 1;
			
			if(numberOfMatchingAnchorsForInputBFTDackPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfMatchingAnchorsForInputBFTDackPublications);
			_numberOfMatchingAnchorsForInputBFTDackPublications += numberOfMatchingAnchorsForInputBFTDackPublications;

			if(numberOfSequencePairsInInputBFTDackPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfSequencePairsInInputBFTDackPublications);
			_numberOfSequencePairsInInputBFTDackPublications += numberOfSequencePairsInInputBFTDackPublications;
		}
	}
	
	public void addOutputBFTDackPublicationInformation(int numberOfSequencePairsInOutputBFTDackPublications, int numberOfLocalSequencePairsInOutputBFTDackPublications) {
		synchronized(_lock) {
			_numberOfOutputBFTDackPublications += 1;
			
			if(numberOfSequencePairsInOutputBFTDackPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfSequencePairsInOutputBFTDackPublications);
			_numberOfSequencePairsInOutputBFTDackPublications += numberOfSequencePairsInOutputBFTDackPublications;
			
			if(numberOfLocalSequencePairsInOutputBFTDackPublications < 0)
				throw new IllegalArgumentException("Invalid: " + numberOfLocalSequencePairsInOutputBFTDackPublications);
			_numberOfLocalSequencePairsInOutputBFTDackPublications += numberOfLocalSequencePairsInOutputBFTDackPublications;
		}
	}
	

	@Override
	protected String createLogLinePrivately() {
		return super.createLogLinePrivately() + '\t' +
				_matchingSetInconsistencies + '\t' +
				_numberOfInputBFTPublications + '\t' +
				_numberOfMatchingAnchorsForInputBFTPublications + '\t' +
				_numberOfSequencePairsInInputBFTPublications + '\t' +
				_numberOfOutputBFTPublications + '\t' +
				_numberOfSequencePairsInOutputBFTPublications + '\t' +
				_numberOfLocalSequencePairsInOutputBFTPublications + '\t' +
				
				/* BFT DACK */
				
				_numberOfInputBFTDackPublications + '\t' +
				_numberOfMatchingAnchorsForInputBFTDackPublications + '\t' +
				_numberOfSequencePairsInInputBFTDackPublications + '\t' +
				_numberOfOutputBFTDackPublications + '\t' +
				_numberOfSequencePairsInOutputBFTDackPublications + '\t' +
				_numberOfLocalSequencePairsInOutputBFTDackPublications;
	}
	
	@Override
	protected void reinitVariables() {
		super.reinitVariables();
		_matchingSetInconsistencies = 0;
		
		_numberOfInputBFTPublications = 0;
		_numberOfMatchingAnchorsForInputBFTPublications = 0;
		_numberOfSequencePairsInInputBFTPublications = 0;
		_numberOfOutputBFTPublications = 0;
		_numberOfSequencePairsInOutputBFTPublications = 0;
		_numberOfLocalSequencePairsInOutputBFTPublications =0;
		
		_numberOfInputBFTDackPublications = 0;
		_numberOfMatchingAnchorsForInputBFTDackPublications = 0;
		_numberOfSequencePairsInInputBFTDackPublications = 0;
		_numberOfOutputBFTDackPublications = 0;
		_numberOfSequencePairsInOutputBFTDackPublications = 0;
		_numberOfLocalSequencePairsInOutputBFTDackPublications =0;
	}
	
	@Override
	protected int writeHeader(int i, Writer ioWriter) throws IOException {
		i = super.writeHeader(i, ioWriter);
		
		String headerLine = ((i == 0) ? "#" : "\t") +
				"MATCING_INCOSTINCENCY" + "(" + (++i) + ")\t" +
				"IN-BFT-PUB-COUNT" + "(" + (++i) + ")\t" +
				"IN-BFT-PUB-ANCH" + "(" + (++i) + ")\t" +
				"IN-BFT-PUB-SP" + "(" + (++i) + ")\t" +
				"OUT-BFT-PUB-COUNT" + "(" + (++i) + ")\t" +
				"OUT-BFT-PUB-SPCOUNT" + "(" + (++i) + ")\t" +
				"OUT-BFT-PUB-LSPCOUNT" + "(" + (++i) + ")\t" +

				"IN-BFTDACK-COUNT" + "(" + (++i) + ")\t" +
				"IN-BFTDACK-ANCH" + "(" + (++i) + ")\t" +
				"IN-BFTDACK-SP" + "(" + (++i) + ")\t" +
				"OUT-BFTDACK-COUNT" + "(" + (++i) + ")\t" +
				"OUT-BFTDACK-SPCOUNT" + "(" + (++i) + ")\t" +
				"OUT-BFTDACK-LSPCOUNT" + "(" + (++i) + ")";
		
		ioWriter.write(headerLine);
		return i;
	}
	
	public void matchingSetInconsistency(IBFTVerifiable verifiable, InetSocketAddress verifier) {
		BrokerInternalTimer.inform("Matching inconsistency: " + Writers.write(verifier, _brokerShadow.getBrokerIdentityManager()) + " vs. " + verifiable);
		synchronized (_lock) {
			_matchingSetInconsistencies += 1;
		}
	}
}
