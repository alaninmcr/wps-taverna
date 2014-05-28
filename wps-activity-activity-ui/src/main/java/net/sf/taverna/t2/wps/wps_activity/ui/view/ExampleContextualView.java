package net.sf.taverna.t2.wps.wps_activity.ui.view;

import java.awt.Frame;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.taverna.t2.workbench.ui.views.contextualviews.ContextualView;

import net.sf.taverna.t2.wps.wps_activity.WPSActivity;
import net.sf.taverna.t2.wps.wps_activity.WPSActivityConfigurationBean;
import net.sf.taverna.t2.wps.wps_activity.ui.config.WPSConfigureAction;

@SuppressWarnings("serial")
public class ExampleContextualView extends ContextualView {
	private final WPSActivity activity;
	private JLabel description = new JLabel("ads");

	public ExampleContextualView(WPSActivity activity) {
		this.activity = activity;
		initView();
	}

	@Override
	public JComponent getMainFrame() {
		JPanel jPanel = new JPanel();
		jPanel.add(description);
		refreshView();
		return jPanel;
	}

	@Override
	public String getViewTitle() {
		WPSActivityConfigurationBean configuration = activity
				.getConfiguration();
		return "Example service " + configuration.getProcessId();
	}

	/**
	 * Typically called when the activity configuration has changed.
	 */
	@Override
	public void refreshView() {
		WPSActivityConfigurationBean configuration = activity
				.getConfiguration();
		description.setText("Example service " + configuration.getUri()
				+ " - " + configuration.getProcessId());
		// TODO: Might also show extra service information looked
		// up dynamically from endpoint/registry
	}

	/**
	 * View position hint
	 */
	@Override
	public int getPreferredPosition() {
		// We want to be on top
		return 100;
	}
	
	@Override
	public Action getConfigureAction(final Frame owner) {
		return new WPSConfigureAction(activity, owner);
	}

}
