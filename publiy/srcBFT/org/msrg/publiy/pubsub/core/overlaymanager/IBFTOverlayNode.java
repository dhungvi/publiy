package org.msrg.publiy.pubsub.core.overlaymanager;

import org.msrg.publiy.broker.IBFTSuspectable;
import org.msrg.publiy.broker.core.sequence.IBFTDackIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTDackVerifier;
import org.msrg.publiy.broker.core.sequence.IBFTIssuerProxy;
import org.msrg.publiy.broker.core.sequence.IBFTVerifierProxy;

public interface IBFTOverlayNode extends IBFTIssuerProxy, IBFTVerifierProxy, IBFTSuspectable, IBFTDackIssuerProxy, IBFTDackVerifier { }
