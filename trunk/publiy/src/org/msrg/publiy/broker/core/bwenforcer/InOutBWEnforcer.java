package org.msrg.publiy.broker.core.bwenforcer;

public class InOutBWEnforcer {
	
	protected BWEnforcer _inBWEnforcer;
	protected BWEnforcer _outBWEnforcer;
	protected final BWMetric _bwMetric;

	public InOutBWEnforcer(BWMetric bwMetric, int inBW, int outBW) {
		_bwMetric = bwMetric;
		switch(_bwMetric) {
		case BW_METRIC_IN_BYTES:
		case BW_METRIC_IN_MSG_COUNT:
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			_inBWEnforcer = new BWEnforcer(inBW);
			break;
			
		case BW_METRIC_UNLIMITTED:
			_inBWEnforcer = new UnlimittedBWEnforcer();
			break;
			
		default:
			break;
		}
		
		switch(_bwMetric) {
		case BW_METRIC_OUT_BYTES:
		case BW_METRIC_OUT_MSG_COUNT:
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			_outBWEnforcer = new BWEnforcer(outBW);
			_outBWEnforcer = new BWEnforcer(outBW);
			break;
			
		case BW_METRIC_UNLIMITTED:
			_outBWEnforcer = new UnlimittedBWEnforcer();
			break;
			
		default:
			break;
		}
	}

	public int getUsedInBW() {
		switch(_bwMetric) {
		case BW_METRIC_UNLIMITTED:
		case BW_METRIC_IN_BYTES:
		case BW_METRIC_IN_MSG_COUNT:
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			return _inBWEnforcer.getUsedBW();
			
		default:
			return -1;
		}
	}

	public int getUsedOutBW() {
		switch(_bwMetric) {
		case BW_METRIC_UNLIMITTED:
		case BW_METRIC_OUT_BYTES:
		case BW_METRIC_OUT_MSG_COUNT:
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			return _outBWEnforcer.getUsedBW();
			
		default:
			return -1;
		}
	}
	
	public BWMetric getSupportedBWMetric() {
		return _bwMetric;
	}
	
	public boolean hasRemaingBW() {
		switch(_bwMetric) {
		case BW_METRIC_UNLIMITTED:
			return true;

		case BW_METRIC_IN_BYTES:
		case BW_METRIC_IN_MSG_COUNT:
			if(!_inBWEnforcer.hasRemainingBW())
				return false;
			else
				return true;

		case BW_METRIC_OUT_BYTES:
		case BW_METRIC_OUT_MSG_COUNT:
			if(!_outBWEnforcer.hasRemainingBW())
				return false;
			else
				return true;
			
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			if(!_outBWEnforcer.hasRemainingBW())
				return false;
			else if(!_inBWEnforcer.hasRemainingBW())
				return false;
			else
				return true;
			
		default:
			throw new UnsupportedOperationException("" + _bwMetric);
		}
	}
	
	public void addIncomingMessage(int messageSize) {
		switch(_bwMetric) {
		case BW_METRIC_IN_BYTES:
		case BW_METRIC_INOUT_BYTES:
			_inBWEnforcer.addToUsedBW(messageSize);
			break;
			
		case BW_METRIC_IN_MSG_COUNT:
		case BW_METRIC_INOUT_MSG_COUNT:
			_inBWEnforcer.addToUsedBW(1);
			break;
			
		default:
			break;
		}
	}
	
	public void addOutgoingMessage(int messageSize) {
		switch(_bwMetric) {
		case BW_METRIC_OUT_BYTES:
		case BW_METRIC_INOUT_BYTES:
			_outBWEnforcer.addToUsedBW(messageSize);
			break;
			
		case BW_METRIC_OUT_MSG_COUNT:
		case BW_METRIC_INOUT_MSG_COUNT:
			_outBWEnforcer.addToUsedBW(1);
			break;
			
		default:
			break;
		}
	}
	
	public int getTotalAvailableInBW() {
		switch(_bwMetric) {
		case BW_METRIC_IN_BYTES:
		case BW_METRIC_IN_MSG_COUNT:
			return _inBWEnforcer.getTotalAvailableBW();
		
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			return _outBWEnforcer.getTotalAvailableBW();
			
		case BW_METRIC_OUT_BYTES:
		case BW_METRIC_OUT_MSG_COUNT:
			return _outBWEnforcer.getTotalAvailableBW();
			
		case BW_METRIC_UNLIMITTED:
			return Integer.MAX_VALUE;
			
		default:
			throw new UnsupportedOperationException("Unknown metric: " + _bwMetric);
		}
	}
	
	public int getTotalUsedInBW() {
		switch(_bwMetric) {
		case BW_METRIC_IN_BYTES:
		case BW_METRIC_IN_MSG_COUNT:
			return _inBWEnforcer.getUsedBW();
		
		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			return _outBWEnforcer.getUsedBW();
			
		case BW_METRIC_OUT_BYTES:
		case BW_METRIC_OUT_MSG_COUNT:
			return _outBWEnforcer.getUsedBW();
			
		case BW_METRIC_UNLIMITTED:
			return Integer.MAX_VALUE;
			
		default:
			throw new UnsupportedOperationException("Unknown metric: " + _bwMetric);
		}
	}
	
	public boolean pastUsedThreshold(double usedThreshold) {
		boolean ret = true;
		switch(_bwMetric) {
		case BW_METRIC_OUT_BYTES:
		case BW_METRIC_OUT_MSG_COUNT:
			if(_outBWEnforcer.passedUsedThreshold(usedThreshold))
				ret = false;
			break;

		case BW_METRIC_INOUT_BYTES:
		case BW_METRIC_INOUT_MSG_COUNT:
			if(_outBWEnforcer.passedUsedThreshold(usedThreshold))
				ret = false;
			if(_inBWEnforcer.passedUsedThreshold(usedThreshold))
				ret = false;
			break;

		case BW_METRIC_IN_BYTES:
		case BW_METRIC_IN_MSG_COUNT:
			if(_inBWEnforcer.passedUsedThreshold(usedThreshold))
				ret = false;
			break;
			
		case BW_METRIC_UNLIMITTED:
			ret = false;
		}
		
		return ret;
	}
}
