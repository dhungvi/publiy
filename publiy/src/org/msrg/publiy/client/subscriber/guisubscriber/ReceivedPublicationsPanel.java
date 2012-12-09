package org.msrg.publiy.client.subscriber.guisubscriber;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.msrg.publiy.utils.SystemTime;

public class ReceivedPublicationsPanel extends JPanel {

	public final static int LATE_PUBLICATION_TIME = 2000;
	public final static int TOO_LATE_PUBLICATION_TIME = 4000;
	
	/**
	 * Auto Generated
	 */
	private static final long serialVersionUID = 5058162223259531432L;
	private String _publisherId;
	private JLabel _lastSubscriberLabel = new JLabel("NONE");
	private JButton _resetButton;
	private int _pubCount = -1;
	private int _receivedCountBeginning = -1;
	private int _receivedCount = 0;
	private long _lastGUIUpdate = 0;
	private static int MIN_GUI_UPDATE_INTERVAL = 100;
	private Timer _timer = new Timer("GUI_Publisher" + "-THREAD");
	
	
	ReceivedPublicationsPanel(String publisherId) {
		_resetButton = new JButton("Reset");
		_resetButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				_receivedCountBeginning = _receivedCount;
				updateLabel();
			}
		});
		_publisherId = publisherId;
		setLayout(new FlowLayout());
		add(_resetButton);
		add(new JLabel("Last received from '" + _publisherId + "': "));
		add(_lastSubscriberLabel);
	}
	
	String getPublisherId() {
		return _publisherId;
	}
	
	void updateLabel(int pubCount, int receivedCount, long lastReceivedPublicationTime) {
		_pubCount = pubCount;
		_receivedCount = receivedCount;
		_lastReceivedPublicationTime = lastReceivedPublicationTime;

		updateLabel();
	}
	
	protected long _lastReceivedPublicationTime = 0;
	
	void updateLabel() {
		long currTime = SystemTime.currentTimeMillis();
		if(currTime - _lastGUIUpdate > MIN_GUI_UPDATE_INTERVAL) {
			long timeDiffSinceLastPublication = currTime - _lastReceivedPublicationTime;
			if((currTime/300) % 3 == 4) // i.e., never!
				_lastSubscriberLabel.setForeground(Color.BLACK);
			else if(timeDiffSinceLastPublication <= LATE_PUBLICATION_TIME)
				_lastSubscriberLabel.setForeground(Color.BLACK);
			else if(timeDiffSinceLastPublication >= TOO_LATE_PUBLICATION_TIME)
					_lastSubscriberLabel.setForeground(Color.RED);
			else 
				_lastSubscriberLabel.setForeground(Color.ORANGE);
			
			int receivedCount = _receivedCount - _receivedCountBeginning;
			_lastSubscriberLabel.setText("[" + receivedCount + "]");// + (receivedCount==_pubCount?"=":(receivedCount>_pubCount?">":"<")) + _pubCount);
			
			_timer.schedule(new GUISubscriberUpdateTask(), MIN_GUI_UPDATE_INTERVAL);
			_lastGUIUpdate = currTime;
		}

	}
	
	class GUISubscriberUpdateTask extends TimerTask{
		@Override
		public void run() {
			updateLabel();
		}
	}

}
