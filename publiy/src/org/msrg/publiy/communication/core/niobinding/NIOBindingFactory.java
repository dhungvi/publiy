package org.msrg.publiy.communication.core.niobinding;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.HashMap;

import org.msrg.publiy.broker.IBrokerShadow;


public class NIOBindingFactory {
	
	public static int SELECT_TIMEOUT = 1;
	public static int SELECTOR_BLOCKING_PROBLEM = 100;
	
	private static HashMap<InetSocketAddress, INIOBinding> _nioBindings =
		new HashMap<InetSocketAddress, INIOBinding>();
	private static final Object _LOCK = new Object();
	
	private NIOBindingFactory() {}
	
	public static boolean destroyNIOBinding(InetSocketAddress localAddress) {
		return _nioBindings.remove(localAddress) != null;
	}
	
	public static INIOBinding getNIOBinding(IBrokerShadow brokerShadow) throws IOException{
		InetSocketAddress ccInetAddress = brokerShadow.getLocalAddress();
		
		synchronized(_LOCK) {
			INIOBinding nioBinding = _nioBindings.get(ccInetAddress);
			if(nioBinding == null) {
				nioBinding = new NIOBindingImp_SelectorBugWorkaround(brokerShadow);
				registerNIOBindingPrivatedly(nioBinding, brokerShadow);
			}
			return nioBinding;
		}
	}

	public static INIOBinding registerNIOBinding(
			INIOBinding nioBinding, IBrokerShadow brokerShadow) {
		synchronized(_LOCK) {
			return registerNIOBindingPrivatedly(nioBinding, brokerShadow);
		}
	}
	
	public static INIOBinding registerNIOBindingPrivatedly(
			INIOBinding nioBinding, IBrokerShadow brokerShadow) {
		InetSocketAddress ccInetAddress = brokerShadow.getLocalAddress();
		if(_nioBindings.get(ccInetAddress) != null)
			throw new IllegalStateException();
		_nioBindings.put(ccInetAddress, nioBinding);
		
		return nioBinding;
	}
}
