package org.msrg.publiy.communication.core.packet.types;

import java.nio.ByteBuffer;
import java.util.UnknownFormatConversionException;

import org.msrg.publiy.broker.IBrokerShadow;


import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPiece;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceId;
import org.msrg.publiy.pubsub.core.packets.TNetworkCoding_CodedPieceIdReq;
import org.msrg.publiy.pubsub.core.packets.multicast.TDack;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.utils.log.trafficcorrelator.correlationdumpspecifier.TrafficCorrelationDumpSpecifier;

import org.msrg.publiy.communication.core.packet.IPacketable;

// For inner-class use ONLY!
// Int values are used to put into packets (but not the aliases).
final class PacketableTypesByteCodes{
	static final byte TTEXT_CODED_VALUE						= 1;
	static final byte THEARTBEAT_CODED_VALUE 				= 2;
	static final byte TPING_CODED_VALUE 					= 3;
	static final byte TPINGREPLY_CODED_VALUE 				= 4;
	static final byte TLOCAL_LOAD_WEIGHT_CODED_VALUE		= 5;
	
	static final byte TCORRELATE_DUMP_SPEC_CODED_VALUE 		= 10;
	static final byte TCOMMAND_CODED_VALUE 					= 11;
	static final byte TCONFIRMATION_CODED_VALUE 			= 12;
		
	static final byte TMULTICAST_CODED_VALUE 				= 20;
//	static final byte TMULTICAST_JOIN_CODED_VALUE 			= x;
//	static final byte TMULTICAST_DEPART_CODED_VALUE 		= y;
//	static final byte TMULTICAST_SUBSCRIBE_CODED_VALUE 		= z;
//	static final byte TMULTICAST_UNSUBSCRIBE_CODED_VALUE 	= w;
//	static final byte TMULTICAST_PUBLISH_CODED_VALUE 		= v;

	static final byte TRECOVERY_CODED_VALUE					= 30;
	static final byte TGUIDED_CODED_VALUE 					= 31;
	static final byte TSESSIONINITIATION_CODED_VALUE 		= 32;
	static final byte TSESSIONINITIATION_BFT_CODED_VALUE 		= 33;
	static final byte TCONF_ACK_CODED_VALUE 				= 34;
	
	static final byte TDACK_CODED_VALUE 					= 40;
	
	static final byte TCODED_PIECE_CODED_VALUE				= 100;
	static final byte TCODED_PIECE_ID_CODED_VALUE			= 101;
	static final byte TCODED_PIECE_ID_REQ_CODED_VALUE		= 102;
};

public enum PacketableTypes {
	//Add new types here
	TTEXT					(PacketableTypesByteCodes.TTEXT_CODED_VALUE					, "TXT", 	false	), 
	THEARTBEAT				(PacketableTypesByteCodes.THEARTBEAT_CODED_VALUE			, "HRT", 	false	),
	TPING					(PacketableTypesByteCodes.TPING_CODED_VALUE					, "PNG", 	false	),
	TPINGREPLY				(PacketableTypesByteCodes.TPINGREPLY_CODED_VALUE			, "PNG_RPL",false	),
	TLOCALLOADWEIGHT		(PacketableTypesByteCodes.TLOCAL_LOAD_WEIGHT_CODED_VALUE	, "LOAD_WGHT", false),
	
	TCORRELATE_DUMP_SPEC	(PacketableTypesByteCodes.TCORRELATE_DUMP_SPEC_CODED_VALUE	, "DUMP_SPEC", false),
	TCOMMAND				(PacketableTypesByteCodes.TCOMMAND_CODED_VALUE				, "CMND", 	false	),
	TCONFIRMATION			(PacketableTypesByteCodes.TCONFIRMATION_CODED_VALUE			, "CONF", 	false	),
		
	TMULTICAST				(PacketableTypesByteCodes.TMULTICAST_CODED_VALUE			, "MULT", 	true	),

	TRECOVERY				(PacketableTypesByteCodes.TRECOVERY_CODED_VALUE				, "RECV", 	false	),
	TGUIDED					(PacketableTypesByteCodes.TGUIDED_CODED_VALUE				, "GUID", 	false	),
	TSESSIONINITIATION		(PacketableTypesByteCodes.TSESSIONINITIATION_CODED_VALUE	, "SINT", 	false	),
	TSESSIONINITIATIONBFT	(PacketableTypesByteCodes.TSESSIONINITIATION_BFT_CODED_VALUE	, "SINTBFT", 	false	),
	TCONF_ACK				(PacketableTypesByteCodes.TCONF_ACK_CODED_VALUE				, "CACK", 	false	),
	
	TDACK 					(PacketableTypesByteCodes.TDACK_CODED_VALUE					, "DACK", 	false	),
	
	TCODEDPIECE				(PacketableTypesByteCodes.TCODED_PIECE_CODED_VALUE			, "CPIECE",	false	),
	TCODEDPIECE_ID			(PacketableTypesByteCodes.TCODED_PIECE_ID_CODED_VALUE		, "CPICID",	false	),
	TCODEDPIECE_ID_REQ		(PacketableTypesByteCodes.TCODED_PIECE_ID_REQ_CODED_VALUE	, "CPICID",	false	)
	;
	// more types ... 
	
	public final byte _codedValue;
	public final String _shortName;
	public final boolean _canRedirect;
	private PacketableTypes(byte intCode, String shortName, boolean canRedirect){
		_codedValue = intCode;
		_shortName = shortName;
		_canRedirect = canRedirect;
	}
	
	@Override
	public String toString(){
		return _shortName;
	}
	
	public byte getCodedValue(){
		return _codedValue;
	}
	
	
	public static PacketableTypes getPacketableTypes(int intCode){
		switch( intCode ){
		case PacketableTypesByteCodes.TTEXT_CODED_VALUE:
			return TTEXT;
			
		case PacketableTypesByteCodes.THEARTBEAT_CODED_VALUE:
			return THEARTBEAT;
		
		case PacketableTypesByteCodes.TCOMMAND_CODED_VALUE:
			return TCOMMAND;
			
		case PacketableTypesByteCodes.TCONFIRMATION_CODED_VALUE:
			return TCONFIRMATION;
			
		case PacketableTypesByteCodes.TMULTICAST_CODED_VALUE:
			return TMULTICAST;
			
		case PacketableTypesByteCodes.TRECOVERY_CODED_VALUE:
			return TRECOVERY;
			
		case PacketableTypesByteCodes.TGUIDED_CODED_VALUE:
			return TGUIDED;
			
		case PacketableTypesByteCodes.TSESSIONINITIATION_CODED_VALUE:
			return TSESSIONINITIATION;
		
		case PacketableTypesByteCodes.TSESSIONINITIATION_BFT_CODED_VALUE:
			return TSESSIONINITIATIONBFT;
			
		case PacketableTypesByteCodes.TCONF_ACK_CODED_VALUE:
			return TCONF_ACK;
		
		case PacketableTypesByteCodes.TDACK_CODED_VALUE:
			return TDACK;
			
		case PacketableTypesByteCodes.TPING_CODED_VALUE:
			return TPING;
			
		case PacketableTypesByteCodes.TPINGREPLY_CODED_VALUE:
			return TPINGREPLY;

		case PacketableTypesByteCodes.TLOCAL_LOAD_WEIGHT_CODED_VALUE:
			return TLOCALLOADWEIGHT;
			
		case PacketableTypesByteCodes.TCORRELATE_DUMP_SPEC_CODED_VALUE:
			return TCORRELATE_DUMP_SPEC;
			
		case PacketableTypesByteCodes.TCODED_PIECE_CODED_VALUE:
			return TCODEDPIECE;
			
		case PacketableTypesByteCodes.TCODED_PIECE_ID_CODED_VALUE:
			return TCODEDPIECE_ID;
			
		case PacketableTypesByteCodes.TCODED_PIECE_ID_REQ_CODED_VALUE:
			return TCODEDPIECE_ID_REQ;

		default:
			throw new IllegalArgumentException("Dont' know this packet type: " + intCode);
		}
	}
	
	
	public static final IPacketable getObjectFromBuffer(
			IBrokerShadow brokerShadow,
			ByteBuffer bdy,
			PacketableTypes packetType,
			int contentSize,
			String annotations) throws UnknownFormatConversionException{
		// Add support for new types here
		switch(packetType){
		
		//Read a 'TText' type form the body buffer
		case TTEXT:
			{
				return new TText(bdy, 0);
			}
			
		//Read a 'THeartBeat' type form the body buffer
		case THEARTBEAT:
			{
				return THeartBeat.getObjectFromBuffer(bdy);
			}
			
		//Read a 'TDack' type form the body buffer
		case TDACK:
			{
				return TDack.getTDackBundleObject(bdy);
			}
			
		//Read a 'TMulticast' type form the body buffer	
		case TMULTICAST:
			{
				return TMulticast.getTMulticastObject(brokerShadow, bdy, contentSize, annotations);
			}
			
		//Read a 'TRecovery' type form the body buffer
		case TRECOVERY:
			{
				return TRecovery.getTRecoveryObject(bdy);
			}
		
		//Read a 'TSessionInitiationBFT' type form the body buffer
		case TSESSIONINITIATIONBFT:
			{
				return new TSessionInitiationBFT(bdy);
			}
			
		//Read a 'TSessionInitiation' type form the body buffer
		case TSESSIONINITIATION:
			{
				return new TSessionInitiation(bdy);
			}
			
		//Read a 'TConf_Ack' type form the body buffer
		case TCONF_ACK:
			{
				return new TConf_Ack(bdy);
			}
			
		//Read a 'TPing' type form the body buffer
		case TPING:
			{
				return TPing.getObjectFromBuffer(bdy);
			}
		
		//Read a 'TPingReply' type form the body buffer
		case TPINGREPLY:
			{
				return TPingReply.getObjectFromBuffer(bdy);
			}
			
		case TCOMMAND:
			{
				return TCommand.getObjectFromBuffer(bdy);
			}
			
		case TLOCALLOADWEIGHT:
			{
				return TLoadWeight.getObjectFromBuffer(bdy);
			}
			
		case TCORRELATE_DUMP_SPEC:
			{
				return TrafficCorrelationDumpSpecifier.getObjectFromBuffer(bdy);
			}
			
		case TCODEDPIECE:
			{
				return new TNetworkCoding_CodedPiece(bdy, 0);
			}
			
		case TCODEDPIECE_ID:
			{
				return new TNetworkCoding_CodedPieceId(bdy, 0);
			}
			
		case TCODEDPIECE_ID_REQ:
			{
				return TNetworkCoding_CodedPieceIdReq.getObjectFromBuffer(bdy, 0);
			}
		default:
			{
				throw new UnknownFormatConversionException("IPacketable::getObjectFromPacket: unknown type in header (" + packetType + ").");
			}
		}
	}

}
