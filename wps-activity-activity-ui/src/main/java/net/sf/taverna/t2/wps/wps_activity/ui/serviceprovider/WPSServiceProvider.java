package net.sf.taverna.t2.wps.wps_activity.ui.serviceprovider;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Icon;

import net.opengis.ows.x11.LanguageStringType;
import net.opengis.wps.x100.CapabilitiesDocument;
import net.opengis.wps.x100.ProcessBriefType;
import net.opengis.wps.x100.WSDLDocument.WSDL;
import net.sf.taverna.t2.activities.wsdl.servicedescriptions.WSDLServiceProvider;
import net.sf.taverna.t2.activities.wsdl.servicedescriptions.WSDLServiceProviderConfig;
import net.sf.taverna.t2.servicedescriptions.AbstractConfigurableServiceProvider;
import net.sf.taverna.t2.servicedescriptions.ConfigurableServiceProvider;
import net.sf.taverna.t2.servicedescriptions.ServiceDescription;
import net.sf.taverna.t2.workflowmodel.ConfigurationException;

import org.apache.log4j.Logger;
import org.n52.wps.client.WPSClientException;
import org.n52.wps.client.WPSClientSession;

public class WPSServiceProvider extends
	AbstractConfigurableServiceProvider<WPSServiceProviderConfig> implements
	ConfigurableServiceProvider<WPSServiceProviderConfig> {
	
	private static Logger logger = Logger.getLogger(WPSServiceProvider.class);
	
	public WPSServiceProvider() {
		super(new WPSServiceProviderConfig());
	}

	private static final URI providerId = URI
		.create("http://example.com/2011/service-provider/wps-activity");
	
	/**
	 * Do the actual search for services. Return using the callBack parameter.
	 */
	@SuppressWarnings("unchecked")
	public void findServiceDescriptionsAsync(
			FindServiceDescriptionsCallBack callBack) {
		// Use callback.status() for long-running searches
		// callBack.status("Resolving example services");
		
		String urlString = this.getConfiguration().getUri().toString();
		List<ServiceDescription> results = new ArrayList<ServiceDescription>();
				
		WPSClientSession wpsClient = WPSClientSession.getInstance();
		try {

			wpsClient.connect(urlString);
		} catch (WPSClientException e) {
			callBack.fail(e.getMessage(), e);
			return;
		}

		CapabilitiesDocument capabilities = wpsClient.getWPSCaps(urlString);
		
//		WSDL wsdl = capabilities.getCapabilities().getWSDL();
//		if (wsdl != null) {
//			String wsdl_location = wsdl.getHref();
//			WSDLServiceProviderConfig proxyConfig = new WSDLServiceProviderConfig(wsdl_location);
//			WSDLServiceProvider proxyProvider = new WSDLServiceProvider();
//			try {
//				proxyProvider.configure(proxyConfig);
//				proxyProvider.findServiceDescriptionsAsync(callBack);
//				return;
//			} catch (ConfigurationException e) {
//				logger.error(e);
//			}			
//		}
        
        ProcessBriefType[] processList = capabilities.getCapabilities()
                        .getProcessOfferings().getProcessArray();
                        
        for (ProcessBriefType process : processList) {
        	WPSServiceDesc service = new WPSServiceDesc();
			// Populate the service description bean
			service.setProcessId(process.getIdentifier().getStringValue());
			service.setUri(this.getConfiguration().getUri());
			LanguageStringType title = process.getTitle();
			String stringValue = title.getStringValue();
			service.setDescription(stringValue);

			results.add(service);
        }
        

		// partialResults() can also be called several times from inside
		// for-loop if the full search takes a long time
		callBack.partialResults(results);

		// No more results will be coming
		callBack.finished();
	}

	/**
	 * Icon for service provider
	 */
	public Icon getIcon() {
		return WPSServiceIcon.getIcon();
	}

	/**
	 * Name of service provider, appears in right click for 'Remove service
	 * provider'
	 */
	public String getName() {
		return "WPS service";
	}
	
	@Override
	public String toString() {
		return getName();
	}
	
	public String getId() {
		return providerId.toASCIIString();
	}

	@Override
	protected List<? extends Object> getIdentifyingData() {
		return Arrays.asList(getConfiguration().getUri());
	}

}
