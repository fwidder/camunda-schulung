package com.camunda.training;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerDelegtate implements JavaDelegate {
	private final Logger LOGGER = LoggerFactory.getLogger(LoggerDelegtate.class.getName());

	private String[] fields;

	public LoggerDelegtate(String... fields) {
		this.fields = fields;
	}

	@Override
	public void execute(DelegateExecution execution) throws Exception {
		for (String field : fields) {
			String content = (String) execution.getVariable(field);
			LOGGER.info(field + ": " + content);
		}

	}

}
