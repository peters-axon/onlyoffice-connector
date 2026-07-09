# ONLYOFFICE Connector

YOUR DESCRIPTION GOES HERE: Please just give a short description here without further headings.

<!--
The explanations under "MY-RRODUCT-NAME" are displayed  e.g. for the Connector A-Trust here: https://market.axonivy.com/a-trust#tab-description   
-->

## Demo

Das Demo zeigt Inline Editing
<!--
We use all entries under the heading "Demo" for the demo-Tab on our Website, e.g. for the Connector A-Trust here: https://market.axonivy.com/a-trust#tab-demo  
-->

## Setup

Zuerst muss man den ONLYOFFICE Document Server starten. Dazu gibt es im extra Projekt ein docker compose Setup. Natürlich kann man auch die Standalone Installation direkt bei ONLYOFFICE herunterladen. Das Passwort im Setup muss auch in einer globalen Variable eingetragen werden.

CSRF muss abgeschaltet sein.

ONLYOFFICE muss beim Laden der Seite geladen werden (es darf also kein conditional rendering der script Komponente geben).

```
@variables.yaml@
```
