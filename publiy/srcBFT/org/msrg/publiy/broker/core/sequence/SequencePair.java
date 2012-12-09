package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;
import java.security.PrivateKey;
import java.util.List;

import org.msrg.raccoon.utils.BytesUtil;

import org.msrg.publiy.broker.BrokerIdentityManager;
import org.msrg.publiy.broker.core.nodes.OverlayNodeId;

import org.msrg.publiy.utils.log.LoggerFactory;
import org.msrg.publiy.utils.log.LoggingSource;
import org.msrg.publiy.utils.log.casuallogger.BFTDSALogger;
import org.msrg.publiy.utils.security.dsa.DSAUtils;

public class SequencePair extends Sequence {

	
	/**
	 * Auto generated.
	 */
	private static final long serialVersionUID = -3828708434192364040L;

	protected final InetSocketAddress _vAddr;
	protected final byte[] _digest;
	protected final byte[] _data;
	public final int _lastReceivedOrder;
	public final int _lastDiscardedOrder;
	protected byte[] _dsa;
	protected byte[] _dataAndDsa;
	
//	public SequencePair(IBFTIssuer issuer, long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder) {
//		this(issuer, epoch, order, vAddr, new byte[0], lastReceivedOrder, lastDiscardedOrder);
//	}
	
	protected SequencePair(IBFTIssuer issuer, long epoch, int order, InetSocketAddress vAddr, byte[] digest) {
		this(issuer, epoch, order, vAddr, digest, 0, 0);
	}

	public SequencePair(IBFTIssuer issuer, long epoch, int order, InetSocketAddress vAddr, IBFTDigestable digestable, int lastReceivedOrder, int lastDiscardedOrder) {
		this(issuer, epoch, order, vAddr, digestable.getDigest(), lastReceivedOrder, lastDiscardedOrder);
	}
	
	protected SequencePair(IBFTIssuer issuer, long epoch, int order, InetSocketAddress vAddr, byte[] digest, int lastReceivedOrder, int lastDiscardedOrder) {
		super(issuer.getIssuerAddress(), epoch, order);

		_vAddr = vAddr;
		_digest = digest;
		_lastReceivedOrder = lastReceivedOrder;
		_lastDiscardedOrder = lastDiscardedOrder;
		_data = populateData();
		_dsa = sign(issuer.getPrivateKey(), _data);
		
		BFTDSALogger bftDSALogger = issuer.getBFTDSALogger();
		if(bftDSALogger != null)
			bftDSALogger.dsaSigned();
		
//		if(!verifyDSA(_dsa, digest, iProxy, epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder))
//			throw new IllegalStateException();
	}
	
	public SequencePair(IBFTIssuer issuer, long epoch, int order, InetSocketAddress vAddr, IBFTDigestable digestable) {
		this(issuer, epoch, order, vAddr, digestable.getDigest());
	}

	@Override
	public boolean succeeds(Sequence seq2) {
		SequencePair sp2 = (SequencePair) seq2;
		if(!_vAddr.equals(sp2._vAddr))
			throw new IllegalArgumentException();
		if(!getIssuerAddress().equals(sp2.getIssuerAddress()))
			throw new IllegalArgumentException();
		
		if(_epoch == sp2._epoch)
			return _order > sp2._order;
		else
			return _epoch > sp2._order;
	}
	
	@Override
	public int hashCode() {
		return _address.hashCode() + _vAddr.hashCode() + (int) _epoch + _order;
	}
	
	@Override
	public boolean succeedsOrEquals(Sequence seq2) {
		SequencePair sp2 = (SequencePair) seq2;
		if(!_vAddr.equals(sp2._vAddr))
			throw new IllegalArgumentException();
		if(!getIssuerAddress().equals(sp2.getIssuerAddress()))
			throw new IllegalArgumentException();
		
		if(_epoch == sp2._epoch)
			return _order >= sp2._order;
		else
			return _epoch >= sp2._order;
	}
	
	public static SequencePair decode(BFTDSALogger dsaLogger, IBFTIssuerProxyRepo iProxyRepo, byte[] data, int offset) {
		InetSocketAddress iAddr = Sequence.readAddressFromByteArray(data, offset+1+data[offset]);
		InetSocketAddress vAddr = Sequence.readAddressFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8);
		
		byte[] digest = Sequence.subarray(data, offset+1, data[offset]);
		IBFTIssuerProxy iProxy = iProxyRepo.getIssuerProxy(iAddr);
		if(iProxy == null) {
			LoggerFactory.getLogger().error(LoggingSource.LOG_SRC_UNKNOWN, "IProxy is null: " + iAddr);
			return null;
		}
		long epoch = Sequence.readLongFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		int order = Sequence.readIntFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8);
		int lastReceivedOrder = Sequence.readIntFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE);
		int lastDiscardedOrder = Sequence.readIntFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Integer.SIZE/8);
		byte[] dsaToBeVerified = Sequence.subarray(data,
				offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+1+2*Integer.SIZE/8,
				data[offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+2*Integer.SIZE/8]);

		if(!iProxyRepo.getLocalVerifierAddress().equals(vAddr))
			return new PsuedoSequencePair(dsaLogger, digest, iProxy, epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder, dsaToBeVerified);
		else
			return new SequencePair(dsaLogger, digest, iProxy, epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder, dsaToBeVerified);
	}
	
	protected SequencePair(IBFTIssuerProxyRepo iProxyRepo, byte[] data, int offset) {
		this(iProxyRepo,
				Sequence.subarray(data, offset+1, data[offset]),
				Sequence.readAddressFromByteArray(data, offset+1+data[offset]),
				Sequence.readLongFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE),
				Sequence.readIntFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8),
				Sequence.readAddressFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8),
				Sequence.readIntFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE),
				Sequence.readIntFromByteArray(data, offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Integer.SIZE/8),
				Sequence.subarray(data,
						offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+1+2*Integer.SIZE/8,
						data[offset+1+data[offset]+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+Long.SIZE/8+Integer.SIZE/8+Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE+2*Integer.SIZE/8]));
	}
	
	public SequencePair(IBFTIssuerProxyRepo iProxyRepo, byte[] digest, InetSocketAddress iAddr, long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder, byte[] dsaToBeVerified) {
		this(null, digest, iProxyRepo.getIssuerProxy(iAddr), epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder, dsaToBeVerified);
	}
	
	public SequencePair(BFTDSALogger dsaLogger, byte[] digest, IBFTIssuerProxy iProxy, long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder, byte[] dsaToBeVerified) {
		super(iProxy.getIssuerAddress(), epoch, order);

		_vAddr = vAddr;
		_digest = digest;
		_lastReceivedOrder = lastReceivedOrder;
		_lastDiscardedOrder = lastDiscardedOrder;
		_data = populateData();
		
		boolean verificationResult =
				verifyDSA(dsaToBeVerified, digest, iProxy, epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder);
		
		if(dsaLogger != null)
			dsaLogger.dsaVerify(verificationResult);
		
		if(verificationResult) {
			_dsa = dsaToBeVerified;
		} else {
			_dsa = null;
		}
	}
		
	public static byte[] sign(PrivateKey prikey, byte[] data) {
		return DSAUtils.getInstance().signData(data, prikey);
	}
	
	@Override
	public int getContentSize() {
		return encode().length;
	}
	
	public InetSocketAddress getVerifierAddress() {
		return _vAddr;
	}
	
	public InetSocketAddress getIssuerAddress() {
		return _address;
	}

	public byte[] getdigest() {
		return _digest;
	}
	
	public byte[] encode() {
		if(_dataAndDsa == null)
			_dataAndDsa = BytesUtil.combineInsertB2sizeAsByte(_data, _dsa);

		return _dataAndDsa;
	}

	private synchronized byte[] populateData() {
		return populateData(_digest, getIssuerAddress(), _epoch, _order, getVerifierAddress(), _lastReceivedOrder, _lastDiscardedOrder);
	}
	
	protected static byte[] populateData(byte[] digest, InetSocketAddress iAddr, long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder) {
		int len = 1 + digest.length + Sequence.INETSOCKETADDRESS_IN_BYTES_ARRAY_SIZE * 2 + Long.SIZE/8 + Integer.SIZE/8 + Integer.SIZE/8*2;
		byte[] data = new byte[len];
		int i=0;

		byte digestlen = (byte) (digest==null ? 0 : digest.length);
		
		// Add digest len
		data[i++] = digestlen;
		
		// Add digest
		for(int j=0 ; j<digestlen ; j++)
			data[i++] = digest[j];
		
		// Add issuer address
		i = Sequence.putAddressInByteArray(iAddr, data, i);
		
		// Add epoch
		i = Sequence.putLongInByteArray(epoch, data, i);
		
		// Add order
		i = Sequence.putIntInByteArray(order, data, i);

		// Add verifier address
		i = Sequence.putAddressInByteArray(vAddr, data, i);
		
		// Add last received order
		i = Sequence.putIntInByteArray(lastReceivedOrder, data, i);
		
		// Add last received order
		i = Sequence.putIntInByteArray(lastDiscardedOrder, data, i);

		return data;
	}
	
	public boolean verifyDSA(byte[] dsaToVerify, byte[] digest, IBFTIssuerProxy iProxy, long epoch, int order, InetSocketAddress vAddr, int lastReceivedOrder, int lastDiscardedOrder) {
		byte[] dataToBeVerified = populateData(digest, iProxy.getIssuerAddress(), epoch, order, vAddr, lastReceivedOrder, lastDiscardedOrder);
		boolean succeed = DSAUtils.getInstance().verifyData(dataToBeVerified, dsaToVerify, iProxy.getPublicKey());
		return succeed;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj == null)
			return false;
		
		if(!SequencePair.class.isAssignableFrom(obj.getClass()))
			return false;
		
		SequencePair sp = (SequencePair) obj;
		if(_data!=null && sp._data!=null) {
			if(_data.length != sp._data.length)
				return false;
			for(int i=0 ; i<_data.length ; i++)
				if(_data[i] != sp._data[i])
					return false;
			return true;
		}
		
		if(_digest==null ^ sp._digest==null)
			return false;
		if(_digest!=null) {
			if(_digest.length != sp._digest.length)
				return false;
			for(int i=0 ; i<_digest.length ; i++)
				if(_digest[i] != sp._digest[i])
					return false;
		}
		
		return _epoch == sp._epoch &&
				_order == sp._order &&
				_address.equals(sp._address) &&
				_vAddr.equals(sp._vAddr) &&
				_lastDiscardedOrder == sp._lastDiscardedOrder &&
				_lastReceivedOrder == sp._lastReceivedOrder;
	}
	
	public static boolean equals(List<SequencePair> l1, List<SequencePair> l2) {
		if(l1.size() != l2.size())
			return false;
		
		for(SequencePair sp : l1)
			if(!l2.contains(sp))
				return false;

		return true;
	}

	public boolean isValid(byte[] digest) {
		if(_dsa == null)
			return false;
		
		if(digest.length != _digest.length)
			return false;
		
		for(int i=0 ; i<digest.length ; i++)
			if(_digest[i] != digest[i])
				return false;
		
		return true;
	}

	@Override
	public String toString() {
		return _address.getPort() + "*" + _vAddr.getPort() + "*" + _order + "*" + _lastReceivedOrder + "*" + _lastDiscardedOrder;
	}

	public String toString(BrokerIdentityManager idMan) {
		if(idMan == null)
			return toString();
		
		OverlayNodeId issuerNodeId = idMan.getBrokerId(_address.toString());
		String issuer = issuerNodeId == null ? null : issuerNodeId.getNodeIdentifier();
		if(issuer == null)
			return toString();
		
		OverlayNodeId verifierNodeId = idMan.getBrokerId(_vAddr.toString());
		String verifier = verifierNodeId == null ? null : verifierNodeId.getNodeIdentifier();
		if(verifier == null)
			return toString();
		
		return issuer + "*" + verifier + "*" + _order + "*" + _lastReceivedOrder + "*" + _lastDiscardedOrder; 
	}
	
//	public BFTVerifyResult checkSignature(IBFTDigestable digestable, IBFTIssuerProxy issuer, IBFTVerifier verifier) {
//		boolean result = verifyDSA(_dsa, digestable, issuer, verifier, _epoch, _order);
//		return result ? BFTVerifyResult.OK : BFTVerifyResult.FAIL;
//	}
//	
//	public BFTVerifyResult checkDigest(IBFTDigestable digestable) {
//		byte[] digest = digestable.getDigest();
//		if(digest.length != _digest.length)
//			return BFTVerifyResult.FAIL;
//		
//		for(int i=0 ; i<_digest.length ; i++)
//			if(_digest[i] != digest[i])
//				return BFTVerifyResult.FAIL;
//		
//		return BFTVerifyResult.OK;
//	}
//	
//	public BFTVerifyResult checkIssuer(IBFTIssuerProxy issuer) {
//		if(!issuer.getIssuerAddress().equals(getIssuerAddress()))
//			return BFTVerifyResult.FAIL;
//		
//		PublicKey pubkey = issuer.getPublicKey();
//		if(!DSAUtils.getInstance().verifyData(_data, _dsa, pubkey))
//			return BFTVerifyResult.FAIL;
//		else
//			return BFTVerifyResult.OK;
//	}
//	
//	public BFTVerifyResult checkVerifier(IBFTVerifier verifier) {
//		if(verifier.getVerifierAddress().equals(_verifierAddress))
//			return BFTVerifyResult.OK;
//		else
//			return BFTVerifyResult.FAIL;
//	}
	
	public boolean canValidateDigest() {
		return true;
	}
}
