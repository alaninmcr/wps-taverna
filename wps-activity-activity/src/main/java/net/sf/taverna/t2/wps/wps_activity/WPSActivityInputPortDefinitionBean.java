package net.sf.taverna.t2.wps.wps_activity;

import net.sf.taverna.t2.workflowmodel.processor.activity.config.ActivityInputPortDefinitionBean;

public class WPSActivityInputPortDefinitionBean extends
		ActivityInputPortDefinitionBean {

	public WPSActivityInputPortDefinitionBean(final String name, int minOccurs, int maxOccurs) {
		super();
		super.setName(name);
	    super.setAllowsLiteralValues(true);
	    super.setTranslatedElementType(String.class);
        super.setDepth((minOccurs == 0) || (maxOccurs > 1) ? 1 : 0);
	}

}