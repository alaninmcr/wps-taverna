package net.sf.taverna.t2.wps.wps_activity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.feature.FeatureCollection;
import org.n52.wps.client.ExecuteResponseAnalyser;
import org.n52.wps.client.WPSClientException;
import org.n52.wps.client.WPSClientSession;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;

import net.opengis.ows.x11.ExceptionReportDocument;
import net.opengis.ows.x11.ExceptionReportDocument.ExceptionReport;
import net.opengis.wps.x100.DataType;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralDataType;
import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.sf.taverna.t2.invocation.InvocationContext;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.processor.activity.AbstractAsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityOutputPort;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;

public class WPSActivity extends
		AbstractAsynchronousActivity<WPSActivityConfigurationBean>
		implements AsynchronousActivity<WPSActivityConfigurationBean> {

	
	private WPSActivityConfigurationBean configBean;

	@Override
	public void configure(WPSActivityConfigurationBean configBean)
			throws ActivityConfigurationException {

		// Store for getConfiguration(), but you could also make
		// getConfiguration() return a new bean from other sources
		this.configBean = configBean;

		super.configurePorts(configBean);
	}

	
	@Override
	public void executeAsynch(final Map<String, T2Reference> inputRefs,
			final AsynchronousActivityCallback callback) {
		// Don't execute service directly now, request to be run ask to be run
		// from thread pool and return asynchronously
		callback.requestRun(new Runnable() {
			
			public void run() {
				InvocationContext context = callback
						.getContext();
				ReferenceService referenceService = context
						.getReferenceService();
				
		        WPSClientSession wpsClient = WPSClientSession.getInstance();
				ProcessDescriptionType processDescription;
				try {
					processDescription = wpsClient
					        .getProcessDescription(WPSActivity.this.configBean.getUri().toString(),
					        		WPSActivity.this.configBean.getProcessId());
				} catch (IOException e1) {
					callback.fail("Unable to create client", e1);
					return;
				}
				
				org.n52.wps.client.ExecuteRequestBuilder executeBuilder = new org.n52.wps.client.ExecuteRequestBuilder(
                        processDescription);

        for (InputDescriptionType input : processDescription.getDataInputs()
                        .getInputArray()) {
                String inputName = input.getIdentifier().getStringValue();
        
                T2Reference boundReference = inputRefs.get(inputName);
                Object inputValue = null;
                if (boundReference != null) {
                	inputValue = referenceService
                        .renderIdentifier(boundReference, String.class,
                                callback.getContext());
                } else {
                	inputValue = "missing";
                }
                if (input.getLiteralData() != null) {
                        if (inputValue instanceof String) {
                                executeBuilder.addLiteralData(inputName,
                                                (String) inputValue);
                        }
                } else if (input.getBoundingBoxData() != null) {
// TODO what ?
                } else if (input.getComplexData() != null) {
                        // Complexdata by value
                        if (inputValue instanceof FeatureCollection) {
                                IData data = new GTVectorDataBinding(
                                                (FeatureCollection) inputValue);
                                try {
									executeBuilder
									                .addComplexData(
									                                inputName,
									                                data,
									                                "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
									                                "UTF-8", "text/xml");
								} catch (WPSClientException e) {
									callback.fail("", e);
									return;
								}
                        }
                        // Complexdata Reference
                        if (inputValue instanceof String) {
                                executeBuilder
                                                .addComplexDataReference(
                                                                inputName,
                                                                (String) inputValue,
                                                                "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
                                                                null, "text/xml");
                        }

                        if (inputValue == null && input.getMinOccurs().intValue() > 0) {
                        	callback.fail("Property not set, but mandatory: "
                                                + inputName);
                        	return;
                        }
                }
        }
//        executeBuilder.setMimeTypeForOutput("text/xml", "result");
//        executeBuilder.setSchemaForOutput(
//                        "http://schemas.opengis.net/gml/3.1.1/base/feature.xsd",
//                        "result");
        ExecuteDocument execute = executeBuilder.getExecute();
        execute.getExecute().setService("WPS");

        Object responseObject = null;
		try {
			responseObject = wpsClient.execute(WPSActivity.this.configBean.getUri().toString(), execute);
		} catch (WPSClientException e) {
			callback.fail("Unable to execute", e);
			return;
		}
		
		Map<String, T2Reference> outputs = new HashMap<String, T2Reference>();
        if (responseObject instanceof ExecuteResponseDocument) {
                ExecuteResponseDocument response = (ExecuteResponseDocument) responseObject;
    			OutputDataType[] processOutputs = response.getExecuteResponse().getProcessOutputs().getOutputArray();
    			for(OutputDataType processOutput : processOutputs) {
    				String outputName = processOutput.getIdentifier().getStringValue();
    				System.err.println(outputName);
    				if (processOutput.isSetData()) {
    					DataType dataType = processOutput.getData();
    					if (dataType.isSetLiteralData()) {
    						LiteralDataType ldt = dataType.getLiteralData();
    		                outputs.put(outputName,
    		                		referenceService.register(ldt.getStringValue(), 0, true, callback.getContext()));
    		                continue;
    		             }
    				}
    				outputs.put(outputName,
	                		referenceService.register("missing", 0, true, callback.getContext()));
    			}
    			callback.receiveResult(outputs, new int[0]);
        } else if (responseObject instanceof ExceptionReportDocument) {
        	ExceptionReportDocument response = (ExceptionReportDocument) responseObject;
        	ExceptionReport report = response.getExceptionReport();
        	callback.fail(report.toString());
        }

		
			}
		});
	}

	@Override
	public WPSActivityConfigurationBean getConfiguration() {
		return this.configBean;
	}

}