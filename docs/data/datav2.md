## Epicollect (Field Data)
**Purpose of this file:** To capture the geographical data, environmental conditions, and logistical metadata recorded at the exact moment a water sample is collected in the field.

**Attributes of the file:**
* **Site ID:** The unique alphanumeric code representing the physical sampling site (e.g., A11).
* **Location Name:** The descriptive human-readable name of the area (e.g., Pretoria Midstream).
* **River Name:** The overarching body of water being sampled (e.g., Apies River).
* **Lat:** The geographical latitude of the site.
* **Lng:** The geographical longitude of the site.
* **Sample ID:** The unique identifier generated for the physical water sample (e.g., SAMP-0001).
* **Sample Name:** The internal lab or secondary reference name for the sample.
* **Analysis Type:** The intended laboratory workflow for this sample (e.g., WGS).
* **Trip ID:** The reference code linking samples collected during the same field excursion.
* **Date:** The date the sample was physically collected (YYYY-MM-DD format).
* **Temp:** The water temperature at the time of collection (in Celsius).
* **pH:** The acidity or alkalinity level of the water.
* **TDS:** Total Dissolved Solids measured in the water.
* **EC:** Electrical Conductivity of the water.
* **DO:** Dissolved Oxygen levels in the water.
* **Collector Email:** The email address of the researcher who physically gathered the sample (used for system assignment).

---

## Binary Info (Isolates)
**Purpose of this file:** To catalog the specific bacterial organisms isolated from the field samples and record the presence (1) or absence (0) of targeted genes and characteristics.

**Attributes of the file:**
* **Sample ID:** The parent water sample this organism was extracted from.
* **Isolate ID:** The unique system identifier for this specific bacterial isolate.
* **Isolate Number:** The laboratory reference code for the isolate tube/plate.
* **Organism:** The scientific taxonomic identification of the bacteria (e.g., Citrobacter freundii).
* **Context:** The environmental or clinical description of the source (e.g., River water upstream).
* **AR Code:** The antimicrobial resistance classification code (e.g., ESBL).
* **Virulence Genes:** A comma-separated list of notable disease-causing genes detected.
* **Intl1:** Binary flag (1 or 0) indicating the presence of the Class 1 Integron.
* **Intl2:** Binary flag (1 or 0) indicating the presence of the Class 2 Integron.
* **Intl3:** Binary flag (1 or 0) indicating the presence of the Class 3 Integron.
* **TEM:** Binary flag (1 or 0) indicating the presence of TEM beta-lactamase genes.
* **SHV:** Binary flag (1 or 0) indicating the presence of SHV beta-lactamase genes.
* **Owner Email:** The email address of the lab technician responsible for this isolate.

---

## AMR Finder (Gene Sequences)
**Purpose of this file:** To provide granular bioinformatics data detailing the specific resistance genes and mutations found during the genetic sequencing of an isolate.

**Attributes of the file:**
* **Isolate ID:** The unique identifier linking these genes back to their parent isolate.
* **Gene Symbol:** The standard scientific shorthand for the detected gene (e.g., aph(3')-Ia).
* **Sequence Name:** The full biological name of the gene or protein sequence.
* **Element Type:** The classification of the genetic element (e.g., AMR, POINT).
* **Class:** The broad family of antibiotics this gene resists (e.g., AMINOGLYCOSIDE).
* **Subclass:** The specific antibiotic within the class (e.g., KANAMYCIN).
* **Target Length:** The sequence length of the targeted gene region.
* **Reference Length:** The known sequence length of the reference gene being compared against.
* **Identity %:** The percentage match between the detected sequence and the reference sequence.
* **Coverage %:** The percentage of the reference sequence covered by the detected sequence.
* **Alignment Length:** The number of aligned bases between the sample and the reference.
* **Accession:** The public database accession number for the closest matching reference sequence.

---

## Star AMR (WGS Metrics)
**Purpose of this file:** To summarize the overall quality and predictive antimicrobial resistance profiles derived from Whole-Genome Sequencing (WGS).

**Attributes of the file:**
* **Isolate ID:** The unique identifier linking these metrics back to their parent isolate.
* **Quality Status:** The bioinformatics quality control result (e.g., Passed, Failed).
* **Genotype:** The high-level summary of resistance genotypes identified in the sequence.
* **Predicted Phenotype:** The physical resistance traits (antibiotics) predicted by the sequence data.
* **SIR Profile:** The Susceptible, Intermediate, or Resistant (SIR) categorization based on the phenotype.
* **Plasmid:** Identifying information for detected plasmids within the genome.
* **Genome Length:** The total calculated base-pair length of the sequenced genome.
* **N50 Value:** A statistical measure of genome assembly quality (longer contig lengths generally indicate better assemblies).

---

## Single File Upload (Consolidated)
**Purpose of this file:** To let users import the essential data in **one** spreadsheet instead of the four files above. Every column in this file is a subset of the data already captured across Epicollect, Binary Info, AMR Finder, and Star AMR. It is the simplest way to get a site, sample, isolate, its resistance genes, and a slice of its WGS profile into the dashboard in a single upload. Any attribute that exists in the four-file workflow but is **not** listed below is simply stored blank — the dashboard shows less detail for single-file records, but all the core information is present.

**How it maps to the four-file workflow:** Each attribute below corresponds to exactly one field from the multi-file sources (shown in brackets as `→ Field (Source file)`). The single file is uploaded on the **"Single file"** tab of the Data Upload page.

**Behaviour and rules:**
* **One row per resistance gene.** Because an isolate can have many AMR genes, the same isolate (and its site/sample) is repeated across rows — one row per gene. Site, sample, isolate, and WGS records are merged (upserted) across those rows.
* **Isolate ID is the only required column.** A row missing it is rejected and the whole upload is rolled back (no partial imports). Every other column may be left blank.
* **Site is keyed on Location.** The single file has no "Site ID" column, so each distinct **Location** value becomes its own site/map marker (rows sharing a Location merge into one site).
* **Number format.** Numeric values may use a comma **or** a dot as the decimal separator (e.g. `76,44` or `76.44`).
* **Date format.** Collection Date accepts `DD-MM-YYYY` (e.g. `30-11-2017`) as well as ISO `YYYY-MM-DD`.
* **Headers.** Column headers must match the names below exactly (surrounding spaces are ignored). Column order does not matter, and any extra columns are ignored.

**Attributes of the file:**
* **Sample Name:** The unique identifier for the water sample, e.g. `SAMP-0001`. → Sample ID (Epicollect & Binary Info).
* **Sample Analysis Type:** The laboratory workflow, either `WGS` or `Metagenomics`. → Analysis Type (Epicollect).
* **Isolate ID:** The unique identifier for the bacterial isolate, e.g. `ISO-100`. **(Required.)** → Isolate ID (AMR Finder, Star AMR, Binary Info).
* **Organism:** The scientific identification of the bacteria, e.g. `Klebsiella pneumoniae`. → Organism (Binary Info).
* **Country:** The country the sample was collected in, e.g. `South Africa`. *Not stored* — this column appears on the client's file and is accepted for parity, but it is ignored during import (the dashboard does not persist or display it).
* **Location:** The descriptive name of the sampling site, e.g. `Groenkloof`. Used as the site key. → Location Name (Epicollect).
* **River Name:** The body of water sampled, e.g. `Apies River`. → River Name (Epicollect).
* **Isolation source:** The environmental description of the source, e.g. `River Water`. → Context (Binary Info).
* **Collection Date:** The date the sample was collected, e.g. `30-11-2017`. → Date (Epicollect).
* **Latitude:** The geographical latitude of the site, e.g. `-25.7470`. → Lat (Epicollect).
* **Longitude:** The geographical longitude of the site, e.g. `28.2290`. → Lng (Epicollect).
* **Collected By:** The email of the researcher who collected the sample, e.g. `jane.doe@tuks.co.za`. → Collector Email (Epicollect).
* **AMR Resistance genes:** The standard shorthand for the detected gene, e.g. `aph(3')-Ia`. → Gene Symbol (AMR Finder).
* **Sequence Name:** The full biological name of the gene/protein, e.g. `aminoglycoside O-phosphotransferase APH(3')-Ia`. → Sequence Name (AMR Finder).
* **Element type:** The classification of the genetic element, e.g. `AMR`. → Element Type (AMR Finder).
* **Class:** The broad antibiotic family the gene resists, e.g. `AMINOGLYCOSIDE`. → Class (AMR Finder).
* **Subclass:** The specific antibiotic within the class, e.g. `KANAMYCIN`. → Subclass (AMR Finder).
* **Target length:** The sequence length of the targeted gene region, e.g. `816`. → Target Length (AMR Finder).
* **Reference sequence length:** The length of the reference gene compared against, e.g. `816`. → Reference Length (AMR Finder).
* **% Coverage of reference sequence:** The percentage of the reference sequence covered, e.g. `76,44`. → Coverage % (AMR Finder).
* **% Identity to reference sequence:** The percentage match to the reference sequence, e.g. `79,23`. → Identity % (AMR Finder).
* **Alignment Length:** The number of aligned bases, e.g. `623`. → Alignment Length (AMR Finder).
* **Accession of Closest Sequence:** The public accession number of the closest match, e.g. `WP_015345217.1`. → Accession (AMR Finder).
* **Virulence Genes:** Notable disease-causing genes detected, e.g. `fyuA`. → Virulence Genes (Binary Info).
* **Plasmid Replicons:** Identifying information for detected plasmids, e.g. `IncFII`. → Plasmid (Star AMR).
* **Predicted SIR profile:** The Susceptible/Intermediate/Resistant categorisation, e.g. `R`. → SIR Profile (Star AMR).
* **pH:** The acidity/alkalinity of the water, e.g. `6,7`. → pH (Epicollect).
* **Temp of water:** The water temperature in Celsius, e.g. `13,5`. → Temp (Epicollect).
* **TDS (mg/L):** Total Dissolved Solids, e.g. `107`. → TDS (Epicollect).
* **Dissolved Oxygen (mg/L):** Dissolved Oxygen level, e.g. `4`. → DO (Epicollect).