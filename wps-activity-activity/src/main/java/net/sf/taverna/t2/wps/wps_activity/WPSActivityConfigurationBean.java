package net.sf.taverna.t2.wps.wps_activity;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import net.sf.taverna.t2.workflowmodel.processor.activity.config.ActivityInputPortDefinitionBean;
import net.sf.taverna.t2.workflowmodel.processor.activity.config.ActivityOutputPortDefinitionBean;
import net.sf.taverna.t2.workflowmodel.processor.activity.config.ActivityPortsDefinitionBean;

/**
 * Example activity configuration bean.
 * 
 */
public class WPSActivityConfigurationBean extends ActivityPortsDefinitionBean implements Serializable {
	
	private URI uri;
	private String processId;
	/**
	 * @return the uri
	 */
	public final URI getUri() {
		return uri;
	}
	/**
	 * @param uri the uri to set
	 */
	public final void setUri(URI uri) {
		this.uri = uri;
	}
	/**
	 * @return the processId
	 */
	public final String getProcessId() {
		return processId;
	}
	/**
	 * @param processId the processId to set
	 */
	public final void setProcessId(String processId) {
		this.processId = processId;
	}
}
