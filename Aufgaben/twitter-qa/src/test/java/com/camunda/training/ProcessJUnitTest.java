package com.camunda.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.claim;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.withVariables;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.runtime.Job;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.task.Task;
import org.camunda.bpm.engine.test.Deployment;
import org.camunda.bpm.engine.test.ProcessEngineRule;
import org.camunda.bpm.engine.test.mock.Mocks;
import org.camunda.bpm.extension.process_test_coverage.junit.rules.TestCoverageProcessEngineRuleBuilder;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

public class ProcessJUnitTest {

	@Rule
	@ClassRule
	public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

	@Before
	public void setup() {
		init(rule.getProcessEngine());
		Mocks.register("createTweetDelegate", new LoggerDelegtate("content"));
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testHappyPath() {
		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("okay", true);
		variables.put("content", "test Happy Path - " + LocalDateTime.now().toString());

		// Start process with Java API and variables
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetBewertenTask");

		// Get Task List
		List<Task> taskList = taskService().createTaskQuery()//
				.taskCandidateGroup("TweetBewerter")//
				.processInstanceId(processInstance.getId())//
				.list();

		// Check that Task List is not Empty
		assertThat(taskList).isNotNull();
		assertThat(taskList).hasSize(1);

		// Get Task
		Task task = taskList.get(0);

		// Assert that Task is okay
		assertThat(task).hasCandidateGroup("TweetBewerter").isNotAssigned();

		// Claim Task
		claim(task, "Chef");

		// Complete Task
		Map<String, Object> approvedMap = new HashMap<String, Object>();
		approvedMap.put("okay", true);
		taskService().complete(task.getId(), approvedMap);

		// Complete Waiting Job
		List<Job> jobList = jobQuery().processInstanceId(processInstance.getId()).list();
		assertThat(jobList).hasSize(1);
		Job job = jobList.get(0);
		execute(job);

		// Make assertions on the process instance
		assertThat(processInstance).isEnded();
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testSadPath() {
		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("okay", true);
		variables.put("content", "test Happy Path - " + LocalDateTime.now().toString());

		// Start process with Java API and variables
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetBewertenTask");

		// Get Task List
		List<Task> taskList = taskService().createTaskQuery()//
				.taskCandidateGroup("TweetBewerter")//
				.processInstanceId(processInstance.getId())//
				.list();

		// Check that Task List is not Empty
		assertThat(taskList).isNotNull();
		assertThat(taskList).hasSize(1);

		// Get Task
		Task task = taskList.get(0);

		// Assert that Task is okay
		assertThat(task).hasCandidateGroup("TweetBewerter").isNotAssigned();

		// Claim Task
		claim(task, "Chef");

		// Complete Task
		// Map<String, Object> approvedMap = new HashMap<String, Object>();
		// approvedMap.put("okay", false);
		// taskService().complete(task.getId(), approvedMap);
		// Alternativ
		complete(task(), withVariables("okay", false));

		// Complete Waiting Job
		// List<Job> jobList =
		// jobQuery().processInstanceId(processInstance.getId()).list();
		// assertThat(jobList).hasSize(1);
		// Job job = jobList.get(0);
		// execute(job);
		// Alternativ
		execute(job());

		// Make assertions on the process instance
		assertThat(processInstance).isEnded();
	}
}
