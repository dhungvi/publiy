package org.msrg.publiy.broker.controller;

import java.net.InetSocketAddress;

import org.msrg.publiy.utils.Utility;

import org.msrg.publiy.broker.internaltimer.BrokerInternalTimer;

public class BC extends BrokerController {

	public BC(InetSocketAddress brokerLontrollerLocalAddress,
			InetSocketAddress brokerLocalAddress) {
		super(brokerLontrollerLocalAddress, brokerLocalAddress);
	}
	
	public static void main(String[] argv) {
		BrokerInternalTimer.inform("Starting broker controller...");
		if ( argv.length != 1 )
			System.exit(-1);
		InetSocketAddress brokerControllerAddress;
		InetSocketAddress brokerAddress;

		try{
			brokerAddress = Utility.stringToInetSocketAddress(argv[0]);
			brokerControllerAddress = BrokerController.getDefaultBrokerControllerAddressForBrokerAddress(brokerAddress);
		}catch(Exception x) {
			int brokerNumber = new Integer(argv[0]).intValue();
			brokerAddress = BrokerController.BROKER_BROKER_CONTROLLER_ADDRESSES[brokerNumber][1]; 
			brokerControllerAddress = BrokerController.BROKER_BROKER_CONTROLLER_ADDRESSES[brokerNumber][0];
		}
		
		BC bc1 = new BC(brokerControllerAddress, brokerAddress);
		bc1.prepareAndStart();
	}
}
