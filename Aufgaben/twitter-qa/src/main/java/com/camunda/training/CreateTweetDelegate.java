package com.camunda.training;

import javax.inject.Inject;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CreateTweetDelegate implements JavaDelegate {
	private final Logger LOGGER = LoggerFactory.getLogger(CreateTweetDelegate.class.getName());

	private TwitterService twitterService;

	@Inject
	public CreateTweetDelegate(TwitterService twitterService) {
		this.twitterService = twitterService;
	}

	public void execute(DelegateExecution execution) throws Exception {
		String content = (String) execution.getVariable("content");
		LOGGER.info("Publishing tweet: " + content);
		String status = twitterService.postTweet(content);
		execution.setVariable("tweet-id", status);
	}
}
