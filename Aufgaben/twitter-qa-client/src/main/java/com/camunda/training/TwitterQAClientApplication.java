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
			String content = externalTask.getVariable("content");
			String[] options = new String[] { "Okay", "Fail", "Unlock" };
			int response = JOptionPane.showOptionDialog(null, content, "Choose for Content", JOptionPane.DEFAULT_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			switch (response) {
			case 0:
				System.out.println("Okay: " + content);
				Map<String, Object> variables = new HashMap<String, Object>();
				variables.put("notficationTimestamp", new Date());
				externalTaskService.complete(externalTask, variables);
				break;

			case 1:
				System.out.println("Fail: " + content);
				externalTaskService.handleFailure(externalTask, "Random Fail", "Random Fail", 0, 0);
				break;

			case 2:
				System.out.println("Unlocked: " + content);
				externalTaskService.unlock(externalTask);
				break;
			}

//			try {
//				Thread.sleep(15000);
//			} catch (InterruptedException e) {
//				e.printStackTrace();
//			}

		});

		// Start working at Tasks
		subscriptionBuilder.open();
	}

}
