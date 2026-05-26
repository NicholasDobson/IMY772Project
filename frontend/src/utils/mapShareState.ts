export type PinColorMode = 'layer' | 'risk';

export interface MapLayerState {
  trip: string;
  riverName: string;
  organism: string;
  sirProfile: string;
  showAdvanced: boolean;
  rivers: string[];
  organisms: string[];
  sirProfiles: string[];
  visible: boolean;
}

export interface MapShareState {
  pinMode: PinColorMode;
  showRiverLines: boolean;
  layers: MapLayerState[];
}

export function encodeMapState(state: MapShareState): string {
  return btoa(unescape(encodeURIComponent(JSON.stringify(state))));
}

export function decodeMapState(encoded: string | undefined | null): MapShareState | null {
  if (!encoded) return null;
  try {
    return JSON.parse(decodeURIComponent(escape(atob(encoded)))) as MapShareState;
  } catch {
    return null;
  }
}
