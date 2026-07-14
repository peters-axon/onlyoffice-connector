# ONLYOFFICE Connector

Es geht darum inline Editieren mit dem ONLYOFFICE Document Server anzubinden.

Ein eigener Handler zum Laden und Speichern von Files kann angelegt und mittels eines Subprocesses mit der Signatur

OnlyOfficeDocumentHandler provideOnlyOfficeDocumentHandler()

angegeben werden und wird dann vom Connecter verwendet.

Der Default Handler arbeitet mit Ivy Dokumenten.

https://api.onlyoffice.com/docs/docs-api/usage-api/doceditor/

Erklärung Konfiguration wird mit dynamischem Content überschrieben sofern der nicht vorhanden ist.

## Demo

Im Demo geht es darum, dass eine Person ein Dokument als Autor hochlädt, editiert und selektiert. Danach bekommen jeweils ein Reviewer und ein Compliance Beauftragter die Aufgabe das Dokument zu bearbeiten. Sie können dies gleichzeitig tun.

## Setup

Zuerst muss man den ONLYOFFICE Document Server starten. Dazu gibt es im extra Projekt ein docker compose Setup. Natürlich kann man auch die Standalone Installation direkt bei ONLYOFFICE herunterladen. Das Passwort im Setup muss auch in einer globalen Variable eingetragen werden.

ONLYOFFICE muss beim Laden der Seite geladen werden (es darf also kein conditional rendering der script Komponente geben).

```
@variables.yaml@
```
