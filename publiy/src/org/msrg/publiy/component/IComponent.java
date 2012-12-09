package org.msrg.publiy.component;

public interface IComponent {
	
	public String getComponentName();
	public ComponentStatus getComponentState();
	
	public void addNewComponentListener(IComponentListener comListener);
	public void removeComponentListener(IComponentListener comListener);
	
	public void awakeFromPause();
	public void stopComponent();
	public void pauseComponent();
	public void startComponent();
	
	public void prepareToStart();
}
