import 'leaflet';

declare module 'leaflet' {
  interface HeatMapOptions {
    minOpacity?: number;
    maxZoom?: number;
    max?: number;
    radius?: number;
    blur?: number;
    gradient?: Record<number, string>;
  }

  interface HeatLayer extends Layer {}

  function heatLayer(
    latlngs: [number, number, number?][],
    options?: HeatMapOptions,
  ): HeatLayer;
}
