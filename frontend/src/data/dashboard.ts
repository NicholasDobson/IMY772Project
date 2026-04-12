import type { StatCardData, Province, ResistanceGene, RiverSite, TopOrganism } from '@/types/amr'

/* ─── Dashboard Mock Data ─────────────────────────────────────────
   All values derived from mockdata/ xlsx files.
   Replace with API calls once backend endpoints are live.
─────────────────────────────────────────────────────────────────── */

export const STAT_CARDS: readonly StatCardData[] = [
  {
    id: 'esbl',
    label: 'ESBL Prevalence Rate',
    value: '38.7%',
    trendIcon: 'pi-sort-up-fill',
    trendText: '0.2% from 2025',
    valueClass: '',
    trendClass: 'trend-danger',
  },
  {
    id: 'isolates',
    label: 'Total Isolates Analysed',
    value: '4 812',
    trendIcon: null,
    trendText: 'across 18 sites',
    valueClass: '',
    trendClass: 'trend-muted',
  },
  {
    id: 'sites',
    label: 'Sampling Sites Active',
    value: '18',
    trendIcon: null,
    trendText: '2 new this campaign',
    valueClass: '',
    trendClass: 'trend-muted',
  },
  {
    id: 'genes',
    label: 'Resistance Genes Detected',
    value: '47',
    trendIcon: 'pi-sort-down-fill',
    trendText: '3 fewer than last trip',
    valueClass: 'value-blue',
    trendClass: 'trend-success',
  },
]

/* Monthly isolate detections (Epicollect dates × Binary_Information) */
export const MONTHS = [
  'Jan',
  'Feb',
  'Mar',
  'Apr',
  'May',
  'Jun',
  'Jul',
  'Aug',
  'Sep',
  'Oct',
  'Nov',
  'Dec',
] as const

export const MONTHLY_NORMAL = [210, 240, 268, 290, 318, 352, 401, 438, 490, null, null, null]
export const MONTHLY_ALERT = [null, null, null, null, null, null, null, null, null, 562, 638, 714]

/* Province risk (Epicollect site lat/lng → province mapping) */
export const PROVINCES: readonly Province[] = [
  { name: 'Gauteng', risk: 'HIGH', percent: 85 },
  { name: 'KwaZulu-Natal', risk: 'HIGH', percent: 78 },
  { name: 'Western Cape', risk: 'MED', percent: 52 },
  { name: 'Eastern Cape', risk: 'MED', percent: 45 },
  { name: 'Limpopo', risk: 'MED', percent: 38 },
  { name: 'Mpumalanga', risk: 'LOW', percent: 22 },
  { name: 'Free State', risk: 'LOW', percent: 18 },
]

/* Top resistance genes (AMRFinderPlus: Gene Symbol, Class, Subclass, % Identity) */
export const RESISTANCE_GENES: readonly ResistanceGene[] = [
  {
    gene: 'blaCTX-M-14',
    resistanceClass: 'BETA-LACTAM',
    subclass: 'CEPHALOSPORIN',
    isolates: 28,
    identity: 99.8,
  },
  {
    gene: 'blaTEM-1B',
    resistanceClass: 'BETA-LACTAM',
    subclass: 'BETA-LACTAM',
    isolates: 22,
    identity: 100.0,
  },
  {
    gene: "aph(3')-Ia",
    resistanceClass: 'AMINOGLYCOSIDE',
    subclass: 'KANAMYCIN',
    isolates: 18,
    identity: 100.0,
  },
  {
    gene: 'tet(A)',
    resistanceClass: 'TETRACYCLINE',
    subclass: 'TETRACYCLINE',
    isolates: 15,
    identity: 97.2,
  },
  {
    gene: 'erm',
    resistanceClass: 'MACROLIDE',
    subclass: 'MACROLIDE',
    isolates: 11,
    identity: 87.5,
  },
]

/* Affected river sites (Epicollect: Site ID, River, Location, Date) */
export const RIVER_SITES: readonly RiverSite[] = [
  {
    siteId: 'A10',
    river: 'Apies River',
    location: 'Farm A Dispatch',
    province: 'Gauteng',
    lastSampled: 'May 10, 2025',
    isolates: 14,
    risk: 'HIGH',
  },
  {
    siteId: 'B26',
    river: 'Apies River',
    location: 'Farm B Pivot 1',
    province: 'Gauteng',
    lastSampled: 'May 10, 2025',
    isolates: 9,
    risk: 'MED',
  },
  {
    siteId: 'T08',
    river: 'Tugela River',
    location: 'Midlands Site T',
    province: 'KwaZulu-Natal',
    lastSampled: 'Jul 15, 2025',
    isolates: 12,
    risk: 'HIGH',
  },
  {
    siteId: 'B05',
    river: 'Breede River',
    location: 'Breede Valley Station',
    province: 'Western Cape',
    lastSampled: 'Mar 12, 2025',
    isolates: 6,
    risk: 'MED',
  },
  {
    siteId: 'L04',
    river: 'Limpopo River',
    location: 'Limpopo Crossing L4',
    province: 'Limpopo',
    lastSampled: 'Feb 28, 2025',
    isolates: 8,
    risk: 'MED',
  },
]

/* Top detected organisms (recommended-endpoints §4) */
export const TOP_ORGANISMS: readonly TopOrganism[] = [
  {
    name: 'Escherichia coli',
    arCode: 'ESBL',
    detectionCount: 1547,
    siteCount: 14,
    yoyTrend: 'up',
    resistanceRate: 38.7,
  },
  {
    name: 'Klebsiella pneumoniae',
    arCode: 'CRE',
    detectionCount: 1301,
    siteCount: 11,
    yoyTrend: 'up',
    resistanceRate: 31.4,
  },
  {
    name: 'Acinetobacter baumannii',
    arCode: 'MDRO',
    detectionCount: 984,
    siteCount: 9,
    yoyTrend: 'stable',
    resistanceRate: 26.1,
  },
  {
    name: 'Pseudomonas aeruginosa',
    arCode: 'MDR',
    detectionCount: 674,
    siteCount: 8,
    yoyTrend: 'down',
    resistanceRate: 17.8,
  },
  {
    name: 'Enterococcus faecium',
    arCode: 'VRE',
    detectionCount: 413,
    siteCount: 6,
    yoyTrend: 'stable',
    resistanceRate: 11.2,
  },
]
