package net.sf.taverna.t2.wps.wps_activity.ui.menu;

import javax.swing.Action;

import net.sf.taverna.t2.workbench.activitytools.AbstractConfigureActivityMenuAction;
import net.sf.taverna.t2.wps.wps_activity.WPSActivity;
import net.sf.taverna.t2.wps.wps_activity.ui.config.WPSConfigureAction;

public class WPSConfigureMenuAction extends
		AbstractConfigureActivityMenuAction<WPSActivity> {

	public WPSConfigureMenuAction() {
		super(WPSActivity.class);
	}

	@Override
	protected Action createAction() {
		WPSActivity a = findActivity();
		Action result = null;
		result = new WPSConfigureAction(findActivity(),
				getParentFrame());
		result.putValue(Action.NAME, "Configure example service");
		addMenuDots(result);
		return result;
	}

}
