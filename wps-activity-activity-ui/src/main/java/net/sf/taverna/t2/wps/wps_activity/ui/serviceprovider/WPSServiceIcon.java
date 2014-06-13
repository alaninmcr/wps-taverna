package net.sf.taverna.t2.wps.wps_activity.ui.serviceprovider;

import java.awt.Color;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import net.sf.taverna.t2.workbench.activityicons.ActivityIconSPI;
import net.sf.taverna.t2.workbench.ui.impl.configuration.colour.ColourManager;
import net.sf.taverna.t2.workflowmodel.processor.activity.Activity;
import net.sf.taverna.t2.wps.wps_activity.WPSActivity;

public class WPSServiceIcon implements ActivityIconSPI {
	
	 private static final String PROCESSOR_COLOUR_STRING = "#8EA4BF";

	static {
		    // set colour for XPath processors in the workflow diagram
		    ColourManager.getInstance().setPreferredColour(
		        WPSActivity.class.getCanonicalName(), Color.decode(PROCESSOR_COLOUR_STRING));
		  }


	private static Icon icon;

	public int canProvideIconScore(Activity<?> activity) {
		if (activity instanceof WPSActivity) {
			return DEFAULT_ICON;
		}
		return NO_ICON;
	}

	public Icon getIcon(Activity<?> activity) {
		return getIcon();
	}
	
	public static Icon getIcon() {
		if (icon == null) {
			icon = new ImageIcon(WPSServiceIcon.class.getResource("/ogc.png"));
		}
		return icon;
	}

}
