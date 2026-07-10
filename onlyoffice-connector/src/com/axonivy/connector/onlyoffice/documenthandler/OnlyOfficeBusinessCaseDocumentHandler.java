package com.axonivy.connector.onlyoffice.documenthandler;

import com.axonivy.connector.onlyoffice.OnlyOfficeDocument;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.document.IDocumentService;

/**
 * Use workflow documents.
 */
public class OnlyOfficeBusinessCaseDocumentHandler implements OnlyOfficeDocumentHandler {
	private static final OnlyOfficeBusinessCaseDocumentHandler INSTANCE = new OnlyOfficeBusinessCaseDocumentHandler();

	public static OnlyOfficeBusinessCaseDocumentHandler get() {
		return INSTANCE;
	}

	@Override
	public OnlyOfficeDocument load(String editGroup, String documentId) {
		OnlyOfficeDocument document = null;
		var doc = documents().get(documentId);
		if(doc != null) {
			document = OnlyOfficeDocument.builder()
					.documentId(documentId)
					.fileName(documentId)
					.editGroup(editGroup)
					.stream(doc.read().asStream())
					.build();
		}
		return document;
	}

	@Override
	public void save(OnlyOfficeDocument document, boolean last) {
		var doc = documents().get(document.getDocumentId());
		if(doc == null) {
			Ivy.log().warn("Document does not exists: {0}", document.getDocumentId());
		}
		else {
			doc.write().withContentFrom(document.getStream());
		}
	}

	protected IDocumentService documents() {
		return Ivy.wf().documents();
	}
}
