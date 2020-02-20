package com.camunda.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.AbstractAssertions.init;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.assertThat;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.claim;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.complete;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.execute;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.externalTask;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.job;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.task;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.jobQuery;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.runtimeService;
import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.taskService;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.camunda.bpm.engine.impl.pvm.PvmException;
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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import twitter4j.TwitterException;

public class ProcessJUnitTest {

	@Rule
	@ClassRule
	public static ProcessEngineRule rule = TestCoverageProcessEngineRuleBuilder.create().build();

	@Mock
	private TwitterService twitterService;

	@Before
	public void setup() {
		init(rule.getProcessEngine());

		MockitoAnnotations.initMocks(this);
		Mocks.register("createTweetDelegate", new CreateTweetDelegate(twitterService));
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testTweetOkay() throws TwitterException {
		// Init Mocks
		Mockito.when(twitterService.postTweet(Mockito.anyString())).thenReturn("Mock ID");

		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
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

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetPostenTask");

		// Complete Waiting Job
		List<Job> jobList = jobQuery().processInstanceId(processInstance.getId()).list();
		assertThat(jobList).hasSize(1);
		Job job = jobList.get(0);
		execute(job);

		// Make assertions on the process instance
		assertThat(processInstance).isEnded().hasPassed("TweetGepostetEndEvent").variables().containsEntry("tweet-id",
				"Mock ID");
		Mockito.verify(twitterService).postTweet(Mockito.anyString());
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testTweetNotOkay() {
		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("okay", false);
		variables.put("content", "test Sad Path - " + LocalDateTime.now().toString());

		// Start process with Java API and variables
		ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("TwitterQAProcess")//
				.startAfterActivity("TweetBewertenTask")//
				.setVariables(variables).execute();

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetAbweisenTask");

		// Complete Waiting Job
		execute(job());

		// Complete Extrenal Task
		assertThat(processInstance).isWaitingAt("TweetAbweisenTask").//
				externalTask().//
				hasTopicName("abgelehnteTweets");
		complete(externalTask());

		// Make assertions on the process instance
		assertThat(processInstance).isEnded().hasPassed("TweetAbgewiesenEndEvent");
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testChefTweet() throws TwitterException {
		// Init Mocks
		Mockito.when(twitterService.postTweet(Mockito.anyString())).thenReturn("Mock ID");

		// Create Chef Message
		ProcessInstance processInstance = runtimeService().createMessageCorrelation("ChefTweet")
				.setVariable("content", "Chef Tweet Test - " + LocalDateTime.now().toString()).correlateWithResult()
				.getProcessInstance();

		// Make Sure Proccess is started
		assertThat(processInstance).isStarted();

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetPostenTask");

		// Complete Waiting Job
		List<Job> jobList = jobQuery().processInstanceId(processInstance.getId()).list();
		assertThat(jobList).hasSize(1);
		Job job = jobList.get(0);
		execute(job);

		// Make assertions on the process instance
		assertThat(processInstance).isEnded().hasPassed("TweetGepostetEndEvent").variables().containsEntry("tweet-id",
				"Mock ID");
		Mockito.verify(twitterService).postTweet(Mockito.anyString());
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testTweetStorniert() throws TwitterException {
		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("content", "test Happy Path - " + LocalDateTime.now().toString());

		// Start process with Java API and variables
		ProcessInstance processInstance = runtimeService().startProcessInstanceByKey("TwitterQAProcess", variables);

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetBewertenTask");

		// Tweet Stornierungs Anfrage
		runtimeService()//
				.createMessageCorrelation("TweetStornieren")//
				.processInstanceId(processInstance.getId())//
				.correlateWithResult();

		// Make assertions on the process instance
		assertThat(processInstance).isEnded().hasPassed("TweetStorniertEndEvent");
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testTweetDupplicateError() throws TwitterException {
		// Init Mocks
		Mockito.when(twitterService.postTweet(Mockito.anyString()))
				.thenThrow(new TwitterException("Dupplicate", null, 187));

		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("okay", true);
		variables.put("content", "test Sad Path");

		// Start process with Java API and variables
		ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("TwitterQAProcess")//
				.startAfterActivity("TweetBewertenTask")//
				.setVariables(variables).execute();

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetPostenTask");

		// Complete Waiting Job
		assertThrows(PvmException.class, () -> execute(job()));

		// Create a HashMap to put in variables for the process instance
		variables = new HashMap<String, Object>();
		variables.put("okay", true);
		variables.put("content", "test Sad Path");

		// Start process with Java API and variables
		processInstance = runtimeService().createProcessInstanceByKey("TwitterQAProcess")//
				.startBeforeActivity("DoppelteNachrichtBoundaryEvent")//
				.setVariables(variables).execute();

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetAnpassenTask");
	}

	@Test
	@Deployment(resources = "twitterQA.bpmn")
	public void testTweetDupplicateAnpassen() throws TwitterException {
		// Init Mocks
		Mockito.when(twitterService.postTweet(Mockito.anyString()))
				.thenThrow(new TwitterException("Dupplicate", null, 187));

		// Create a HashMap to put in variables for the process instance
		Map<String, Object> variables = new HashMap<String, Object>();
		variables.put("okay", true);
		variables.put("content", "test Sad Path");

		// Start process
		ProcessInstance processInstance = runtimeService().createProcessInstanceByKey("TwitterQAProcess")//
				.startBeforeActivity("DoppelteNachrichtBoundaryEvent")//
				.setVariables(variables).execute();

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetAnpassenTask");

		// Complete Waiting Job
		complete(task());

		// Make Sure Task is created and waiting
		assertThat(processInstance).isWaitingAt("TweetBewertenTask");
	}
}
