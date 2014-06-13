package net.sf.taverna.t2.wps.wps_activity.ui.view;

import java.util.Arrays;
import java.util.List;

import net.sf.taverna.t2.workbench.ui.views.contextualviews.ContextualView;
import net.sf.taverna.t2.workbench.ui.views.contextualviews.activity.ContextualViewFactory;

import net.sf.taverna.t2.wps.wps_activity.WPSActivity;

public class WPSActivityContextViewFactory implements
		ContextualViewFactory<WPSActivity> {

	public boolean canHandle(Object selection) {
		return selection instanceof WPSActivity;
	}

	public List<ContextualView> getViews(WPSActivity selection) {
		return Arrays.<ContextualView>asList(new WPSContextualView(selection));
	}
	
}
