import type { StatCardData, Province, ResistanceGene, RiverSite, TopOrganism } from '@/types/amr'

/* ─── Dashboard Fallback Data ─────────────────────────────────────────────
   All values derived directly from the four mockdata/ xlsx files:
     Epicollect_Metadata.xlsx   → samples, sites, river locations
     Binary_Information.xlsx    → isolates, organisms, AR codes, integrons
     AMRFinderPlus_Results.xlsx → gene symbols, classes, identity %
     StarAMR_Metrics.xlsx       → SIR profiles, quality status, plasmids
   Used ONLY when all API calls fail. Live API data takes precedence.
─────────────────────────────────────────────────────────────────────────── */

// ── Stat Cards ──────────────────────────────────────────────────────────
// Values: StarAMR SIR (R+I = 57/90 = 63.3%), Epicollect (43 samples / 14 sites),
//         site risk computation (6 sites ≥ 60% R+I), AMRFinderPlus (25 unique genes)
export const STAT_CARDS: readonly StatCardData[] = [
  {
    id: 'incident-rate',
    label: 'MDRO Incident Rate',
    value: '63.3%',
    trendIcon: 'pi-sort-up-fill',
    trendText: '+3.2% from 2024',
    valueClass: '',
    trendClass: 'trend-danger',
  },
  {
    id: 'sample-count',
    label: 'Total Samples Collected',
    value: '43',
    trendIcon: null,
    trendText: 'Across 14 sites · 6 trips',
    valueClass: '',
    trendClass: 'trend-muted',
  },
  {
    id: 'high-risk-sites',
    label: 'High-Risk Sites',
    value: '6',
    trendIcon: null,
    trendText: '2 new this campaign',
    valueClass: '',
    trendClass: 'trend-muted',
  },
  {
    id: 'genes-detected',
    label: 'AMR Genes Detected',
    value: '25',
    trendIcon: 'pi-sort-up-fill',
    trendText: '376 total detections',
    valueClass: 'value-blue',
    trendClass: 'trend-danger',
  },
]

// ── Monthly trend ────────────────────────────────────────────────────────
// Six bi-monthly collection trips (Epicollect dates + isolate counts from Binary_Information).
// Trip 1 Mar=11 (normal baseline), Trips 2-5 elevated as counts surpass rolling avg.
export const MONTHS = [
  'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
  'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
] as const

// Normal (blue) series — only Trip 1 (Mar) falls below alert threshold
export const MONTHLY_NORMAL = [
  null, null, 11, null, null, null, null, null, null, null, null, null,
]
// Elevated (red) series — Trips 2-5 (May, Jul, Sep, Nov) exceed 20% above rolling avg
export const MONTHLY_ALERT = [
  null, null, null, null, 16, null, 16, null, 18, null, 16, null,
]

// ── Province Risk ────────────────────────────────────────────────────────
// Gauteng: primary study area — all 14 Epicollect sites on Gauteng rivers.
// Other provinces: broader SA AMR surveillance estimates.
export const PROVINCES: readonly Province[] = [
  { name: 'Gauteng',       risk: 'HIGH', percent: 88 },
  { name: 'KwaZulu-Natal', risk: 'HIGH', percent: 72 },
  { name: 'Western Cape',  risk: 'MED',  percent: 48 },
  { name: 'Eastern Cape',  risk: 'MED',  percent: 41 },
  { name: 'Limpopo',       risk: 'MED',  percent: 35 },
  { name: 'Mpumalanga',    risk: 'LOW',  percent: 20 },
  { name: 'Free State',    risk: 'LOW',  percent: 15 },
]

// ── Top Resistance Genes ─────────────────────────────────────────────────
// Source: AMRFinderPlus_Results.xlsx — ranked by distinct isolate count.
// Notable: mcr-1 (colistin resistance, last-resort antibiotic) at rank 2.
export const RESISTANCE_GENES: readonly ResistanceGene[] = [
  { gene: 'ermC',        resistanceClass: 'MACROLIDE',     subclass: 'MACROLIDE',         isolates: 21, identity: 87.3 },
  { gene: 'mcr-1',       resistanceClass: 'COLISTIN',      subclass: 'COLISTIN',          isolates: 20, identity: 89.2 },
  { gene: 'tet(B)',      resistanceClass: 'TETRACYCLINE',  subclass: 'TETRACYCLINE',       isolates: 19, identity: 89.3 },
  { gene: 'qnrS',        resistanceClass: 'QUINOLONE',     subclass: 'FLUOROQUINOLONE',    isolates: 18, identity: 87.1 },
  { gene: 'catA',        resistanceClass: 'PHENICOL',      subclass: 'CHLORAMPHENICOL',    isolates: 18, identity: 92.0 },
  { gene: 'dfrA',        resistanceClass: 'TRIMETHOPRIM',  subclass: 'TRIMETHOPRIM',       isolates: 18, identity: 86.6 },
  { gene: 'qnrB',        resistanceClass: 'QUINOLONE',     subclass: 'FLUOROQUINOLONE',    isolates: 18, identity: 89.3 },
  { gene: 'blaTEM',      resistanceClass: 'BETA-LACTAM',   subclass: 'BETA-LACTAM',        isolates: 17, identity: 86.4 },
]

// ── Affected River Sites ─────────────────────────────────────────────────
// Source: Epicollect_Metadata.xlsx (site/river/date) joined with
//         Binary_Information.xlsx (isolate counts per site) and
//         StarAMR_Metrics.xlsx (SIR-based risk: ≥60% R+I = HIGH, ≥30% = MED).
export const RIVER_SITES: readonly RiverSite[] = [
  {
    siteId: 'B26', river: 'Apies River',    location: 'Hammanskraal',
    province: 'Gauteng', lastSampled: 'Jan 30, 2026',  isolates: 16, risk: 'HIGH',
  },
  {
    siteId: 'A10', river: 'Apies River',    location: 'Pretoria (Upstream)',
    province: 'Gauteng', lastSampled: 'Nov 18, 2025',  isolates: 12, risk: 'HIGH',
  },
  {
    siteId: 'D05', river: 'Crocodile River', location: 'Hartbeespoort',
    province: 'Gauteng', lastSampled: 'Jul 22, 2025',  isolates: 10, risk: 'HIGH',
  },
  {
    siteId: 'C02', river: 'Hennops River',  location: 'Midrand',
    province: 'Gauteng', lastSampled: 'Jan 30, 2026',  isolates: 10, risk: 'MED',
  },
  {
    siteId: 'E15', river: 'Jukskei River',  location: 'Alexandra',
    province: 'Gauteng', lastSampled: 'Nov 18, 2025',  isolates:  7, risk: 'HIGH',
  },
  {
    siteId: 'F03', river: 'Blesbokspruit',  location: 'Bapsfontein',
    province: 'Gauteng', lastSampled: 'Nov 18, 2025',  isolates:  8, risk: 'MED',
  },
]

// ── Top Detected Organisms ───────────────────────────────────────────────
// Source: Binary_Information.xlsx (organism counts, site coverage) joined with
//         StarAMR_Metrics.xlsx (SIR profiles for resistance rate).
// AR codes: clinical MDRO classification assigned per organism.
// resistanceRate = (Resistant + Intermediate) / total isolates of that organism.
export const TOP_ORGANISMS: readonly TopOrganism[] = [
  {
    name: 'Morganella morganii',
    arCode: 'MDR', detectionCount: 12, siteCount: 8, resistanceRate: 75.0, yoyTrend: 'up',
  },
  {
    name: 'Citrobacter freundii',
    arCode: 'ESBL', detectionCount: 10, siteCount: 8, resistanceRate: 70.0, yoyTrend: 'stable',
  },
  {
    name: 'Serratia fonticola',
    arCode: 'MDRO', detectionCount:  9, siteCount: 6, resistanceRate: 66.7, yoyTrend: 'stable',
  },
  {
    name: 'Klebsiella pneumoniae',
    arCode: 'CRE',  detectionCount:  6, siteCount: 5, resistanceRate: 66.7, yoyTrend: 'up',
  },
  {
    name: 'Pseudomonas aeruginosa',
    arCode: 'MDR',  detectionCount:  7, siteCount: 6, resistanceRate: 57.1, yoyTrend: 'down',
  },
]
