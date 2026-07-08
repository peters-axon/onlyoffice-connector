<!--
Dear developer!     

When you create your very valuable documentation, please be aware that this Readme.md is not only published on github. This documentation is also processed automatically and published on our website. For this to work, the two headings "Demo" and "Setup" must not be changed. Do also not change the order of the headings. Feel free to add sub-sections wherever you want.
-->

# onlyoffice-connector Connector

YOUR DESCRIPTION GOES HERE: Please just give a short description here without further headings.

<!--
The explanations under "MY-RRODUCT-NAME" are displayed  e.g. for the Connector A-Trust here: https://market.axonivy.com/a-trust#tab-description   
-->

## Demo

YOUR DEMO DESCRIPTION GOES HERE

<!--
We use all entries under the heading "Demo" for the demo-Tab on our Website, e.g. for the Connector A-Trust here: https://market.axonivy.com/a-trust#tab-demo  
-->

## Setup

Zuerst muss man den ONLYOFFICE Document Server starten. Dazu gibt es im extra Projekt ein docker compose Setup. Natürlich kann man auch die Standalone Installation direkt bei ONLYOFFICE herunterladen. Das Passwort im Setup muss auch in einer globalen Variable eingetragen werden.

CSRF muss abgeschaltet sein.

```
@variables.yaml@
```
