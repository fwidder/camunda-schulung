package com.camunda.training;

import java.util.HashMap;
import java.util.Map;

import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import static org.camunda.bpm.engine.test.assertions.ProcessEngineTests.*;
import static org.assertj.core.api.Assertions.*;

public class ProcessJUnitTest {

	@Rule
	public ProcessEngineRule rule = new ProcessEngineRule();

	@Before
	public void setup() {
		init(rule.getProcessEngine());
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testHappyPath() {
		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("okay", true);

		// Start process with Java API and variables
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

		// Make assertions on the process instance
		assertThat(processInstance).isEnded();
	}
}
