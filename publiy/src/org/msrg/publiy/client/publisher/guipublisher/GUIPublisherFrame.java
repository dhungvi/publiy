package org.msrg.publiy.client.publisher.guipublisher;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

import org.msrg.publiy.utils.PropertyGrabber;

class GUIPublisherFrame extends JFrame {

	/**
	 * Auto Generated.
	 */
	private static final long serialVersionUID = -543878363503724418L;
	private static int MIN_GUI_UPDATE_INTERVAL = 100;
	private Timer _timer = new Timer("GUI_Publisher" + "-THREAD");
	
	private final Properties _arguments;
	private JLabel _lastPublishedLabel = new JLabel("NONE");
	private JButton _jButton_pause;
	private JTextField _jTextField_rate;
	private int _count;

	private final ISimpleGUIPublisher _publisher;
	
	protected GUIPublisherFrame(ISimpleGUIPublisher publisher, String id, Properties arguments) {
		super(id);
		_arguments = arguments;
		_publisher = publisher;
		
		_jTextField_rate = new JTextField();
		_jTextField_rate.setPreferredSize(new Dimension(30, 20));
		_jTextField_rate.setText("" + _publisher.getDelay());
		_jTextField_rate.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try{
					String input = _jTextField_rate.getText();
					int newDelay = new Integer(input).intValue();
					_publisher.setSpeedLevel(newDelay);
				}catch(Exception x) {
					_jTextField_rate.setText("" + _publisher.getDelay());
				}
			}
		});
		
		_jButton_pause = new JButton("Start");
		_jButton_pause.setEnabled(false);
		_jButton_pause.setPreferredSize(new Dimension(70, 20));
		refreshPauseButton();
		_jButton_pause.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				boolean paused = _publisher.isPaused();
				if(paused) {
//					_paused = false;
//					createTimer();
//					publishNext();
					_publisher.play();
				} else{
//					_paused = true;
//					cancelTimer();
					_publisher.pause();
				}
				refreshPauseButton();
			}
		});
		
		setLayout(new FlowLayout());
		add(new JLabel("Last published:"));
		add(_lastPublishedLabel);
		add(_jButton_pause);
		add(_jTextField_rate);
		doLayout();
	}
	
	void updateLabel(int count) {
		_lastPublishedLabel.setText("" + count);
	}
	
	public void refreshPauseButton() {
		if(_publisher.isPaused())
			_jButton_pause.setText("Start");
		else
			_jButton_pause.setText("Pause");
	}
	
	protected void initGUI() {
		pack();
		setLocation(getPreferredLocation());
		setAlwaysOnTop(true);
		setPreferredSize(getFramePreferredSize());
		setVisible(true);
		_timer.schedule(new GUIPublisherUpdateTask(), 0, MIN_GUI_UPDATE_INTERVAL);
	}

	protected Dimension getFramePreferredSize() {
		int width = PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_WIDTH, 30);
		int height = (Integer) PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_HEIGHT, 20);
		return new Dimension(width, height);
	}
	
	protected Point getPreferredLocation() {
		int x = PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_X, 0);
		int y = PropertyGrabber.getIntProperty(_arguments, PropertyGrabber.PROPERTY_GUI_Y, 0);
		return new Point(x, y);
	}

	public void enableButton(boolean b) {
		_jButton_pause.setEnabled(b);
	}
	
	private synchronized void updateGUI() {
		updateLabel(_count);
	}
	
	public void setPublishedCount(int count) {
		_count = count;
	}
	
	class GUIPublisherUpdateTask extends TimerTask{
		
		@Override
		public void run() {
			updateGUI();
		}
	}

	public void setSpeed(int interval) {
		_jTextField_rate.setText("" + interval);		
	}
}