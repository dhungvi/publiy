package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.text.DecimalFormat;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Conf;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT;
import org.msrg.publiy.pubsub.core.packets.multicast.TMulticast_Publish_BFT_Dack;
import org.msrg.publiy.pubsub.core.packets.recovery.TRecovery;
import org.msrg.publiy.utils.SystemTime;
import org.msrg.publiy.utils.Writers;
import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.trafficcorrelator.TrafficCorrelator;

import org.msrg.publiy.communication.core.packet.IPacketable;
import org.msrg.publiy.communication.core.packet.IRawPacket;
import org.msrg.publiy.communication.core.packet.PacketFactory;
import org.msrg.publiy.communication.core.packet.types.PacketableTypes;
import org.msrg.publiy.communication.core.packet.types.TLoadWeight;
import org.msrg.publiy.communication.core.sessions.ISession;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class TrafficLogger extends AbstractCasualLogger {
	@SuppressWarnings("unused")
	private long _lastWrite = SystemTime.currentTimeMillis();
	public boolean _compact = false; 
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;

	protected final IBrokerShadow _brokerShadow;
	protected final String _trafficLogFilename;
	protected final BrokerIdentityManager _idMan;
	
	private long _recent_tdack_incoming_count = 0,	_recent_tdack_incoming_bytes = 0;
	private long _recent_tm_incoming_count = 0,		_recent_tm_incoming_bytes = 0;
	private long _recent_pub_incoming_count = 0,	_recent_pub_incoming_bytes = 0;
	private long _recent_sub_incoming_count = 0,	_recent_sub_incoming_bytes = 0;
	private long _recent_conf_j_incoming_count = 0,	_recent_conf_j_incoming_bytes = 0;
	private long _recent_conf_sub_incoming_count = 0,	_recent_conf_sub_incoming_bytes = 0;
	private long _recent_conf_pub_incoming_count = 0,	_recent_conf_pub_incoming_bytes = 0;
	private long _recent_non_tm_incoming_count = 0,	_recent_non_tm_incoming_bytes = 0;
	private long _recent_tdack_outgoing_count = 0,	_recent_tdack_outgoing_bytes = 0;
	private long _recent_tm_outgoing_count = 0,		_recent_tm_outgoing_bytes = 0;
	private long _recent_pub_outgoing_count = 0,	_recent_pub_outgoing_bytes = 0;
	private long _recent_sub_outgoing_count = 0,	_recent_sub_outgoing_bytes = 0;
	private long _recent_conf_j_outgoing_count = 0,	_recent_conf_j_outgoing_bytes = 0;
	private long _recent_conf_sub_outgoing_count = 0,	_recent_conf_sub_outgoing_bytes = 0;
	private long _recent_conf_pub_outgoing_count = 0,	_recent_conf_pub_outgoing_bytes = 0;
	private long _recent_non_tm_outgoing_count = 0,	_recent_non_tm_outgoing_bytes = 0;
	
	private long _total_tdack_incoming_count = 0,	_total_tdack_incoming_bytes = 0;
	private long _total_tm_incoming_count = 0,		_total_tm_incoming_bytes = 0;
	private long _total_pub_incoming_count = 0,		_total_pub_incoming_bytes = 0;
	private long _total_sub_incoming_count = 0,		_total_sub_incoming_bytes = 0;
	private long _total_conf_j_incoming_count = 0,	_total_conf_j_incoming_bytes = 0;
	private long _total_conf_sub_incoming_count = 0,	_total_conf_sub_incoming_bytes = 0;
	private long _total_conf_pub_incoming_count = 0,	_total_conf_pub_incoming_bytes = 0;
	private long _total_non_tm_incoming_count = 0,	_total_non_tm_incoming_bytes = 0;
	private long _total_tdack_outgoing_count = 0,	_total_tdack_outgoing_bytes = 0;
	private long _total_tm_outgoing_count = 0,		_total_tm_outgoing_bytes = 0;
	private long _total_pub_outgoing_count = 0,		_total_pub_outgoing_bytes = 0;
	private long _total_sub_outgoing_count = 0,		_total_sub_outgoing_bytes = 0;
	private long _total_conf_j_outgoing_count = 0,	_total_conf_j_outgoing_bytes = 0;
	private long _total_conf_sub_outgoing_count = 0,	_total_conf_sub_outgoing_bytes = 0;
	private long _total_conf_pub_outgoing_count = 0,	_total_conf_pub_outgoing_bytes = 0;
	private long _total_other_outgoing_count = 0, 	_total_other_outgoing_bytes = 0;
	
	public TrafficLogger(BrokerShadow brokerShadow) {
		_brokerShadow = brokerShadow;
		brokerShadow.setTrafficLogger(this);
		_trafficLogFilename = _brokerShadow.getTrafficLogFilename();
		_idMan = _brokerShadow.getBrokerIdentityManager();
	}
	
	private void customizedRule(String comment, IRawPacket raw) {
		if(Broker.RELEASE && !Broker.DEBUG) // FOR DEBUGGING
			return;

		IPacketable msg = PacketFactory.unwrapObject(_brokerShadow, raw);
		
		PacketableTypes packetType = raw.getType();
		switch( packetType) {
		case THEARTBEAT:
//			THeartBeat tHeartBeat = (THeartBeat) msg;
//			LoggerFactory.getLogger().info(this, "THEARTBEAT " + comment + " " + tHeartBeat);
			break;
			
		case TCOMMAND:
//			TCommand tCommand = (TCommand) PacketFactory.unwrapObject(raw);
//			LoggerFactory.getLogger().info(this, "TCOMMAND " + comment + " " + tCommand + " BETWEEN: " + raw.getSender().getPort() + "_" + raw.getReceiver().getPort());
			break;
			
		case TSESSIONINITIATION:
//			TSessionInitiation sInit = (TSessionInitiation) msg;
//			LoggerFactory.getLogger().info(this, "TSINIT " + comment + " " + sInit + " BETWEEN: " + raw.getSender().getPort() + "_" + raw.getReceiver().getPort());
			break;
			
			
		case TMULTICAST:
//			if(true) // FOR DEBUGGING
//				return;
			
			TMulticastTypes tmType = TMulticast.getTMulticastType(raw);
			switch(tmType) {
			case T_MULTICAST_JOIN:
//				TMulticast_Join tmj = (TMulticast_Join) msg;
//				if(tmj.getJoiningNode().equals(LocalSequencer.getLocalSequencer().getLocalAddress()) ||
//						tmj.getJoinPoint().equals(LocalSequencer.getLocalSequencer().getLocalAddress()))
//					LoggerFactory.getLogger().info(this, "TMJ " + comment + " " + tmj + " BETWEEN: " + raw.getSender().getPort() + "_" + raw.getReceiver().getPort()); // OK
				break;
				
			case T_MULTICAST_CONF:
//				TMulticast_Conf tmc = (TMulticast_Conf) msg;
//				if(tmc.getOriginalTMulticastType() == TMulticastTypes.T_MULTICAST_PUBLICATION || 
//						tmc.getOriginalTMulticastType() == TMulticastTypes.T_MULTICAST_PUBLICATION_MP)
//					LoggerFactory.getLogger().info(this, "TMC " +comment + " " + tmc + " BETWEEN: " + raw.getSender().getPort() + "_" + raw.getReceiver().getPort()); // OK
				break;
				
			case T_MULTICAST_SUBSCRIPTION:
//				TMulticast_Subscribe tms = (TMulticast_Subscribe) msg;
//				LoggerFactory.getLogger().info(this, comment + " " + tms + " BETWEEN: " + raw.getSender().getPort() + "_" + raw.getReceiver().getPort()); // OK
				break;

			case T_MULTICAST_PUBLICATION_NC:
			case T_MULTICAST_PUBLICATION_MP:
			case T_MULTICAST_PUBLICATION:
				TMulticast_Publish tmp = (TMulticast_Publish) msg;
				LoggerFactory.getLogger().info(this, "TMP " + comment + " " + tmp.toStringTooLong() + " BETWEEN: " + Writers.write(raw.getSender()) + "_" + Writers.write(raw.getReceiver()) + " TMSender: " + tmp.getSenderAddress()); // OK
				break;
				
			case T_MULTICAST_PUBLICATION_BFT:
				TMulticast_Publish_BFT bftTM = (TMulticast_Publish_BFT) msg;
				LoggerFactory.getLogger().info(this, "BFTTMP " + comment + " " + bftTM.toString(_idMan) + " BETWEEN: " + Writers.write(raw.getSender()) + "_" + Writers.write(raw.getReceiver()) + " TMSender: " + bftTM.getSenderAddress()); // OK
				break;

			case T_MULTICAST_PUBLICATION_BFT_DACK:
				TMulticast_Publish_BFT_Dack dack = (TMulticast_Publish_BFT_Dack) msg;
				LoggerFactory.getLogger().info(this, "TMP_BFT_DACK " + comment + " " + dack.toString() + " BETWEEN: " + Writers.write(raw.getSender()) + "_" + Writers.write(raw.getReceiver()) + " TMSender: " + dack.getSenderAddress()); // OK
				break;
				
			default:
				break;
			}
			return;
			
		case TRECOVERY:
			if(Broker.DEBUG) {
				TRecovery tr = (TRecovery) msg;
//				if(tr.isLastJoin() || tr.isLastSubscription()) //|| tr.getType() == TRecoveryTypes.T_RECOVERY_JOIN)
					LoggerFactory.getLogger().info(this, "Last " + comment + " " + tr + " BETWEEN: " + raw.getSender().getPort() + "_" + raw.getReceiver().getPort());
			}
			return;
			
		case TLOCALLOADWEIGHT:
			TLoadWeight tLoadWeight = (TLoadWeight) msg;
			LoggerFactory.getLogger().info(this, "LW: " + comment + ">>> " + tLoadWeight + " BETWEEN: " + raw.getSender().getPort() + "_" + raw.getReceiver().getPort() + " *** " + raw.getSize());
			return;
		
		default:
			break;
		}
	}
	
	public void logIncoming(ISession session, IRawPacket raw) {
		customizedRule("Received", raw);
		
		synchronized (_lock) {
			if(raw == null)
				return;
			
			int bytes = raw.getLength();
			PacketableTypes type = raw.getType();
			if(type == PacketableTypes.TMULTICAST)
			{
				TMulticast tm = (TMulticast) PacketFactory.unwrapObject(_brokerShadow, raw);
				if(Broker.CORRELATE)
					TrafficCorrelator.getInstance().messageArrived(tm, session);
				
				_recent_tm_incoming_count++;
				_recent_tm_incoming_bytes+=bytes;
				
				TMulticastTypes tmType = TMulticast.getTMulticastType(raw);
				switch(tmType) {
				case T_MULTICAST_CONF:
					TMulticast_Conf conf = (TMulticast_Conf) tm;
					TMulticastTypes tmConfType = conf.getOriginalTMulticastType();
					switch(tmConfType) {
					case T_MULTICAST_JOIN:
					case T_MULTICAST_DEPART:
						_recent_conf_j_incoming_count++;
						_recent_conf_j_incoming_bytes+=bytes;
						break;
						
					case T_MULTICAST_PUBLICATION:
					case T_MULTICAST_PUBLICATION_BFT:
					case T_MULTICAST_PUBLICATION_MP:
					case T_MULTICAST_PUBLICATION_NC:
						_recent_conf_pub_incoming_count++;
						_recent_conf_pub_incoming_bytes+=bytes;
						break;
						
					case T_MULTICAST_SUBSCRIPTION:
					case T_MULTICAST_UNSUBSCRIPTION:
						_recent_conf_sub_incoming_count++;
						_recent_conf_sub_incoming_bytes+=bytes;
						break;
						
					default:
						break;
					}
					break;
				
				case T_MULTICAST_PUBLICATION_BFT:
				case T_MULTICAST_PUBLICATION_MP: 
				case T_MULTICAST_PUBLICATION:
					_recent_pub_incoming_count++;
					_recent_pub_incoming_bytes+=bytes;
					break;
				
				case T_MULTICAST_SUBSCRIPTION:
					_recent_sub_incoming_count++;
					_recent_sub_incoming_bytes+=bytes;
					break;
					
				default:
					break;
				}
			}
			else{
				if(type == PacketableTypes.TDACK)
				{
					_recent_tdack_incoming_count++;
					_recent_tdack_incoming_bytes+=bytes;
				}
				
				_recent_non_tm_incoming_count++;
				_recent_non_tm_incoming_bytes+=bytes;
			}
		}
	}
	
	public void logOutgoing(ISession session, IRawPacket raw) {
		customizedRule("Sending", raw);
		
		synchronized (_lock) {
			if(raw == null)
				return;
			
			int bytes = raw.getLength();
			PacketableTypes type = raw.getType();
			if(type == PacketableTypes.TMULTICAST)
			{
				TMulticast tm = (TMulticast) PacketFactory.unwrapObject(_brokerShadow, raw);
				if(Broker.CORRELATE)
					TrafficCorrelator.getInstance().messageSent(tm, session);
				
//				TMulticast tm = (TMulticast)PacketFactory.unwrapObject(raw);
//				if(tm.getSenderAddress() == null)
//					throw new IllegalStateException("Sender of '" + tm.toStringTooLong() + "' is null");
					
				_recent_tm_outgoing_count++;
				_recent_tm_outgoing_bytes+=bytes;
				
				TMulticastTypes tmType = TMulticast.getTMulticastType(raw);
				switch(tmType) {
				case T_MULTICAST_CONF:
					TMulticast_Conf conf = (TMulticast_Conf)PacketFactory.unwrapObject(_brokerShadow, raw);
					TMulticastTypes tmConfType = conf.getOriginalTMulticastType();
					switch(tmConfType) {
					case T_MULTICAST_JOIN:
					case T_MULTICAST_DEPART:
						_recent_conf_j_outgoing_count++;
						_recent_conf_j_outgoing_bytes+=bytes;
						break;
						
					case T_MULTICAST_PUBLICATION:
					case T_MULTICAST_PUBLICATION_BFT:
					case T_MULTICAST_PUBLICATION_MP:
						_recent_conf_pub_outgoing_count++;
						_recent_conf_pub_outgoing_bytes+=bytes;
						break;
						
					case T_MULTICAST_SUBSCRIPTION:
					case T_MULTICAST_UNSUBSCRIPTION:
						_recent_conf_sub_outgoing_count++;
						_recent_conf_sub_outgoing_bytes+=bytes;
						break;
						
					default:
						break;
					}
					break;
				
				case T_MULTICAST_PUBLICATION_BFT:
				case T_MULTICAST_PUBLICATION_MP:
				case T_MULTICAST_PUBLICATION:
					_recent_pub_outgoing_count++;
					_recent_pub_outgoing_bytes+=bytes;
					break;
				
				case T_MULTICAST_SUBSCRIPTION:
					_recent_sub_outgoing_count++;
					_recent_sub_outgoing_bytes+=bytes;
					break;
					
				default:
					break;
				}
			}
			else{
				if(type == PacketableTypes.TDACK) {
					_recent_tdack_outgoing_count++;
					_recent_tdack_outgoing_bytes+=bytes;
				}
				_recent_non_tm_outgoing_count++;
				_recent_non_tm_outgoing_bytes+=bytes;
			}
		}
	}

	protected DecimalFormat _decf2 = new DecimalFormat ("0.00");
	protected DecimalFormat _decf3 = new DecimalFormat ("0.000");
	@Override
	protected void runMe() throws IOException {
		synchronized (_lock) {
			if(_firstTime) {
				String headerLine = 
							/* ****************************************************************** */
							/* 0 */ "#" + (_compact ? "" : ("CPU MEM TIME\t")) +
							/* ****************************************************************** */
							/* 1 */ "ITM ITMB\t" + 
							/* 2 */ "OTM OTMB\t" + 
							/* ****************************************************************** */
							/* 3 */ "IPU IPUB\t" + 
							/* 4 */ "OPU OPUB\t" + 
							/* ****************************************************************** */
							/* 5 */ "ISU ISUB\t" + 
							/* 6 */ "OSU OSUB\t" + 
							/* ****************************************************************** */
							/* 7 */ "ICJ ICJB\t" +
							/* 8 */ "OCJ OCJB\t" +
							
							/* 9 */ "ICS ICSB\t" +
							/* 10 */ "OCS OCSB\t" +
							
							/* 11 */ "ICP ICPB\t" +
							/* 12 */ "OCP OCPB\t" +
							/* ****************************************************************** */
							/* 13 */ "ITDK ITDKB\t" +
							/* 14 */ "OTDK OTDKB\t" +
							/* 15 */ "IOTH IOTHB\t" +
							/* 16 */ "OOTH OOTHB\t" +
							/* ****************************************************************** */
							"\n";
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
		
			_total_tdack_incoming_count += _recent_tdack_incoming_count;
			_total_tdack_incoming_bytes += _recent_tdack_incoming_bytes;
			
			_total_tdack_outgoing_count += _recent_tdack_outgoing_count;
			_total_tdack_outgoing_bytes += _recent_tdack_outgoing_bytes;
			
			_total_tm_incoming_count += _recent_tm_incoming_count;
			_total_tm_incoming_bytes += _recent_tm_incoming_bytes;
			
			_total_tm_outgoing_count += _recent_tm_outgoing_count;
			_total_tm_outgoing_bytes += _recent_tm_outgoing_bytes;
			
			_total_non_tm_incoming_count += _recent_non_tm_incoming_count;
			_total_non_tm_incoming_bytes += _recent_non_tm_incoming_bytes;
			
			_total_other_outgoing_count += _recent_non_tm_outgoing_count;
			_total_other_outgoing_bytes += _recent_non_tm_outgoing_bytes;
			
			_total_conf_j_outgoing_count += _recent_conf_j_outgoing_count;
			_total_conf_j_outgoing_bytes += _recent_conf_j_outgoing_bytes;
			_total_conf_sub_outgoing_count += _recent_conf_sub_outgoing_count;
			_total_conf_sub_outgoing_bytes += _recent_conf_sub_outgoing_bytes;
			_total_conf_pub_outgoing_count += _recent_conf_pub_outgoing_count;
			_total_conf_pub_outgoing_bytes += _recent_conf_pub_outgoing_bytes;
			
			_total_conf_j_incoming_count += _recent_conf_j_incoming_count;
			_total_conf_j_incoming_bytes += _recent_conf_j_incoming_bytes;
			_total_conf_sub_incoming_count += _recent_conf_sub_incoming_count;
			_total_conf_sub_incoming_bytes += _recent_conf_sub_incoming_bytes;
			_total_conf_pub_incoming_count += _recent_conf_pub_incoming_count;
			_total_conf_pub_incoming_bytes += _recent_conf_pub_incoming_bytes;
			
			_total_pub_incoming_count += _recent_pub_incoming_count;
			_total_pub_incoming_bytes += _recent_pub_incoming_bytes;
			
			_total_sub_incoming_count += _recent_sub_incoming_count;
			_total_sub_incoming_bytes += _recent_sub_incoming_bytes;
			
			_total_pub_outgoing_count += _recent_pub_outgoing_count;
			_total_pub_outgoing_bytes += _recent_pub_outgoing_bytes;
			
			_total_sub_outgoing_count += _recent_sub_outgoing_count;
			_total_sub_outgoing_bytes += _recent_sub_outgoing_bytes;
			
			
			long currTime = SystemTime.currentTimeMillis();
			String logLine = 
						/* ****************************************************************** */
						/*	 */ _decf2.format(_brokerShadow.getCpuCasualReader().getAverageCpuPerc()) + "% " +
						/*	 */	_decf3.format(((double)_brokerShadow.getCpuCasualReader().getUsedHeapMemory())/1000000) + " " +
						/* 0 */ (_compact ? "" : BrokerInternalTimer.read().toString()) + " " + //"TLog" +
						/* ****************************************************************** */
						/* 1 */ (_recent_tm_incoming_count) +   " " + //(_total_tm_incoming) + ",\t" +
						/* 1 */ (_recent_tm_incoming_bytes) +   "\t" + //(_total_tm_incoming) + ",\t" +
						/* 2 */ (_recent_tm_outgoing_count) +   " " + //(_total_tm_outgoing) + ";\t" +
						/* 2 */ (_recent_tm_outgoing_bytes) +   "\t" + //(_total_tm_outgoing) + ";\t" +
						/* ****************************************************************** */
						/* 3 */ (_recent_pub_incoming_count) +   " " + //(_total_pub_incoming) + ",\t" +
						/* 3 */ (_recent_pub_incoming_bytes) +   "\t" + //(_total_pub_incoming) + ",\t" +
						/* 4 */ (_recent_pub_outgoing_count) +   " " + //(_total_pub_outgoing) + ";\t" +
						/* 4 */ (_recent_pub_outgoing_bytes) +   "\t" + //(_total_pub_outgoing) + ";\t" +
						/* ****************************************************************** */
						/* 5 */ (_recent_sub_incoming_count) +   " " + //(_total_sub_incoming) + ",\t" +
						/* 5 */ (_recent_sub_incoming_bytes) +   "\t" + //(_total_sub_incoming) + ",\t" +
						/* 6 */ (_recent_sub_outgoing_count) +   " " + //(_total_sub_outgoing) + ";\t" +
						/* 6 */ (_recent_sub_outgoing_bytes) +   "\t" + //(_total_sub_outgoing) + ";\t" +
						/* ****************************************************************** */
						/* 7 */ (_recent_conf_j_incoming_count) +   " " + //(_total_conf_j_incoming) + ",\t" +
						/* 7 */ (_recent_conf_j_incoming_bytes) +   "\t" + //(_total_conf_j_incoming) + ",\t" +
						/* 8 */ (_recent_conf_j_outgoing_count) +   " " + //(_total_conf_j_outgoing) + ";\t" +
						/* 8 */ (_recent_conf_j_outgoing_bytes) +   "\t" + //(_total_conf_j_outgoing) + ";\t" +
						
						/* 9 */ (_recent_conf_sub_incoming_count) +   " " + //(_total_conf_sub_incoming) + ",\t" +
						/* 9 */ (_recent_conf_sub_incoming_bytes) +   "\t" + //(_total_conf_sub_incoming) + ",\t" +
						/* 10 */ (_recent_conf_sub_outgoing_count) +   " " + //(_total_conf_sub_outgoing) + ";\t" +
						/* 10 */ (_recent_conf_sub_outgoing_bytes) +   "\t" + //(_total_conf_sub_outgoing) + ";\t" +
						
						/* 11 */ (_recent_conf_pub_incoming_count) +   " " + //(_total_conf_pub_incoming) + ",\t" +
						/* 11 */ (_recent_conf_pub_incoming_bytes) +   "\t" + //(_total_conf_pub_incoming) + ",\t" +
						/* 12 */ (_recent_conf_pub_outgoing_count) +   " " + //(_total_conf_pub_outgoing) + ";\t" +
						/* 12 */ (_recent_conf_pub_outgoing_bytes) +   "\t" + //(_total_conf_pub_outgoing) + ";\t" +
						/* ****************************************************************** */
						/* 13 */ (_recent_tdack_incoming_count) +   " " + //(_total_non_tm_incoming) + ",\t" +
						/* 13 */ (_recent_tdack_incoming_bytes) +   "\t" + //(_total_non_tm_incoming) + ",\t" +
						/* 14 */(_recent_tdack_outgoing_count) +   " " + //(_total_non_tm_incoming) + ",\t" +
						/* 14 */(_recent_tdack_outgoing_bytes) +   "\t" + //(_total_non_tm_incoming) + ",\t" +
						/* ****************************************************************** */
						/* 15 */(_recent_non_tm_incoming_count) +   " " + //(_total_non_tm_incoming) + ",\t" +
						/* 15 */(_recent_non_tm_incoming_bytes) +   "\t" + //(_total_non_tm_incoming) + ",\t" +
						/* 16 */(_recent_non_tm_outgoing_count) +   " " + //(_total_other_outgoing) + ";\t" +
						/* 16 */(_recent_non_tm_outgoing_bytes) +   "\t" + //(_total_other_outgoing) + ";\t" +
						/* ****************************************************************** */
								"\n";
			_logFileWriter.write(logLine);
			
			_recent_tdack_incoming_count = 0;
			_recent_tdack_incoming_bytes = 0;
			
			_recent_tdack_outgoing_count = 0;
			_recent_tdack_outgoing_bytes = 0;
			
			_recent_tm_incoming_count = 0;
			_recent_tm_incoming_bytes = 0;
			
			_recent_tm_outgoing_count = 0; 
			_recent_tm_outgoing_bytes = 0;
			
			_recent_non_tm_incoming_count = 0;
			_recent_non_tm_incoming_bytes = 0;
			
			_recent_non_tm_outgoing_count = 0;
			_recent_non_tm_outgoing_bytes = 0;
			
			_recent_conf_j_outgoing_count = 0;
			_recent_conf_j_outgoing_bytes = 0;
			
			_recent_conf_sub_outgoing_count = 0;
			_recent_conf_sub_outgoing_bytes = 0;
			
			_recent_conf_pub_outgoing_count = 0;
			_recent_conf_pub_outgoing_bytes = 0;
			
			_recent_conf_j_incoming_count = 0;
			_recent_conf_j_incoming_bytes = 0;
			
			_recent_conf_sub_incoming_count = 0;
			_recent_conf_sub_incoming_bytes = 0;
			
			_recent_conf_pub_incoming_count = 0;
			_recent_conf_pub_incoming_bytes = 0;
			
			_recent_sub_incoming_count = 0;
			_recent_sub_incoming_bytes = 0;
			
			_recent_sub_incoming_count = 0;
			_recent_sub_incoming_bytes = 0;
			
			_recent_pub_incoming_count = 0;
			_recent_pub_incoming_bytes = 0;
			
			_recent_pub_outgoing_count = 0;
			_recent_pub_outgoing_bytes = 0;
			
			_recent_sub_outgoing_count = 0;
			_recent_sub_outgoing_bytes = 0;
			
			_lastWrite = currTime;
		}
	}
	
	@Override
	public boolean isEnabled() {
		return Broker.LOG_TRAFFIC && super.isEnabled();
	}

	@Override
	protected String getFileName() {
		return _trafficLogFilename;
	}

	@Override
	public String toString() {
		return "TrafficLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}

	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}

	public LoggingSource getLogSource() {
		return LoggingSource.LOG_SRC_TRAFFIC_LOGGER;
	}
}
