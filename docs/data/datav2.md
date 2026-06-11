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