package com.camunda.training;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JOptionPane;

import org.camunda.bpm.client.ExternalTaskClient;
import org.camunda.bpm.client.topic.TopicSubscriptionBuilder;

public class TwitterQAClientApplication {

	public static void main(String[] args) {
		// bootstrap the client
		ExternalTaskClient client = ExternalTaskClient.create()//
				.baseUrl("http://localhost:8080/rest")//
				.asyncResponseTimeout(10000)//
				.lockDuration(10000)//
				.maxTasks(1)//
				.build();

		// subscribe to the topic
		TopicSubscriptionBuilder subscriptionBuilder = client//
				.subscribe("abgelehnteTweets");

		// handle job
		subscriptionBuilder.handler((externalTask, externalTaskService) -> {
			// Get Content
			String content = externalTask.getVariable("content");

			// Choose Okay Fail or Unlock
			String[] options = new String[] { "Okay", "Fail", "Unlock" };
			int response = JOptionPane.showOptionDialog(null, content, "Choose for Content", JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

			// Evaluate
			switch (response) {
			case 0: // Okay
				System.out.println("Okay: " + content);
				Map<String, Object> variables = new HashMap<String, Object>();
				variables.put("notficationTimestamp", new Date());
				externalTaskService.complete(externalTask, variables);
				break;

			case 1: // Fail
				System.out.println("Fail: " + content);
				externalTaskService.handleFailure(externalTask, "Random Fail", "Random Fail", 0, 0);
				break;

			case 2: // Unlock
				System.out.println("Unlocked: " + content);
				externalTaskService.unlock(externalTask);
				break;
			}

		});

		// Start working at Tasks
		subscriptionBuilder.open();
	}

}
