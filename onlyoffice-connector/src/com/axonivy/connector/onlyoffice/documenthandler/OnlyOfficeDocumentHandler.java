package com.axonivy.connector.onlyoffice.documenthandler;

import com.axonivy.connector.onlyoffice.OnlyOfficeDocument;

public interface OnlyOfficeDocumentHandler {
	public OnlyOfficeDocument load(String editGroup, String documentId);
	public void save(OnlyOfficeDocument document, boolean last);
}
