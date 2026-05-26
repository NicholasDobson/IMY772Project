<script setup lang="ts">
import { watch, onBeforeUnmount } from 'vue';
import type { Map as LeafletMap } from 'leaflet';
import L from 'leaflet';
import 'leaflet.heat';

const props = defineProps<{
  map: LeafletMap | null;
  points: [number, number, number][];
  enabled: boolean;
}>();

let heatLayer: L.HeatLayer | null = null;

function removeLayer() {
  if (heatLayer && props.map) {
    props.map.removeLayer(heatLayer);
    heatLayer = null;
  }
}

function syncHeatLayer() {
  removeLayer();
  if (!props.enabled || !props.map || props.points.length === 0) return;

  heatLayer = L.heatLayer(props.points, {
    radius: 32,
    blur: 24,
    maxZoom: 14,
    max: 1,
    minOpacity: 0.35,
    gradient: {
      0.15: '#3b82f6',
      0.4: '#22c55e',
      0.55: '#eab308',
      0.75: '#f97316',
      1: '#dc2626',
    },
  });
  heatLayer.addTo(props.map);
}

watch(
  () => [props.map, props.enabled, props.points] as const,
  () => syncHeatLayer(),
  { deep: true },
);

onBeforeUnmount(removeLayer);
</script>

<template><span class="map-heat-layer-host" /></template>

<style scoped>
.map-heat-layer-host {
  display: none;
}
</style>
