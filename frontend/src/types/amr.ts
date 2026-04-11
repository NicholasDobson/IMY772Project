/* ─── AMR Domain Types ─────────────────────────────────────────────
   Shared across DashboardView, BacteriaDetailView, and components.
   All data shapes derived from:
     AMRFinderPlus_Results.xlsx, Binary_Information.xlsx,
     Epicollect_Metadata.xlsx, StarAMR_Metrics.xlsx
─────────────────────────────────────────────────────────────────── */

export type RiskLevel  = 'HIGH' | 'MED' | 'LOW'
export type YoyTrend   = 'up' | 'down' | 'stable'
export type ResistanceLevel = 'R' | 'I' | 'S'
export type QualityStatus   = 'PASS' | 'FAIL' | 'WARN'

/* AMRFinderPlus row */
export interface Gene {
  geneSymbol: string
  resistanceClass: string
  subclass: string
  elementType: string
  occurrenceCount: number
  avgIdentityPct: number
  avgCoveragePct: number
}

/* Binary typing / antibiotic susceptibility row */
export interface AntibioticEntry {
  antibiotic: string
  resistanceRate: number
  level: ResistanceLevel
}

/* Epicollect sampling site with water quality */
export interface Site {
  siteId: string
  river: string
  location: string
  province: string
  lastSampled: string
  isolates: number
  risk: RiskLevel
  ph: number
  tds: number
  temp: number
  dissolvedOxygen: number
}

/* StarAMR WGS metrics */
export interface WgsMetrics {
  predictedPhenotype: string
  genotype: string
  plasmid: string
  qualityStatus: QualityStatus
  genomeLength: number
  n50: number
}

/* Full organism record (bacteria detail page) */
export interface OrganismData {
  arCode: string
  commonName: string
  gramStain: string
  description: string
  detectionCount: number
  siteCount: number
  resistanceRate: number
  yoyTrend: YoyTrend
  topGene: string
  monthlyTrend: number[]
  genes: Gene[]
  resistanceProfile: AntibioticEntry[]
  sites: Site[]
  wgs: WgsMetrics
}

/* Dashboard — top organisms table row */
export interface TopOrganism {
  name: string
  arCode: string
  detectionCount: number
  siteCount: number
  yoyTrend: YoyTrend
  resistanceRate: number
}

/* Dashboard — province risk row */
export interface Province {
  name: string
  risk: RiskLevel
  percent: number
}

/* Dashboard — resistance genes table row */
export interface ResistanceGene {
  gene: string
  resistanceClass: string
  subclass: string
  isolates: number
  identity: number
}

/* Dashboard — river sites table row */
export interface RiverSite {
  siteId: string
  river: string
  location: string
  province: string
  lastSampled: string
  isolates: number
  risk: RiskLevel
}

/* Dashboard — stat card */
export interface StatCardData {
  id: string
  label: string
  value: string
  trendIcon: string | null
  trendText: string
  valueClass: string
  trendClass: string
}
