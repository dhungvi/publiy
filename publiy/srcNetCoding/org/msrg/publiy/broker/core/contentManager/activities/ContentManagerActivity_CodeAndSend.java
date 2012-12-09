package org.msrg.publiy.broker.core.contentManager.activities;

import java.net.InetSocketAddress;

import org.msrg.publiy.broker.core.contentManager.Content;
import org.msrg.publiy.broker.core.contentManager.IContentListener;

public class ContentManagerActivity_CodeAndSend extends ContentManagerActivity {
	
	public final Content _content;
	public final InetSocketAddress _remote;
	
	public ContentManagerActivity_CodeAndSend(IContentListener contentListener, Content content, InetSocketAddress remote) {
		super(ContentManagerActivityTypes.CONTENTMANAGER_ACTIVITY_ENCODE_AND_SEND, contentListener);
		
		_content = content;
		_remote = remote;
	}
	
	@Override
	public String toString() {
		return super.toString() + "_SEQ[" + _content.getSourceSequence() + "]_Remote[" + _remote.getPort() + "]";
	}
}
