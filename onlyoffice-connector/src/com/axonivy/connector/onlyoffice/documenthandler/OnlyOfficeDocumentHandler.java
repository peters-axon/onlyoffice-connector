package com.axonivy.connector.onlyoffice.documenthandler;

import com.axonivy.connector.onlyoffice.OnlyOfficeDocument;

public interface OnlyOfficeDocumentHandler {
	/**
	 * Called when the document should be loaded.
	 *
	 * @param editGroup
	 * @param documentId
	 * @return
	 */

	public OnlyOfficeDocument load(String editGroup, String documentId);
	/**
	 * Called when the document should be saved.
	 *
	 * @param document
	 * @param last the last simultaneous editor left the document
	 */
	public void save(OnlyOfficeDocument document, boolean last);
}
