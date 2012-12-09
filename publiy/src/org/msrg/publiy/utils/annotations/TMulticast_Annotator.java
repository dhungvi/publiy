package org.msrg.publiy.utils.annotations;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;

import org.msrg.publiy.broker.Broker;


import org.msrg.publiy.publishSubscribe.Publication;
import org.msrg.publiy.publishSubscribe.Subscription;
import org.msrg.publiy.publishSubscribe.matching.SimplePredicate;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastAnnotatorFactory;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Subscribe;

import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;

import org.msrg.publiy.broker.core.sequence.LocalSequencer;
import org.msrg.publiy.broker.core.sequence.Sequence;

public class TMulticast_Annotator implements IAnnotator {

	public static int _recentHrdIndex = 0;

	static public String ANNOTATION_FILED_SEPARATOR = "\t";
	static public String ANNOTATION_ANNOTATION_SEPARATOR = "\n";
	static public String ANNOTATION_ANNOTATION_GROUP_SEPARATOR = "." + ANNOTATION_ANNOTATION_SEPARATOR;
	
	@Override
	public synchronized IAnnotatable annotate(LocalSequencer localSequencer, IAnnotatable annotatable, AnnotationEvent annSrc, String msg) {
		if ( Broker.ANNOTATE == false )
			return annotatable;

		Sequence sourceSequence = annotatable.getSourceSequence();
		if ( sourceSequence == null )
			return annotatable;
		
		long time = System.nanoTime();
		Sequence seq = localSequencer.getNext();
		
		String annotationString = annSrc.toString() + ANNOTATION_FILED_SEPARATOR 
									+ sourceSequence + ANNOTATION_FILED_SEPARATOR 
									+ time + ANNOTATION_FILED_SEPARATOR 
									+ seq + ANNOTATION_FILED_SEPARATOR  
									+ msg + ANNOTATION_ANNOTATION_SEPARATOR;
		
		annotatable.annotate(annotationString);
		
		return annotatable;
	}

	private FileWriter fwriter;
	private String lastFilename;
	
	@Override
	public synchronized void forceFlush() {
		try{
			if ( fwriter!=null )
				fwriter.flush();
		}catch(IOException iox) {}
	}
	
	private int _unflushedCount = 0;
	private static final int MAX_UNFLUSHED_COUNT = 100;
	
	@Override
	public synchronized void dumpAnnotations(IAnnotatable annotatable, String filename) {
		
		if ( Broker.ANNOTATE == false )
			return;
		
		try{
			if ( fwriter == null || !filename.equalsIgnoreCase(lastFilename) ) {
				if ( fwriter != null )
					fwriter.close();
				lastFilename = filename;
				fwriter = new FileWriter(filename, true);
				if ( ++_unflushedCount % MAX_UNFLUSHED_COUNT == 0 )
					fwriter.flush();
			}
			TMulticast tm = (TMulticast) annotatable;
			String annotations = tm.getAnnotations();

			fwriter.write(annotations + ANNOTATION_ANNOTATION_GROUP_SEPARATOR);
			
		}catch (IOException iox) {
			return;
		}

	}

	@Override
	public String getAnnotations(IAnnotatable annotatable) {
		return ((TMulticast)annotatable).getAnnotations();
	}

	public static void main(String[] argv) {
		Publication pub = new Publication();
		pub.addPredicate("attr1", 1);
		Subscription sub = new Subscription();
		SimplePredicate sp = SimplePredicate.buildSimplePredicate("attr2", '=', 12);
		sub.addPredicate(sp); 
		LocalSequencer localSequencer = LocalSequencer.init(null, new InetSocketAddress("127.0.0.1", 2000));

		TMulticast_Subscribe tms = new TMulticast_Subscribe(sub, Broker.bAddress3, localSequencer);
//		tms.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress4, 600));
		
		IRawPacket raw_tms = PacketFactory.wrapObject(localSequencer, tms);
		TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(localSequencer, raw_tms, AnnotationEvent.ANNOTATION_EVENT_MESSAGE_SENT, "Annotation String");
		TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(localSequencer, raw_tms, AnnotationEvent.ANNOTATION_EVENT_MESSAGE_SENT, "Annotation String");
		
		TMulticast tmp2 = (TMulticast) PacketFactory.unwrapObject(null, raw_tms);
		String annotations = tmp2.getAnnotations();
		System.out.println("ANN:::" + annotations); // OK
		
		TMulticast_Conf tmc = new TMulticast_Conf(tms, Broker.bAddress5, Broker.bAddress6, localSequencer.getNext());
//		tmc.setLocalSequence(Sequence.getPsuedoSequence(Broker.bAddress12, 890));
		IRawPacket raw_tmc = PacketFactory.wrapObject(localSequencer, tmc);
		TMulticastAnnotatorFactory.getTMulticast_Annotator().annotate(localSequencer, raw_tmc, AnnotationEvent.ANNOTATION_EVENT_MESSAGE_SENT, "Annotation String");
		TMulticast_Conf tmc2 = (TMulticast_Conf) PacketFactory.unwrapObject(null, raw_tmc);
		
		System.out.println(tmc2);
		String annotations2 = tmc2.getAnnotations();
		System.out.println(annotations2); // OK
	}
}
