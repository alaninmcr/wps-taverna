package net.sf.taverna.t2.wps.wps_activity.ui.serviceprovider;

import java.net.URI;

import net.sf.taverna.t2.lang.beans.PropertyAnnotated;

public class WPSServiceProviderConfig extends PropertyAnnotated {
	
	private URI uri = URI.create("http://example.com");

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

}
