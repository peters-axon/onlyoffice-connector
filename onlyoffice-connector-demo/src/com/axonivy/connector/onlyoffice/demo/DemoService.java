package com.axonivy.connector.onlyoffice.demo;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.ICase;
import ch.ivyteam.ivy.workflow.query.CaseQuery;

public class DemoService {
	private static final DemoService INSTANCE = new DemoService();
	public static DemoService get() {
		return INSTANCE;
	}

	public ICase assertBusinessCase() {
		var iCase = Ivy.wfCase();
		Ivy.log().info("Case: {0}", iCase);

		Ivy.log().debug("Search for demo business case.");
		var bCase = CaseQuery.businessCases().where().customField().textField("DEMO").isNotNull().orderBy().caseId().executor().firstResult();

		if(bCase == null) {
			Ivy.log().debug("Creating demo business case: {0}", iCase);
			iCase.customFields().textField("DEMO").set("Demo Case");
			bCase = iCase;
		}
		else {
			iCase.attachToBusinessCase(bCase.getId());
		}

		Ivy.log().debug("The business case is: {0}", bCase);
		return null;
	}
}