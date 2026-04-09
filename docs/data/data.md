# Environmental AMR Dashboard: Data Model & Frontend Guide

This document outlines the relational data model for the AMR Dashboard. It explains how environmental metadata links to complex genomic results and provides concrete mock data examples to help the frontend team structure their API calls, state management, and UI components.

## 1. High-Level Architecture
The database is highly relational, flowing from the physical location down to the microscopic genetics. 
The core hierarchy is: **Site** $\rightarrow$ **Water Sample** $\rightarrow$ **Isolate** $\rightarrow$ **Sequences & Metrics**.

Whenever you fetch data from the API, keep this hierarchy in mind. For example, to find all resistance genes at "Farm A," you must traverse: `Site (Farm A) -> Water Samples -> Isolates -> AMR Sequences`.

---

## 2. Data Dictionary & Mock Examples

### A. `Site` (The Physical Location)
Represents the static geographic locations where sampling occurs. [cite_start]This is the primary data source for plotting markers on the Interactive Geospatial Map[cite: 100, 102].

| Field | Type | Description | Mock Example |
| :--- | :--- | :--- | :--- |
| `siteId` | String | Unique identifier for the location. | `"A10"` |
| `locationName` | String | Human-readable name. | `"Farm A Dispatch"` |
| `riverName` | String | The broader water system. | `"Apies River"` |
| `latitude` | Float | GPS Latitude for map plotting. | `-25.747` |
| `longitude` | Float | GPS Longitude for map plotting. | `28.229` |

### B. `Water Sample` (The Environmental Context)
Represents a single visit to a site to collect water. [cite_start]This holds the physicochemical water properties required for the time-series charts on the River Detail Page[cite: 122].

| Field | Type | Description | Mock Example |
| :--- | :--- | :--- | :--- |
| `sampleId` | String | Unique identifier for the collection event. | `"SAMP-001"` |
| `siteId` | String | Foreign key linking to the `Site`. | `"A10"` |
| `tripIdentifier` | String | Groups samples taken during the same run. | `"Trip 1"` |
| `collectionDate` | String | Date of collection (YYYY-MM-DD). | `"2025-05-10"` |
| `waterTemperature` | Float | Temperature in Celsius. | `18.5` |
| `phLevel` | Float | Acidity/alkalinity of the water. | `7.2` |
| `tds` | Float | Total Dissolved Solids. | `250.0` |
| `ec` | Float | Electrical Conductivity. | `400.0` |
| `dissolvedOxygen` | Float | Dissolved Oxygen levels. | `6.5` |

### C. `Isolate` (The Bacteria)
Represents a specific bacterial organism isolated from a water sample. [cite_start]This feeds the Bacteria Details Page and organism filters[cite: 72, 73, 104].

**Crucial Note for Frontend:** The `binaryTypingProfile` will be delivered by the API as a nested JSON object containing boolean values (true/false).

| Field | Type | Description | Mock Example |
| :--- | :--- | :--- | :--- |
| `isolateId` | String | Unique identifier for the bacteria. | `"ISO-101"` |
| `sampleId` | String | Foreign key linking to the `Water Sample`. | `"SAMP-001"` |
| `organismIdentity` | String | Scientific name of the bacteria. | `"Klebsiella pneumoniae"` |
| `sourceContext` | String | Where specifically it was found. | `"Spinach at harvest"` |
| `arCode` | String | Internal lab reference code. | `"B1"` |
| `binaryTypingProfile` | Object | True/false flags for specific resistance markers. | `{ "Intl1": true, "TEM": true, "SHV": true, "Intl2": false }` |

### D. `AMR Sequence` (The Resistance Genes)
Represents specific resistance genes found within an isolate. An isolate can have *many* sequences. [cite_start]This is used for the "antibiotic resistance filter" to show specific classifications[cite: 104].

| Field | Type | Description | Mock Example |
| :--- | :--- | :--- | :--- |
| `sequenceId` | UUID | Unique identifier for this specific gene hit. | *(Auto-generated)* |
| `isolateId` | String | Foreign key linking to the `Isolate`. | `"ISO-101"` |
| `geneSymbol` | String | The specific gene identifier. | `"bla"` |
| `elementType` | String | Category of the element (AMR, STRESS). | `"AMR"` |
| `resistanceClass` | String | Broad resistance category. | `"BETA-LACTAM"` |
| `identityPercentage` | Float | Confidence score of the gene match. | `81.36` |

### E. `WGS Metrics` (Whole Genome Stats)
Represents the overarching quality and predicted physical traits of the isolate based on genome sequencing.

| Field | Type | Description | Mock Example |
| :--- | :--- | :--- | :--- |
| `wgsId` | UUID | Unique identifier for the metric record. | *(Auto-generated)* |
| `isolateId` | String | Foreign key linking to the `Isolate` (1-to-1). | `"ISO-101"` |
| `qualityStatus` | String | Did the sequence pass QC? | `"Passed"` |
| `predictedPhenotype`| String | Comma-separated list of resisted antibiotics. | `"kanamycin, ampicillin, ceftriaxone"` |
| `plasmid` | String | Associated plasmid types. | `"IncFIB(K)"` |
| `genomeLength` | Integer| Total length of the genome sequence. | `5017831` |

---

## 3. How to Use This Data for the UI

Here is how the frontend team can map these entities to the specific pages designed in the Phase 1 prototypes:

* [cite_start]**Interactive Geospatial Map Page:** Fetch all `Sites` to plot markers on the map[cite: 100, 102]. [cite_start]When a user applies the **Organism filter**, the frontend should request `Sites` filtered by a joined query checking `Isolate.organismIdentity == 'Selected Organism'`[cite: 104].
* **River Detail Page:** When a user clicks a river/site, use the `siteId` to fetch all associated `Water Samples`. [cite_start]You can then map `collectionDate` to the X-axis and `phLevel`, `waterTemperature`, `tds`, `ec`, or `dissolvedOxygen` to the Y-axis for your time-series line charts[cite: 122].
* **Bacteria Details Page:** To show the **Case distribution by province** or site, fetch all `Isolates` matching a specific `organismIdentity`. [cite_start]Then group those records by their parent `WaterSample.Site`[cite: 76]. [cite_start]Use the `predictedPhenotype` from the `WGS Metrics` table to display the susceptibility patterns[cite: 77].