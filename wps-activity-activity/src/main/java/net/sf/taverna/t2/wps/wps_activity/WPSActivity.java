package net.sf.taverna.t2.wps.wps_activity;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.opengis.ows.x11.ExceptionReportDocument;
import net.opengis.ows.x11.ExceptionReportDocument.ExceptionReport;
import net.opengis.ows.x11.ExceptionType;
import net.opengis.wps.x100.ComplexDataType;
import net.opengis.wps.x100.DataType;
import net.opengis.wps.x100.DocumentOutputDefinitionType;
import net.opengis.wps.x100.ExecuteDocument;
import net.opengis.wps.x100.ExecuteResponseDocument;
import net.opengis.wps.x100.InputDescriptionType;
import net.opengis.wps.x100.LiteralDataType;
import net.opengis.wps.x100.OutputDataType;
import net.opengis.wps.x100.OutputDefinitionType;
import net.opengis.wps.x100.OutputDescriptionType;
import net.opengis.wps.x100.OutputReferenceType;
import net.opengis.wps.x100.ProcessDescriptionType;
import net.sf.taverna.t2.invocation.InvocationContext;
import net.sf.taverna.t2.reference.ExternalReferenceSPI;
import net.sf.taverna.t2.reference.ReferenceService;
import net.sf.taverna.t2.reference.T2Reference;
import net.sf.taverna.t2.workflowmodel.processor.activity.AbstractAsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivity;
import net.sf.taverna.t2.workflowmodel.processor.activity.AsynchronousActivityCallback;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.geotools.feature.FeatureCollection;
import org.n52.wps.client.ExecuteResponseAnalyser;
import org.n52.wps.client.StaticDataHandlerRepository;
import org.n52.wps.client.WPSClientException;
import org.n52.wps.client.WPSClientSession;
import org.n52.wps.io.IParser;
import org.n52.wps.io.data.IData;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.GenericFileDataBinding;
import org.n52.wps.io.datahandler.parser.GenericFileParser;

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
                
                patchExecute(execute);

                try {
                	OutputDataType[] processOutputs = response.getExecuteResponse().getProcessOutputs().getOutputArray();
    			for(OutputDataType processOutput : processOutputs) {
    				String outputName = processOutput.getIdentifier().getStringValue();

    				if (processOutput.isSetData()) {
    					DataType dataType = processOutput.getData();
    					if (dataType.isSetLiteralData()) {
    						LiteralDataType ldt = dataType.getLiteralData();
    		                outputs.put(outputName,
    		                		referenceService.register(ldt.getStringValue(), 0, true, callback.getContext()));
    		                continue;
    		             }
    					else if (dataType.isSetComplexData()) {
    						IData d = extractData(response, outputName);
    						Object o = d.getPayload();
    		                outputs.put(outputName,
    		                		referenceService.register(o.toString(), 0, true, callback.getContext()));
    		                continue;
    					}
    				}
    				outputs.put(outputName,
	                		referenceService.register("Placeholder" + response.toString(), 0, true, callback.getContext()));
    			}
				} catch (WPSClientException e) {
					logger.error(e);
					callback.fail(e.getMessage());
				}
    			callback.receiveResult(outputs, new int[0]);
        } else if (responseObject instanceof ExceptionReportDocument) {
        	ExceptionReportDocument response = (ExceptionReportDocument) responseObject;
        	ExceptionReport report = response.getExceptionReport();
        	ExceptionType[] exceptions = report.getExceptionArray();
        	
        	List<String> all_texts = new ArrayList<String>();
        	for (ExceptionType ex : exceptions) {
        		if (ex.getExceptionCode().equals("JAVA_StackTrace")) {
        			continue;
        		}
        		String[] texts = ex.getExceptionTextArray();
        		all_texts.addAll(Arrays.asList(texts));
        	}
        	String complete_text = StringUtils.join(all_texts, "\n");
        	logger.error(report.toString());
        	callback.fail(complete_text);
        }

		
			}
		});
	}
	
	private ExternalReferenceSPI extractDataReference (ExecuteResponseDocument responseDocument, String outputID) throws WPSClientException {
	IParser parser = new GenericFileParser();
	InputStream is = null;
	String mimeType = null;
	String encoding = null;
	String schema = null;
		OutputDataType[] processOutputs = responseDocument.getExecuteResponse().getProcessOutputs().getOutputArray();
		for(OutputDataType processOutput : processOutputs){
			if(processOutput.getIdentifier().getStringValue().equalsIgnoreCase(outputID)){
				if(processOutput.isSetReference()){
					//request the reference
					OutputReferenceType reference = processOutput.getReference();
					mimeType = reference.getMimeType();
					encoding = reference.getEncoding();
					schema = reference.getSchema();
					String urlString = reference.getHref();
					URL url;
					try {
						url = new URL(urlString);
						is = url.openStream();
					} catch (MalformedURLException e) {
						throw new WPSClientException("Could not fetch response from referenced URL", e);
					} catch (IOException e) {
						throw new WPSClientException("Could not fetch response from referenced URL", e);
					}
					
				}else{
					ComplexDataType complexData = processOutput.getData().getComplexData();
					mimeType = complexData.getMimeType();
					encoding = complexData.getEncoding();
					schema = complexData.getSchema();
					is = complexData.newInputStream();
				}
				
			}
		}
		
		if (is == null) {
			return null;
		}
		if("base64".equalsIgnoreCase(encoding) && "text/plain".equals(mimeType)){
			String result = IOUtils.toString(is);
			is.close();
			return new 
			return parser.parseBase64(is, mimeType, schema);
		}else{
			return parser.parse(is, mimeType, schema);
		}
	}
	
	protected void patchExecute(ExecuteDocument execute) {
		String encoding;
		if(execute.getExecute().isSetResponseForm() && execute.getExecute().getResponseForm().isSetRawDataOutput()){
			// get data specification from request
			OutputDefinitionType rawDataOutput = execute.getExecute().getResponseForm().getRawDataOutput();
			encoding = rawDataOutput.getEncoding();
			if (encoding == null) {
				rawDataOutput.setEncoding("UTF-8");
			}
		}else if(execute.getExecute().isSetResponseForm() && execute.getExecute().getResponseForm().isSetResponseDocument()){
			DocumentOutputDefinitionType[] outputs = execute.getExecute().getResponseForm().getResponseDocument().getOutputArray();
			for(DocumentOutputDefinitionType output : outputs){
					encoding = output.getEncoding();
					if (encoding == null) {
						output.setEncoding("UTF-8");
					}
			}
			
		}
	}


	@Override
	public WPSActivityConfigurationBean getConfiguration() {
		return this.configBean;
	}

}
