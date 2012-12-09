package org.msrg.publiy.broker.core.bwenforcer;

import org.msrg.publiy.pubsub.multipath.loadweights.LoadProfiler;
import org.msrg.publiy.pubsub.multipath.loadweights.ProfilerBucket;
import org.msrg.publiy.utils.ElementTotal;
import org.msrg.publiy.utils.LimittedSizeList;

class BWEnforcer implements IBWEnforcer {

	protected final int _profilerCacheSize = 21;
	protected final int _bucketRefreshRate = 50;

	protected LoadProfiler _loadProfiler;
	protected int _totalAvailableBW;
	protected final Object _lock = new Object();
	
	BWEnforcer() {
		this(Integer.MAX_VALUE);
	}
			
	BWEnforcer(int totalAvailableBW) {
		_totalAvailableBW = totalAvailableBW;
		_loadProfiler = new BWLoadProfiler(_profilerCacheSize, _bucketRefreshRate);
	}

	@Override
	public boolean passedUsedThreshold(double usedThreshold) {
		return ((double)getUsedBW()) / ((double)getTotalAvailableBW()) >= usedThreshold; 
	}
	
	@Override
	public int getTotalAvailableBW() {
		synchronized(_lock) {
			return _totalAvailableBW;
		}
	}

	@Override
	public void addToUsedBW(int usedBW) {
		synchronized(_lock) {
			_loadProfiler.logMessage(null, null, usedBW);
		}
	}

	@Override
	public double getNormalizedAvailableBWPercentile() {
		synchronized(_lock) {
			return getRemainingBW() / getTotalAvailableBW();
		}
	}

	@Override
	public int getRemainingBW() {
		synchronized(_lock) {
			return getTotalAvailableBW() - getUsedBW();
		}
	}

	@Override
	public int getUsedBW() {
		synchronized(_lock) {
			ElementTotal<LimittedSizeList<ProfilerBucket>> profiledElementTotal = _loadProfiler.getHistoryOfType(null, null);
			double usedBW = (profiledElementTotal==null||profiledElementTotal._totaledValuesCount==0)?0:
				profiledElementTotal._total
				/
				profiledElementTotal._totaledValuesCount;
			return (int) (usedBW * (1000/_bucketRefreshRate));
		}
	}

	@Override
	public boolean hasRemainingBW() {
		synchronized(_lock) {
			return getRemainingBW() > 0;
		}
	}

	@Override
	public void setTotalAvailableBW(int totalAvailableBW) {
		synchronized(_lock) {
			_totalAvailableBW = totalAvailableBW;
		}
	}
}
