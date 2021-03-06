package net.sf.taverna.t2.wps.wps_activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.sf.taverna.t2.activities.testutils.ActivityInvoker;
import net.sf.taverna.t2.workflowmodel.OutputPort;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityConfigurationException;
import net.sf.taverna.t2.workflowmodel.processor.activity.ActivityInputPort;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class ExampleActivityTest {

	private WPSActivityConfigurationBean configBean;

	private WPSActivity activity = new WPSActivity();

	@Before
	@Ignore
	public void makeConfigBean() throws Exception {
		configBean = new WPSActivityConfigurationBean();
		configBean.setProcessId("something");
		configBean
				.setUri(URI.create("http://localhost:8080/myEndPoint"));
	}

	@Test(expected = ActivityConfigurationException.class)
	@Ignore
	public void invalidConfiguration() throws ActivityConfigurationException {
		WPSActivityConfigurationBean invalidBean = new WPSActivityConfigurationBean();
		invalidBean.setProcessId("invalidExample");
		// Should throw ActivityConfigurationException
		activity.configure(invalidBean);
	}

	@Test
	@Ignore
	public void executeAsynch() throws Exception {
		activity.configure(configBean);

		Map<String, Object> inputs = new HashMap<String, Object>();
		inputs.put("firstInput", "hello");

		Map<String, Class<?>> expectedOutputTypes = new HashMap<String, Class<?>>();
		expectedOutputTypes.put("simpleOutput", String.class);
		expectedOutputTypes.put("moreOutputs", String.class);

		Map<String, Object> outputs = ActivityInvoker.invokeAsyncActivity(
				activity, inputs, expectedOutputTypes);

		assertEquals("Unexpected outputs", 2, outputs.size());
		assertEquals("simple", outputs.get("simpleOutput"));
		assertEquals(Arrays.asList("Value 1", "Value 2"), outputs
				.get("moreOutputs"));

	}

	@Test
	@Ignore
	public void reConfiguredActivity() throws Exception {
		assertEquals("Unexpected inputs", 0, activity.getInputPorts().size());
		assertEquals("Unexpected outputs", 0, activity.getOutputPorts().size());

		activity.configure(configBean);
		assertEquals("Unexpected inputs", 1, activity.getInputPorts().size());
		assertEquals("Unexpected outputs", 2, activity.getOutputPorts().size());

		activity.configure(configBean);
		// Should not change on reconfigure
		assertEquals("Unexpected inputs", 1, activity.getInputPorts().size());
		assertEquals("Unexpected outputs", 2, activity.getOutputPorts().size());
	}

	@Test
	@Ignore
	public void reConfiguredSpecialPorts() throws Exception {
		activity.configure(configBean);

		WPSActivityConfigurationBean specialBean = new WPSActivityConfigurationBean();
		specialBean.setProcessId("specialCase");
		specialBean.setUri(URI
				.create("http://localhost:8080/myEndPoint"));
		activity.configure(specialBean);		
		// Should now have added the optional ports
		assertEquals("Unexpected inputs", 2, activity.getInputPorts().size());
		assertEquals("Unexpected outputs", 3, activity.getOutputPorts().size());
	}

	@Test
	@Ignore
	public void configureActivity() throws Exception {
		Set<String> expectedInputs = new HashSet<String>();
		expectedInputs.add("firstInput");

		Set<String> expectedOutputs = new HashSet<String>();
		expectedOutputs.add("simpleOutput");
		expectedOutputs.add("moreOutputs");

		activity.configure(configBean);

		Set<ActivityInputPort> inputPorts = activity.getInputPorts();
		assertEquals(expectedInputs.size(), inputPorts.size());
		for (ActivityInputPort inputPort : inputPorts) {
			assertTrue("Wrong input : " + inputPort.getName(), expectedInputs
					.remove(inputPort.getName()));
		}

		Set<OutputPort> outputPorts = activity.getOutputPorts();
		assertEquals(expectedOutputs.size(), outputPorts.size());
		for (OutputPort outputPort : outputPorts) {
			assertTrue("Wrong output : " + outputPort.getName(),
					expectedOutputs.remove(outputPort.getName()));
		}
	}
}
