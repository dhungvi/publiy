package org.msrg.publiy.broker.core.sequence;

import java.net.InetSocketAddress;

public class DummyLocalSequencer extends LocalSequencer {

	public DummyLocalSequencer(InetSocketAddress addr) {
		super(addr);
	}

	public void amIinitialized() {
		return;
	}
}
