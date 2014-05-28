package net.sf.taverna.t2.wps.wps_activity.ui.serviceprovider;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.swing.Icon;

import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.sf.taverna.t2.servicedescriptions.ServiceDescription;
import net.sf.taverna.t2.workflowmodel.processor.activity.Activity;
import net.sf.taverna.t2.wps.wps_activity.WPSActivity;
import net.sf.taverna.t2.wps.wps_activity.WPSActivityConfigurationBean;
import net.sf.taverna.t2.wps.wps_activity.WPSActivityInputPortDefinitionBean;
import net.sf.taverna.t2.wps.wps_activity.WPSActivityOutputPortDefinitionBean;

import org.apache.log4j.Logger;
import org.n52.wps.client.WPSClientSession;

public class WPSServiceDesc extends ServiceDescription<WPSActivityConfigurationBean> {
	
	private static Logger logger = Logger.getLogger(WPSServiceDesc.class);
	
	private URI uri;
	private String processId;

	/**
	 * The subclass of Activity which should be instantiated when adding a service
	 * for this description 
	 */
	@Override
	public Class<? extends Activity<WPSActivityConfigurationBean>> getActivityClass() {
		return WPSActivity.class;
	}

	/**
	 * The configuration bean which is to be used for configuring the instantiated activity.
	 * Making this bean will typically require some of the fields set on this service
	 * description, like an endpoint URL or method name. 
	 * 
	 */
	@Override
	public WPSActivityConfigurationBean getActivityConfiguration() {
		WPSActivityConfigurationBean bean = new WPSActivityConfigurationBean();
		bean.setUri(this.getUri());
		bean.setProcessId(this.getProcessId());
        WPSClientSession wpsClient = WPSClientSession.getInstance();
        
        ProcessDescriptionType processDescription;
		try {
			processDescription = wpsClient
			                .getProcessDescription(this.getUri().toString(), this.getProcessId());
		} catch (IOException e) {
			logger.error("Unable to read processDescription", e);
			return null;
		}
		
		List<WPSActivityInputPortDefinitionBean> inputPortDefs = new ArrayList<WPSActivityInputPortDefinitionBean>();                
        InputDescriptionType[] inputList = processDescription.getDataInputs()
                        .getInputArray();
                        
        for (InputDescriptionType a : inputList) {
        	WPSActivityInputPortDefinitionBean newInput = 
        			new WPSActivityInputPortDefinitionBean(a.getIdentifier().getStringValue(),
        					a.getMinOccurs().intValue(), a.getMaxOccurs().intValue());

            inputPortDefs.add(newInput);
        }
        bean.setWPSInputPortDefinitions(inputPortDefs);
		
        List<WPSActivityOutputPortDefinitionBean> outputPortDefs = new ArrayList<WPSActivityOutputPortDefinitionBean>();
        OutputDescriptionType[] outputList = processDescription.getProcessOutputs().getOutputArray();
        for (OutputDescriptionType output : outputList) {
        	WPSActivityOutputPortDefinitionBean newOutput =
        			new WPSActivityOutputPortDefinitionBean(output.getIdentifier().getStringValue());
        	newOutput.setDepth(0);
        	outputPortDefs.add(newOutput);
        }
        bean.setWPSOutputPortDefinitions(outputPortDefs);
        
		return bean;
	}

	/**
	 * An icon to represent this service description in the service palette.
	 */
	@Override
	public Icon getIcon() {
		return WPSServiceIcon.getIcon();
	}

	/**
	 * The display name that will be shown in service palette and will
	 * be used as a template for processor name when added to workflow.
	 */
	@Override
	public String getName() {
		return getLastProcessIdPart();
	}

	/**
	 * The path to this service description in the service palette. Folders
	 * will be created for each element of the returned path.
	 */
	@Override
	public List<String> getPath() {
		// For deeper paths you may return several strings
		ArrayList<String> result = new ArrayList<String>();
		result.add("WPS: " + this.getUri().toString());
		result.addAll(getProcessPath());
		return result;
	}

	/**
	 * Return a list of data values uniquely identifying this service
	 * description (to avoid duplicates). Include only primary key like fields,
	 * ie. ignore descriptions, icons, etc.
	 */
	@Override
	protected List<? extends Object> getIdentifyingData() {
		return Arrays.<Object>asList(this.getUri(), this.getProcessId());
	}

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

	private String getLastProcessIdPart() {
		if (this.processId != null) {
			return processId.substring(processId.lastIndexOf(".") + 1);
		}
		return "";
	}
	
	private List<String> getProcessPath() {
		if (this.processId != null) {
			return Arrays.asList(processId.substring(0, processId.lastIndexOf(".")).split("\\."));
		}
		return Collections.emptyList();
	}
}
