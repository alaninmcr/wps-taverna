package net.sf.taverna.t2.wps.wps_activity.ui.config;

import java.awt.Frame;
import java.awt.event.ActionEvent;

import net.sf.taverna.t2.workbench.ui.actions.activity.ActivityConfigurationAction;
import net.sf.taverna.t2.workbench.ui.views.contextualviews.activity.ActivityConfigurationDialog;

import net.sf.taverna.t2.wps.wps_activity.WPSActivity;
import net.sf.taverna.t2.wps.wps_activity.WPSActivityConfigurationBean;

@SuppressWarnings("serial")
public class WPSConfigureAction
		extends
		ActivityConfigurationAction<WPSActivity,
        WPSActivityConfigurationBean> {

	public WPSConfigureAction(WPSActivity activity, Frame owner) {
		super(activity);
	}

	@SuppressWarnings("unchecked")
	public void actionPerformed(ActionEvent e) {
		ActivityConfigurationDialog<WPSActivity, WPSActivityConfigurationBean> currentDialog = ActivityConfigurationAction
				.getDialog(getActivity());
		if (currentDialog != null) {
			currentDialog.toFront();
			return;
		}
		WPSConfigurationPanel panel = new WPSConfigurationPanel(
				getActivity());
		ActivityConfigurationDialog<WPSActivity,
        WPSActivityConfigurationBean> dialog = new ActivityConfigurationDialog<WPSActivity, WPSActivityConfigurationBean>(
				getActivity(), panel);

		ActivityConfigurationAction.setDialog(getActivity(), dialog);

	}

}
