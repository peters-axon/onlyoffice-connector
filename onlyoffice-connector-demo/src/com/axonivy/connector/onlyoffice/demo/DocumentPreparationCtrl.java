package com.axonivy.connector.onlyoffice.demo;

import java.io.IOException;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import javax.faces.application.FacesMessage;

import org.primefaces.context.PrimeFacesContext;
import org.primefaces.event.FileUploadEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

import ch.ivyteam.ivy.environment.Ivy;
import ch.ivyteam.ivy.workflow.document.IDocument;
import ch.ivyteam.ivy.workflow.document.IDocumentService;

public class DocumentPreparationCtrl {
	Map<String, IDocument> documents = new LinkedHashMap<>();
	private IDocument selectedDocument;

	public DocumentPreparationCtrl() {
		updateDocuments();
	}

	public Collection<IDocument> getDocuments() {
		return documents.values();
	}

	public void handleFileUpload(FileUploadEvent event) throws IOException {
		var uploaded = event.getFile();
		var name = uploaded.getFileName();

		updateDocuments();

		var old = documents.get(name);
		if(old != null) {
			documents().delete(old);
		}

		documents().add(name).write().withContentFrom(uploaded.getInputStream());

		updateDocuments();
	}

	public IDocument getSelectedDocument() {
		return selectedDocument;
	}

	public void setSelectedDocument(IDocument selectedDocument) {
		Ivy.log().debug("Select document {0}", selectedDocument.getName());
		this.selectedDocument = selectedDocument;
	}

	public void onRowSelect(SelectEvent<IDocument> event) {
		Ivy.log().debug("On row select {0}", event);
		this.selectedDocument = event.getObject();
	}

	public StreamedContent getFileData(String name) {
		StreamedContent result = null;
		var doc = documents.get(name);
		if(doc == null) {
			PrimeFacesContext.getCurrentInstance().addMessage(null, new FacesMessage("Could not find document '%s'".formatted(name)));
		}
		else {
			result = DefaultStreamedContent.builder()
					.stream(() -> doc.read().asStream())
					.name(doc.getName())
					.build();
		}
		return result;
	}

	public boolean isBusinessCasePersistent() {
		return Ivy.wfCase().getBusinessCase().isPersistent();
	}

	public String getConfiguration() {
		return DemoService.get().createConfiguration("12345", selectedDocument.uuid(), selectedDocument.getName());
	}

	protected void updateDocuments() {
		documents = documents().getAll().stream()
				.sorted(Comparator.comparing(IDocument::getName))
				.collect(Collectors.toMap(IDocument::getName, d -> d, (o, n) -> n, LinkedHashMap::new));
	}

	protected IDocumentService documents() {
		return Ivy.wfCase().getBusinessCase().documents();
	}
}

