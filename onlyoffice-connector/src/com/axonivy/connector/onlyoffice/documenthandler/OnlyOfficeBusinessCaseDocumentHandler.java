package com.axonivy.connector.onlyoffice.documenthandler;

import com.axonivy.connector.onlyoffice.OnlyOfficeDocument;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.document.IDocumentService;

/**
 * Use documents of current business case. The documentId contains the path.
 */
public class OnlyOfficeBusinessCaseDocumentHandler implements OnlyOfficeDocumentHandler {
	private static final OnlyOfficeBusinessCaseDocumentHandler INSTANCE = new OnlyOfficeBusinessCaseDocumentHandler();

	public static OnlyOfficeBusinessCaseDocumentHandler get() {
		return INSTANCE;
	}

	@Override
	public OnlyOfficeDocument load(String editGroup, String documentId) {
		OnlyOfficeDocument document = null;
		var caseDoc = documents().get(documentId);
		if(caseDoc != null) {
			document = OnlyOfficeDocument.builder()
					.documentId(documentId)
					.fileName(documentId)
					.editGroup(editGroup)
					.stream(caseDoc.read().asStream())
					.build();
		}
		return document;
	}

	@Override
	public void save(OnlyOfficeDocument document, boolean last) {
		var caseDoc = documents().get(document.getDocumentId());
		if(caseDoc != null) {
			documents().delete(caseDoc);
		}
		documents().add(document.getDocumentId()).write().withContentFrom(document.getStream());
	}

	protected IDocumentService documents() {
		return Ivy.wfCase().getBusinessCase().documents();
	}
}
