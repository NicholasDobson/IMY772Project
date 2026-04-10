CREATE TABLE sites (
    site_id VARCHAR(50) PRIMARY KEY,
    location_name VARCHAR(255),
    river_name VARCHAR(255),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION
);

CREATE TABLE water_samples (
    sample_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    site_id VARCHAR(50) REFERENCES sites(site_id),
    collected_by_user_id UUID, -- Links to the AWS Cognito User 'sub'
    trip_identifier VARCHAR(100),
    collection_date DATE,
    water_temperature DOUBLE PRECISION,
    ph_level DOUBLE PRECISION,
    tds DOUBLE PRECISION,
    ec DOUBLE PRECISION,
    dissolved_oxygen DOUBLE PRECISION,
    sample_name VARCHAR(255),
    sample_analysis_type VARCHAR(100)
);

CREATE TABLE isolates (
    isolate_id VARCHAR(100) PRIMARY KEY, -- e.g., UP_BN_LR_0089
    sample_id UUID REFERENCES water_samples(sample_id),
    owner_id UUID, -- Links to AWS Cognito User 'sub'
    isolate_number VARCHAR(100),
    organism_identity VARCHAR(255),
    source_context VARCHAR(255),
    ar_code VARCHAR(50),
    binary_typing_profile JSONB, -- Stores the dynamic true/false flags for genes
    virulence_genes TEXT
);

CREATE TABLE amr_sequences (
    sequence_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    isolate_id VARCHAR(100) REFERENCES isolates(isolate_id),
    gene_symbol VARCHAR(255),
    element_type VARCHAR(255),
    resistance_class VARCHAR(255),
    resistance_subclass VARCHAR(255),
    identity_percentage DOUBLE PRECISION,
    coverage_percentage DOUBLE PRECISION,
    sequence_name VARCHAR(255),
    target_length INTEGER,
    reference_sequence_length INTEGER,
    alignment_length INTEGER,
    accession_closest_sequence VARCHAR(100)
);

CREATE TABLE wgs_metrics (
    wgs_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    isolate_id VARCHAR(100) REFERENCES isolates(isolate_id) UNIQUE,
    quality_status VARCHAR(50),
    predicted_phenotype TEXT,
    genotype TEXT,
    plasmid TEXT,
    genome_length INTEGER,
    n50_value INTEGER,
    predicted_sir_profile VARCHAR(100)
);