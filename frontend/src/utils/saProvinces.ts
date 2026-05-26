/** River → province mapping (aligned with backend AnalyticsService). */
const RIVER_PROVINCE_ENTRIES: [string, string][] = [
  ['Apies', 'Gauteng'],
  ['Hennops', 'Gauteng'],
  ['Jukskei', 'Gauteng'],
  ['Crocodile', 'Gauteng'],
  ['Blesbokspruit', 'Gauteng'],
  ['Vaal', 'Gauteng'],
  ['Tugela', 'KwaZulu-Natal'],
  ['Umgeni', 'KwaZulu-Natal'],
  ['Breede', 'Western Cape'],
  ['Berg', 'Western Cape'],
  ['Olifants', 'Limpopo'],
  ['Limpopo', 'Limpopo'],
  ['Komati', 'Mpumalanga'],
  ['Sabie', 'Mpumalanga'],
  ['Molopo', 'North West'],
  ['Harts', 'North West'],
];

export function normalizeProvinceName(name: string): string {
  if (name === 'Nothern Cape') return 'Northern Cape';
  return name;
}

export function provinceForRiver(riverName: string | null | undefined): string {
  if (!riverName) return 'Unknown';
  const lower = riverName.toLowerCase();
  for (const [fragment, province] of RIVER_PROVINCE_ENTRIES) {
    if (lower.includes(fragment.toLowerCase())) return province;
  }
  return 'Unknown';
}

export function provinceFillColor(siteCount: number): string {
  if (siteCount >= 4) return '#dc2626';
  if (siteCount >= 2) return '#d97706';
  if (siteCount >= 1) return '#2563eb';
  return '#94a3b8';
}
