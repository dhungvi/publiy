package org.msrg.publiy.utils.log.casuallogger;

import java.io.IOException;
import java.text.DecimalFormat;

import org.msrg.publiy.broker.Broker;
import org.msrg.publiy.broker.BrokerShadow;
import org.msrg.publiy.broker.IBrokerShadow;

import org.msrg.publiy.pubsub.core.packets.multicast.TMulticastTypes;
import org.msrg.publiy.utils.SystemTime;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class ConfirmationDataLogger extends AbstractCasualLogger {
	
	public boolean _compact = false; 
	public static final int SAMPLING_INTERVAL = 1000;
	public static final int FORCE_FILE_FLUSH = 5;

	protected IBrokerShadow _brokerShadow;
	protected String _confimationDataLogFilename;
	
	private long _pub_confirmation_delay;
	private long _sub_confirmation_delay;
	private long _join_confirmation_delay;
	private long _conf_confirmation_delay;
	
	private int _bft_hb_created = 0;
	private int _pub_created = 0;
	private int _sub_created = 0;
	private int _join_created = 0;
	private int _conf_created = 0;
	private int _all_created = 0;
	
	private int _bft_hb_discarded = 0;
	private int _pub_discarded = 0;
	private int _sub_discarded = 0;
	private int _join_discarded = 0;
	private int _conf_discarded = 0;
	private int _all_discarded = 0;
	
	private int _last_bft_hb_created = 0;
	private int _last_pub_created = 0;
	private int _last_sub_created = 0;
	private int _last_join_created = 0;
	private int _last_conf_created = 0;
	private int _last_all_created = 0;
	
	private int _last_bft_hb_discarded = 0;
	private int _last_pub_discarded = 0;
	private int _last_sub_discarded = 0;
	private int _last_join_discarded = 0;
	private int _last_conf_discarded = 0;
	private int _last_all_discarded = 0;
	
	@SuppressWarnings("unused")
	private long _lastWrite = SystemTime.currentTimeMillis();
	
	public ConfirmationDataLogger(BrokerShadow brokerShadow) {
		brokerShadow.setConfirmationDataLogger(this);
		_brokerShadow = brokerShadow;
		_confimationDataLogFilename = _brokerShadow.getConfirmationDataLogFilename();
	}
	
	public void logMQNodeCreation(TMulticastTypes tmType) {
		synchronized (_lock)
		{
			_all_created ++;
			
			switch(tmType) {
			case T_MULTICAST_PUBLICATION_BFT_DACK:
				_bft_hb_created++;
				break;
				
			case T_MULTICAST_PUBLICATION_BFT:
			case T_MULTICAST_PUBLICATION_MP:
			case T_MULTICAST_PUBLICATION:
				_pub_created ++;
				break;

			case T_MULTICAST_SUBSCRIPTION:
				_sub_created ++;
				break;
				
			case T_MULTICAST_JOIN:
				_join_created ++;
				break;
				
			case T_MULTICAST_CONF:
				_conf_created ++;
				break;
				
			default: 
				throw new IllegalArgumentException("Unsupported TMType: " + tmType);
			}
		}
	}
	
	public void logMQNodeConfirmed(TMulticastTypes tmType, long confirmedAfter) {
		synchronized (_lock)
		{
			_all_discarded ++;
			
			switch(tmType) {
			case T_MULTICAST_PUBLICATION_BFT_DACK:
				_bft_hb_discarded++;
				break;
				
			case T_MULTICAST_PUBLICATION_BFT:
			case T_MULTICAST_PUBLICATION_MP:
			case T_MULTICAST_PUBLICATION:
				_pub_discarded ++;
				_pub_confirmation_delay += confirmedAfter;
				break;

			case T_MULTICAST_SUBSCRIPTION:
				_sub_discarded ++;
				_sub_confirmation_delay += confirmedAfter;
				break;

			case T_MULTICAST_JOIN:
				_join_discarded ++;
				_join_confirmation_delay += confirmedAfter;
				break;
				
			case T_MULTICAST_CONF:
				_conf_discarded ++;
				_conf_confirmation_delay += confirmedAfter;
				break;
			
			default: 
				throw new IllegalArgumentException("Unsupported TMType: " + tmType);
			}
		}
	}
	
	private DecimalFormat _decf3 = new DecimalFormat ("0.000") ;
	@Override
	protected void runMe() throws IOException{
		synchronized (_lock) {
			int i=4;
			if(_firstTime) {
				String headerLine = 
							/* 0 */ (_compact ? "" : ("CPU MEM TIME\t")) +
							
							/* 1 */ "PDL(" + (i++) + ")\t" +
							/* 2 */ "PCR(" + (i++) + ")\t" + 
							/* 3 */ "PDS(" + (i++) + ")\t" + 
							
							/* 4 */ "SDL(" + (i++) + ")\t" + 
							/* 5 */ "SCR(" + (i++) + ")\t" + 
							/* 6 */ "SDS(" + (i++) + ")\t" + 
							
							/* 7 */ "JDL(" + (i++) + ")\t" + 
							/* 8 */ "JCR(" + (i++) + ")\t" + 
							/* 9 */ "JDS(" + (i++) + ")\t" + 
							
							/* 10 */ "CDL(" + (i++) + ")\t" +
							/* 11 */ "CCR(" + (i++) + ")\t" +
							/* 12 */ "CDS(" + (i++) + ")\t" +
							
							/* 13 */ "ACR(" + (i++) + ")\t" +
							/* 14 */ "ADS(" + (i++) + ")\t" +

							/* 15 */ "BFTHBCR(" + (i++) + ")\t" +
							/* 16 */ "BFTHBDS(" + (i++) + ")\t" +
							"\n";
				
				_logFileWriter.write(headerLine);
				_firstTime = false;
			}
		
			_last_bft_hb_created += _bft_hb_created;
			_last_bft_hb_discarded += _bft_hb_discarded;

			_last_pub_created += _pub_created;
			_last_pub_discarded += _pub_discarded;

			_last_sub_created += _sub_created;
			_last_sub_discarded += _sub_discarded;
			
			_last_join_created += _join_created;
			_last_join_discarded += _join_discarded;
			
			_last_conf_created += _conf_created;
			_last_conf_discarded += _conf_discarded;
			
			_last_all_created += _all_created;
			_last_all_discarded += _all_discarded;
			
			long currTime = SystemTime.currentTimeMillis();
			String logLine = 
						_decf3.format(
						((double)_brokerShadow.getCpuCasualReader().getAverageCpuPerc()))
					+ " " +
						_decf3.format(
						((double)_brokerShadow.getCpuCasualReader().getUsedHeapMemory())/1000000)
					+ " " +
						/* 0 */ (_compact ? "" : BrokerInternalTimer.read().toString()) + "\t" + //"TLog" +
						
						/* 1 */ (_pub_confirmation_delay) +   "\t" + //(_last_tm_incoming) + ",\t" +
						/* 2 */ (_pub_created) +   "\t" + //(_last_tm_incoming) + ",\t" +
						/* 3 */ (_pub_discarded) +   "\t" + //(_last_tm_outgoing) + ";\t" +
					
						/* 4 */ (_sub_confirmation_delay) +   "\t" + //(_last_tm_incoming) + ",\t" +
						/* 5 */ (_sub_created) +   "\t" + //(_last_pub_incoming) + ",\t" +
						/* 6 */ (_sub_discarded) +   "\t" + //(_last_pub_outgoing) + ";\t" +

						/* 7 */ (_join_confirmation_delay) +   "\t" + //(_last_tm_incoming) + ",\t" +
						/* 8 */ (_join_created) +   "\t" + //(_last_sub_incoming) + ",\t" +									
						/* 9 */ (_join_discarded) +   "\t" + //(_last_sub_outgoing) + ";\t" +
						
						/* 10 */ (_conf_confirmation_delay) +   "\t" + //(_last_tm_incoming) + ",\t" +
						/* 11 */ (_conf_created) +   "\t" + //(_last_conf_incoming) + ",\t" +
						/* 12 */ (_conf_discarded) +   "\t" + //(_last_conf_outgoing) + ";\t" +

						/* 13 */ (_all_created) +   "\t" + //(_last_non_tm_incoming) + ",\t" +
						/* 14 */(_all_discarded) +   "\t" + //(_last_other_outgoing) + ";\t" +

						/* 15 */ (_bft_hb_created) +   "\t" + //(_last_non_tm_incoming) + ",\t" +
						/* 14 */(_bft_hb_discarded) +   "\t" + //(_last_other_outgoing) + ";\t" +
						
						"\n";
			
			_logFileWriter.write(logLine);
			
			_pub_confirmation_delay = 0;
			_sub_confirmation_delay = 0;
			_join_confirmation_delay = 0;
			_conf_confirmation_delay = 0;
			
			_bft_hb_created = 0;
			_pub_created = 0;
			_sub_created = 0;
			_join_created = 0;
			_conf_created = 0;
			_all_created = 0;
			
			_bft_hb_discarded = 0;
			_pub_discarded = 0;
			_sub_discarded = 0;
			_join_discarded = 0;
			_conf_discarded = 0;
			_all_discarded = 0;
			
			_last_pub_created = 0;
			_last_sub_created = 0;
			_last_join_created = 0;
			_last_conf_created = 0;
			_last_all_created = 0;
			
			_last_pub_discarded = 0;
			_last_sub_discarded = 0;
			_last_join_discarded = 0;
			_last_conf_discarded = 0;
			_last_all_discarded = 0;
			_lastWrite = currTime;
		}
	}
	
	@Override
	public boolean isEnabled() {
		return Broker.LOG_CONFIRMATION_DATA && super.isEnabled();
	}

	@Override
	protected String getFileName() {
		return _confimationDataLogFilename;
	}

	@Override
	public String toString() {
		return "ConfDataLogger-" + _brokerShadow.getBrokerID() + "[" + _problem + "_" + _initialized + "]";
	}
	
	@Override
	protected int getDefaultLoggingInterval() {
		return SAMPLING_INTERVAL;
	}
}
