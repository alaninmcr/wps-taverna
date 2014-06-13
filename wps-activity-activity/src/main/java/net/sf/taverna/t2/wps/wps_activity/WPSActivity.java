package net.sf.taverna.t2.wps.wps_activity;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;

import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.client.WPSClientException;
import org.n52.wps.client.WPSClientSession;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;

public class WPSActivity extends
		AbstractAsynchronousActivity<WPSActivityConfigurationBean>
		implements AsynchronousActivity<WPSActivityConfigurationBean> {

	private static Logger logger = Logger.getLogger(WPSActivity.class);
	
	private WPSActivityConfigurationBean configBean;
	
	private ProcessDescriptionType processDescription;

	@Override
	public void configure(WPSActivityConfigurationBean configBean)
			throws ActivityConfigurationException {

		// Store for getConfiguration(), but you could also make
		// getConfiguration() return a new bean from other sources
		this.configBean = configBean;
		
		removeInputs();
		removeOutputs();

        WPSClientSession wpsClient = WPSClientSession.getInstance();
        
		try {
			processDescription = wpsClient
			                .getProcessDescription(configBean.getUri().toString(), configBean.getProcessId());
		} catch (IOException e) {
			logger.error("Unable to read processDescription", e);
			throw new ActivityConfigurationException("Unable to read processDescription", e);
		}
		              
        InputDescriptionType[] inputList = processDescription.getDataInputs()
                        .getInputArray();
                        
        for (InputDescriptionType a : inputList) {
        	int minOccurs = a.getMinOccurs().intValue();
        	int maxOccurs = a.getMaxOccurs().intValue();
			addInput(a.getIdentifier().getStringValue(),
					(minOccurs == 0) || (maxOccurs > 1) ? 1 : 0,
					true,
					Collections.EMPTY_LIST,
					String.class);
        }
		
        OutputDescriptionType[] outputList = processDescription.getProcessOutputs().getOutputArray();
        for (OutputDescriptionType output : outputList) {
        	
        	addOutput(output.getIdentifier().getStringValue(),
        			0,
        			0);
        }
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
                String inputValueString = (String) inputValue;
				if (input.getLiteralData() != null) {
                        if (inputValue instanceof String) {
                                executeBuilder.addLiteralData(inputName,
                                                inputValueString);
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
                                                                inputValueString,
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
	                		referenceService.register("Placeholder" + response.toString(), 0, true, callback.getContext()));
    			}
    			callback.receiveResult(outputs, new int[0]);
        } else if (responseObject instanceof ExceptionReportDocument) {
        	ExceptionReportDocument response = (ExceptionReportDocument) responseObject;
        	ExceptionReport report = response.getExceptionReport();
        	logger.error(report.toString());
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
