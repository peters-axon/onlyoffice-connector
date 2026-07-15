# ONLYOFFICE Connector

The ONLYOFFICE Connector enables inline editing by integrating the ONLYOFFICE Document Server into Axon Ivy. The purpose is to allow users to edit documents directly inside the application workflow without leaving the process context.

A custom handler for loading and saving files can be created and registered through a subprocess using the signature:

OnlyOfficeDocumentHandler provideOnlyOfficeDocumentHandler()

This handler is then consumed by the connector. The default handler works with Ivy documents.

For reference, the implementation is based on the ONLYOFFICE document editor API:
https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/

The configuration can be overridden by dynamic content whenever the dynamic content is not available. This makes it possible to adapt the editor configuration to runtime-specific requirements.

Avoid opening the same document with the same `editGroup` in the same editor twice in a row. Reusing the same editor context for the same document can lead to inconsistent behavior.

Saving is asynchronous by default and is usually executed only after the page is left. However, the editor configuration can be used to disable autosave and enable forcesave:

```text
editorConfig.customization.autosave = false
editorConfig.customization.forcesave = true
```

Note that changing, closing, and then reopening the same document in the same browser may cause a new server-side version of the document to be created. This can then lead to an error in the browser when the previous editor state is reused.



## Demo

The demo shows a collaboration scenario in which one user uploads a document as the author, edits it, and selects it for the next step. Afterwards, a reviewer and a compliance officer each receive a task to work on the document. They can perform their updates simultaneously.

![Select document](images/01_select_document.png)

The process starts with selecting the document from the Axon Ivy workflow context. The user chooses the file that should be opened in the ONLYOFFICE editor.

![Edit document](images/02_edit_document.png)

Once the document is opened, the user can edit it directly in the integrated editor. This is the central benefit of the connector: document editing happens inline within the business process, without breaking the user flow.

![Rework tasks](images/03_rework_tasks.png)

After the author completes the initial editing step, the document is handed over to the next participants. A reviewer and a compliance representative each receive the relevant task and continue the same workflow with the same document.

![Simultaneous editing](images/04_simultaneous_editing.png)

The demo highlights the core collaboration model of the connector: multiple users can work on the same document at the same time, which supports parallel review and rework inside one end-to-end process.

## Setup

First, the ONLYOFFICE Document Server must be started. A docker-compose setup is available in the separate project for this purpose. Alternatively, the standalone installation can be downloaded directly from ONLYOFFICE. The password used in the setup must also be stored in a global variable.

ONLYOFFICE must be loaded when the page is opened. In other words, the script component must not be rendered conditionally.

```
@variables.yaml@
```
