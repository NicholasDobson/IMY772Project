<template>
  <div class="map-view-container">
    
    <div class="map-wrapper">
      <l-map
        ref="mapRef"
        v-model:zoom="zoom"
        :center="center"
        :use-global-leaflet="false"
        :zoomAnimation="true"
        @ready="onMapReady"
      >
        <l-tile-layer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
          layer-type="base"
          name="CartoDB Voyager"
          attribution="&copy; <a href='https://carto.com/'>CartoDB</a>"
        ></l-tile-layer>

        <l-polyline
          v-for="line in riverPolylines"
          :key="line.river"
          :lat-lngs="line.latlngs"
          :color="line.color"
          :weight="3"
          :opacity="0.55"
          :dash-array="'8 6'"
        />

        <l-marker
          v-for="marker in mapDisplayMarkers"
          :key="marker.mapKey"
          :lat-lng="[marker.latitude, marker.longitude]"
          :icon="marker.icon"
          @click="onMarkerClick(marker.siteId)"
        >
          <l-tooltip>
            <strong>{{ marker.locationName }}</strong><br/>
            <span style="color: var(--c-text-muted)">{{ marker.riverName }}</span>
            <br/><span class="tooltip-risk" :style="{ color: marker.riskColor }">{{ marker.riskLevel }}</span>
            <template v-if="marker.layerLabels.length > 1">
              <br/><span class="tooltip-layers">{{ marker.layerLabels.join(' · ') }}</span>
            </template>
          </l-tooltip>
        </l-marker>

        <l-control position="topright" class="map-legend-control">
          <div class="map-legend">
            <div class="map-legend-title">Legend</div>
            <div class="map-legend-mode">
              <button
                type="button"
                :class="{ active: pinColorMode === 'layer' }"
                @click="setPinColorMode('layer')"
              >Layers</button>
              <button
                type="button"
                :class="{ active: pinColorMode === 'risk' }"
                @click="setPinColorMode('risk')"
              >Risk</button>
            </div>
            <template v-if="pinColorMode === 'layer'">
              <div v-for="set in filterSets" :key="set.id" class="legend-row">
                <span class="layer-dot" :style="{ backgroundColor: set.color }"></span>
                {{ set.label }}
              </div>
              <div v-if="filterSets.length > 1" class="legend-row legend-row--muted">
                <span class="layer-dot pin-dot--overlap-mini"></span>
                Overlap
              </div>
            </template>
            <template v-else>
              <div v-for="item in RISK_LEGEND" :key="item.level" class="legend-row">
                <span class="layer-dot" :style="{ backgroundColor: item.color }"></span>
                {{ item.level }}
              </div>
            </template>
          </div>
        </l-control>

        <l-control position="topleft" class="map-toolbar-control">
          <div class="map-toolbar">
            <button type="button" title="Fit all markers" @click="fitMapBounds">
              <i class="pi pi-expand"></i>
            </button>
            <button type="button" title="Copy shareable link" @click="copyShareLink">
              <i class="pi pi-link"></i>
            </button>
            <button
              type="button"
              :class="{ active: showRiverLines }"
              title="Toggle river connections"
              @click="showRiverLines = !showRiverLines"
            >
              <i class="pi pi-share-alt"></i>
            </button>
          </div>
        </l-control>

        <l-control position="bottomleft" class="custom-map-control">
          <div class="live-stats-pill">
            <template v-if="filterSets.length === 1 && filterSets[0]">
              <span class="pulse-dot" :style="{ backgroundColor: filterSets[0].color }"></span>
              Showing <strong>{{ filterSets[0].markers.length }}</strong> sites
            </template>
            <template v-else>
              <div class="stats-layers">
                <div v-for="set in filterSets" :key="set.id" class="stats-layer-row">
                  <span class="layer-dot" :style="{ backgroundColor: set.color }"></span>
                  <span><strong>{{ set.label }}</strong>: {{ set.visible ? set.markers.length : 'hidden' }} sites</span>
                </div>
              </div>
            </template>
          </div>
        </l-control>
      </l-map>
    </div>

    <div v-if="compareSlots.some(Boolean)" class="compare-bar">
      <div class="compare-bar-label"><i class="pi pi-sliders-h"></i> Compare sites</div>
      <div class="compare-slots">
        <div
          v-for="(slot, idx) in compareSlots"
          :key="idx"
          class="compare-slot"
          :class="{ 'compare-slot--filled': !!slot }"
        >
          <span class="compare-slot-tag">{{ idx === 0 ? 'A' : 'B' }}</span>
          <span>{{ slot ? slotLabel(slot) : 'Click a pin to select' }}</span>
          <button v-if="slot" type="button" class="compare-slot-clear" @click="clearCompareSlot(idx as 0 | 1)">
            <i class="pi pi-times"></i>
          </button>
        </div>
      </div>
      <button
        type="button"
        class="compare-go-btn"
        :disabled="!canOpenComparison"
        @click="openComparison"
      >
        Open comparison
      </button>
      <button type="button" class="compare-clear-all" @click="clearCompare">Clear</button>
    </div>

    <div class="floating-filters-panel">
      <div class="filters-header">
        <h2><i class="pi pi-filter"></i> Map Filters</h2>
        <button 
          v-if="hasActiveFilters" 
          @click="resetAllFilters" 
          class="reset-btn" 
          title="Clear all layers"
        >
          <i class="pi pi-refresh"></i> Reset
        </button>
      </div>

      <div class="layer-tabs">
        <button
          v-for="set in filterSets"
          :key="set.id"
          type="button"
          class="layer-tab"
          :class="{ 'layer-tab--active': activeFilterSetId === set.id }"
          :style="activeFilterSetId === set.id ? { borderColor: set.color, color: set.color } : {}"
          @click="activeFilterSetId = set.id"
        >
          <span class="layer-dot" :style="{ backgroundColor: set.color }"></span>
          <span class="layer-tab-label">{{ set.label }}</span>
          <span class="layer-count">{{ set.markers.length }}</span>
          <button
            v-if="filterSets.length > 1"
            type="button"
            class="layer-remove-btn"
            title="Remove layer"
            @click.stop="removeFilterSet(set.id)"
          >
            <i class="pi pi-times"></i>
          </button>
        </button>
      </div>

      <label v-if="filterSets.length > 1 && activeFilterSet" class="layer-visibility">
        <input type="checkbox" v-model="activeFilterSet.visible" @change="onLayerVisibilityChange" />
        Show {{ activeFilterSet.label }} on map
      </label>
      
      <div v-if="activeFilterSet" class="filters-content">
        <div class="filter-group">
          <label>Sampling trip</label>
          <div class="select-wrapper">
            <select v-model="activeFilterSet.trip" @change="fetchMarkersForSet(activeFilterSet)">
              <option value="">All trips</option>
              <option v-for="trip in filterOptions.trips" :key="trip" :value="trip">{{ trip }}</option>
            </select>
            <i class="pi pi-chevron-down select-icon"></i>
          </div>
        </div>

        <template v-if="!activeFilterSet.showAdvanced">
          <div class="filter-group">
            <label>River System</label>
            <div class="select-wrapper">
              <select v-model="activeFilterSet.selectedFilters.riverName" @change="fetchMarkersForSet(activeFilterSet)">
                <option value="">All Rivers</option>
                <option v-for="river in filterOptions.rivers" :key="river" :value="river">
                  {{ river }}
                </option>
              </select>
              <i class="pi pi-chevron-down select-icon"></i>
            </div>
          </div>

          <div class="filter-group">
            <label>Organism</label>
            <div class="select-wrapper">
              <select v-model="activeFilterSet.selectedFilters.organism" @change="fetchMarkersForSet(activeFilterSet)">
                <option value="">All Organisms</option>
                <option v-for="org in filterOptions.organisms" :key="org" :value="org">
                  {{ org }}
                </option>
              </select>
              <i class="pi pi-chevron-down select-icon"></i>
            </div>
          </div>

          <div class="filter-group">
            <label>SIR Profile</label>
            <div class="select-wrapper">
              <select v-model="activeFilterSet.selectedFilters.sirProfile" @change="fetchMarkersForSet(activeFilterSet)">
                <option value="">All Profiles</option>
                <option v-for="sir in filterOptions.sirProfiles" :key="sir" :value="sir">
                  {{ sir }}
                </option>
              </select>
              <i class="pi pi-chevron-down select-icon"></i>
            </div>
          </div>
        </template>

        <div v-else class="advanced-filters">
          <div class="filter-group">
            <div class="advanced-group-header">
              <label>River Systems</label>
              <button type="button" class="link-btn" @click="clearAdvancedGroup(activeFilterSet, 'rivers')">Clear</button>
            </div>
            <div class="checkbox-list">
              <label
                v-for="river in filterOptions.rivers"
                :key="river"
                class="checkbox-item"
              >
                <input
                  type="checkbox"
                  :value="river"
                  v-model="activeFilterSet.advancedFilters.rivers"
                  @change="fetchMarkersForSet(activeFilterSet)"
                />
                <span>{{ river }}</span>
              </label>
            </div>
            <p v-if="activeFilterSet.advancedFilters.rivers.length === 0" class="filter-hint">No selection = all rivers</p>
          </div>

          <div class="filter-group">
            <div class="advanced-group-header">
              <label>Organisms</label>
              <button type="button" class="link-btn" @click="clearAdvancedGroup(activeFilterSet, 'organisms')">Clear</button>
            </div>
            <div class="checkbox-list">
              <label
                v-for="org in filterOptions.organisms"
                :key="org"
                class="checkbox-item"
              >
                <input
                  type="checkbox"
                  :value="org"
                  v-model="activeFilterSet.advancedFilters.organisms"
                  @change="fetchMarkersForSet(activeFilterSet)"
                />
                <span>{{ org }}</span>
              </label>
            </div>
            <p v-if="activeFilterSet.advancedFilters.organisms.length === 0" class="filter-hint">No selection = all organisms</p>
          </div>

          <div class="filter-group">
            <div class="advanced-group-header">
              <label>SIR Profiles</label>
              <button type="button" class="link-btn" @click="clearAdvancedGroup(activeFilterSet, 'sirProfiles')">Clear</button>
            </div>
            <div class="checkbox-list">
              <label
                v-for="sir in filterOptions.sirProfiles"
                :key="sir"
                class="checkbox-item"
              >
                <input
                  type="checkbox"
                  :value="sir"
                  v-model="activeFilterSet.advancedFilters.sirProfiles"
                  @change="fetchMarkersForSet(activeFilterSet)"
                />
                <span>{{ sir }}</span>
              </label>
            </div>
            <p v-if="activeFilterSet.advancedFilters.sirProfiles.length === 0" class="filter-hint">No selection = all profiles</p>
          </div>
        </div>

        <button
          type="button"
          class="advanced-toggle-btn"
          :class="{ 'advanced-toggle-btn--active': activeFilterSet.showAdvanced }"
          @click="toggleAdvanced(activeFilterSet)"
        >
          <i class="pi" :class="activeFilterSet.showAdvanced ? 'pi-sliders-h' : 'pi-sliders-v'"></i>
          {{ activeFilterSet.showAdvanced ? 'Simple filters' : 'Advanced options' }}
          <span
            v-if="advancedSelectionCount(activeFilterSet) > 0 && !activeFilterSet.showAdvanced"
            class="advanced-badge"
            :style="{ background: activeFilterSet.color }"
          >
            {{ advancedSelectionCount(activeFilterSet) }}
          </span>
        </button>

        <button
          v-if="filterSets.length < MAX_LAYERS"
          type="button"
          class="add-layer-btn"
          @click="addFilterSet"
        >
          <i class="pi pi-plus"></i> Add comparison layer
        </button>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="shareToast" class="share-toast">{{ shareToast }}</div>
    </Transition>

    <Transition name="slide-fade">
      <div v-if="siteSummary" class="summary-panel">
        <button class="close-btn" @click="siteSummary = null"><i class="pi pi-times"></i></button>
        
        <div class="summary-content">
          <div class="summary-header-info">
            <span class="badge">SITE ID: {{ siteSummary.siteId }}</span>
            <h3>{{ siteSummary.locationName }}</h3>
          </div>

          <div
            v-if="siteSummary.riskLevel"
            class="safety-banner"
            :style="{ borderColor: siteSummary.riskColor, background: `color-mix(in srgb, ${siteSummary.riskColor} 12%, transparent)` }"
          >
            <span class="safety-level" :style="{ color: siteSummary.riskColor }">{{ siteSummary.riskLevel }}</span>
            <p class="safety-headline">{{ siteSummary.safetyHeadline }}</p>
            <p class="safety-detail">{{ siteSummary.safetyDetail }}</p>
          </div>

          <div class="summary-stats-grid">
            <div class="stat-box">
              <i class="pi pi-water"></i>
              <span class="stat-label">River System</span>
              <span class="stat-value">{{ siteSummary.riverName }}</span>
            </div>
            <div class="stat-box">
              <i class="pi pi-box"></i>
              <span class="stat-label">Total Samples</span>
              <span class="stat-value">{{ siteSummary.totalWaterSamples }}</span>
            </div>
            <div class="stat-box">
              <i class="pi pi-shield"></i>
              <span class="stat-label">Resistant isolates</span>
              <span class="stat-value">{{ siteSummary.resistantPercent }}% ({{ siteSummary.resistantCount }}/{{ siteSummary.totalWgs }})</span>
            </div>
            <div class="stat-box">
              <i class="pi pi-chart-line"></i>
              <span class="stat-label">Latest pH / DO</span>
              <span class="stat-value">{{ formatPhDo(siteSummary) }}</span>
            </div>
            <div class="stat-box full-width">
              <i class="pi pi-calendar"></i>
              <span class="stat-label">Last Sampled Date</span>
              <span class="stat-value">{{ siteSummary.lastSampledDate }}</span>
            </div>
          </div>
          
          <h4><i class="pi pi-microbe"></i> Detected Organisms</h4>
          <ul v-if="siteSummary.detectedOrganisms.length > 0" class="organisms-list">
            <li v-for="org in siteSummary.detectedOrganisms" :key="org">
              <a href="#" @click.prevent="goToBacteriaDetails(org)" class="organism-link">
                {{ org }} <i class="pi pi-external-link" style="font-size: 0.75rem; margin-left: 4px;"></i>
              </a>
            </li>
          </ul>
          <p v-else class="empty-state">No organisms isolated at this site yet.</p>

          <div class="summary-actions">
            <button type="button" class="secondary-btn" @click="addToCompare(siteSummary.siteId)">
              <i class="pi pi-plus"></i> {{ compareButtonLabel(siteSummary.siteId) }}
            </button>
            <button type="button" class="details-btn" @click="goToRiverDetails(siteSummary.siteId)">
              <i class="pi pi-chart-line"></i> View River Analytics
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import L from 'leaflet';
import { LMap, LTileLayer, LMarker, LTooltip, LControl, LPolyline } from '@vue-leaflet/vue-leaflet';
import {
  decodeMapState,
  encodeMapState,
  type MapLayerState,
  type MapShareState,
  type PinColorMode,
} from '@/utils/mapShareState';

const router = useRouter();
const route = useRoute();
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

const MAX_LAYERS = 3;
const LAYER_COLORS = ['#2563EB', '#DC2626', '#059669'];
const RISK_LEGEND = [
  { level: 'HIGH RISK', color: '#DC2626' },
  { level: 'CAUTION', color: '#D97706' },
  { level: 'LOW ADVISORY', color: '#2563EB' },
  { level: 'LOW RISK', color: '#059669' },
] as const;

interface SiteMarker {
  siteId: string;
  locationName: string;
  riverName: string;
  latitude: number;
  longitude: number;
  riskLevel: string;
  riskColor: string;
  resistantPercent: number;
  totalWgs: number;
}

interface SiteSummary {
  siteId: string;
  locationName: string;
  riverName: string;
  totalWaterSamples: number;
  detectedOrganisms: string[];
  lastSampledDate: string;
  riskLevel: string;
  riskColor: string;
  safetyHeadline: string;
  safetyDetail: string;
  resistantPercent: number;
  resistantCount: number;
  totalWgs: number;
  latestPh: number | null;
  latestDissolvedOxygen: number | null;
}

interface FilterSet {
  id: string;
  label: string;
  color: string;
  trip: string;
  selectedFilters: { riverName: string; organism: string; sirProfile: string };
  advancedFilters: { rivers: string[]; organisms: string[]; sirProfiles: string[] };
  showAdvanced: boolean;
  markers: SiteMarker[];
  visible: boolean;
}

interface MapDisplayMarker extends SiteMarker {
  mapKey: string;
  layerLabels: string[];
  icon: L.Icon;
}

let layerIdCounter = 0;

function createFilterSet(index: number): FilterSet {
  layerIdCounter += 1;
  return {
    id: `layer-${layerIdCounter}`,
    label: `Layer ${index + 1}`,
    color: LAYER_COLORS[index % LAYER_COLORS.length]!,
    trip: '',
    selectedFilters: { riverName: '', organism: '', sirProfile: '' },
    advancedFilters: { rivers: [], organisms: [], sirProfiles: [] },
    showAdvanced: false,
    markers: [],
    visible: true,
  };
}

const iconCache = new Map<string, L.DivIcon>();

function createPinIcon(color: string): L.Icon {
  const cached = iconCache.get(color);
  if (cached) return cached as L.Icon;

  const icon = L.divIcon({
    className: 'map-color-pin',
    html: `<div class="pin-dot" style="background-color:${color}"></div>`,
    iconSize: [18, 18],
    iconAnchor: [9, 9],
  });
  iconCache.set(color, icon);
  return icon as L.Icon;
}

function createOverlapIcon(colors: string[]): L.Icon {
  const key = `overlap:${colors.join('|')}`;
  const cached = iconCache.get(key);
  if (cached) return cached as L.Icon;

  const segment = 360 / colors.length;
  const stops = colors
    .map((color, index) => {
      const start = index * segment;
      const end = (index + 1) * segment;
      return `${color} ${start}deg ${end}deg`;
    })
    .join(', ');

  const icon = L.divIcon({
    className: 'map-color-pin',
    html: `<div class="pin-dot pin-dot--overlap" style="background:conic-gradient(${stops})"></div>`,
    iconSize: [20, 20],
    iconAnchor: [10, 10],
  });
  iconCache.set(key, icon);
  return icon as L.Icon;
}

const mapRef = ref<InstanceType<typeof LMap> | null>(null);
const leafletMap = ref<L.Map | null>(null);
const zoom = ref(10);
const center = ref<[number, number]>([ -25.747, 28.229 ]);
const pinColorMode = ref<PinColorMode>('layer');
const showRiverLines = ref(true);
const shareToast = ref('');
const compareSlots = ref<[string | null, string | null]>([null, null]);

const filterOptions = reactive<{ rivers: string[]; organisms: string[]; sirProfiles: string[]; trips: string[] }>({
  rivers: [],
  organisms: [],
  sirProfiles: [],
  trips: [],
});
const filterSets = ref<FilterSet[]>([createFilterSet(0)]);
const activeFilterSetId = ref(filterSets.value[0]!.id);
const siteSummary = ref<SiteSummary | null>(null);
const siteNameById = ref<Record<string, string>>({});
const urlSyncEnabled = ref(false);

const activeFilterSet = computed(() =>
  filterSets.value.find((set) => set.id === activeFilterSetId.value) ?? filterSets.value[0],
);

const mapDisplayMarkers = computed<MapDisplayMarker[]>(() => {
  const bySite = new Map<string, { marker: SiteMarker; labels: string[]; layerColors: string[] }>();

  for (const set of filterSets.value) {
    if (!set.visible) continue;
    for (const marker of set.markers) {
      const existing = bySite.get(marker.siteId);
      if (existing) {
        existing.labels.push(set.label);
        existing.layerColors.push(set.color);
      } else {
        bySite.set(marker.siteId, {
          marker,
          labels: [set.label],
          layerColors: [set.color],
        });
      }
    }
  }

  return Array.from(bySite.entries()).map(([siteId, { marker, labels, layerColors }]) => {
    const displayColors =
      pinColorMode.value === 'risk'
        ? [marker.riskColor || '#2563EB']
        : layerColors.length > 1
          ? layerColors
          : [layerColors[0]!];

    return {
      ...marker,
      mapKey: `${siteId}-${pinColorMode.value}-${displayColors.join('|')}`,
      layerLabels: labels,
      icon: displayColors.length > 1 ? createOverlapIcon(displayColors) : createPinIcon(displayColors[0]!),
    };
  });
});

const riverPolylines = computed(() => {
  if (!showRiverLines.value) return [];
  const byRiver = new Map<string, SiteMarker[]>();

  for (const set of filterSets.value) {
    if (!set.visible) continue;
    for (const marker of set.markers) {
      const list = byRiver.get(marker.riverName) ?? [];
      list.push(marker);
      byRiver.set(marker.riverName, list);
    }
  }

  return Array.from(byRiver.entries()).map(([river, markers]) => {
    const unique = [...new Map(markers.map((m) => [m.siteId, m])).values()];
    unique.sort((a, b) => a.latitude - b.latitude);
    return {
      river,
      latlngs: unique.map((m) => [m.latitude, m.longitude] as [number, number]),
      color: '#64748b',
    };
  });
});

const canOpenComparison = computed(
  () => compareSlots.value[0] && compareSlots.value[1] && compareSlots.value[0] !== compareSlots.value[1],
);

function advancedSelectionCount(set: FilterSet) {
  return set.advancedFilters.rivers.length + set.advancedFilters.organisms.length + set.advancedFilters.sirProfiles.length;
}

function setHasActiveFilters(set: FilterSet) {
  if (set.trip) return true;
  if (set.showAdvanced) {
    return advancedSelectionCount(set) > 0;
  }
  return set.selectedFilters.riverName !== '' || set.selectedFilters.organism !== '' || set.selectedFilters.sirProfile !== '';
}

const hasActiveFilters = computed(() => filterSets.value.some(setHasActiveFilters));

onMounted(async () => {
  await fetchFilterOptions();
  applyStateFromUrl();
  await fetchAllMarkers();
  urlSyncEnabled.value = true;
});

watch(
  () => [filterSets.value, pinColorMode.value, showRiverLines.value],
  () => {
    if (urlSyncEnabled.value) syncUrlFromState();
  },
  { deep: true },
);

async function fetchFilterOptions() {
  try {
    const res = await fetch(`${API_BASE}/map/filters`);
    if (res.ok) {
      const data = await res.json();
      filterOptions.rivers = data.rivers ?? [];
      filterOptions.organisms = data.organisms ?? [];
      filterOptions.sirProfiles = data.sirProfiles ?? [];
      filterOptions.trips = data.trips ?? [];
    }
  } catch (error) {
    console.error("Failed to load filters", error);
  }
}

function appendFilterParams(params: URLSearchParams, set: FilterSet) {
  if (set.trip) params.append('trip', set.trip);
  if (set.showAdvanced) {
    set.advancedFilters.rivers.forEach((r) => params.append('riverName', r));
    set.advancedFilters.organisms.forEach((o) => params.append('organism', o));
    set.advancedFilters.sirProfiles.forEach((s) => params.append('sirProfile', s));
  } else {
    if (set.selectedFilters.riverName) params.append('riverName', set.selectedFilters.riverName);
    if (set.selectedFilters.organism) params.append('organism', set.selectedFilters.organism);
    if (set.selectedFilters.sirProfile) params.append('sirProfile', set.selectedFilters.sirProfile);
  }
}

async function fetchMarkersForSet(set: FilterSet) {
  try {
    const params = new URLSearchParams();
    appendFilterParams(params, set);

    const res = await fetch(`${API_BASE}/map/markers?${params.toString()}`);
    if (res.ok) {
      const raw = await res.json();
      const markers: SiteMarker[] = raw.map((m: Partial<SiteMarker> & Pick<SiteMarker, 'siteId' | 'latitude' | 'longitude'>) => ({
        siteId: m.siteId,
        locationName: m.locationName ?? m.siteId,
        riverName: m.riverName ?? '',
        latitude: m.latitude,
        longitude: m.longitude,
        riskLevel: m.riskLevel ?? 'LOW RISK',
        riskColor: m.riskColor ?? '#059669',
        resistantPercent: m.resistantPercent ?? 0,
        totalWgs: m.totalWgs ?? 0,
      }));
      set.markers = markers;
      for (const m of markers) {
        siteNameById.value[m.siteId] = m.locationName;
      }
      siteSummary.value = null;
    }
  } catch (error) {
    console.error("Failed to fetch markers", error);
  }
}

async function fetchAllMarkers() {
  await Promise.all(filterSets.value.map((set) => fetchMarkersForSet(set)));
}

function resetAllFilters() {
  for (const set of filterSets.value) {
    set.trip = '';
    set.selectedFilters.riverName = '';
    set.selectedFilters.organism = '';
    set.selectedFilters.sirProfile = '';
    set.advancedFilters.rivers = [];
    set.advancedFilters.organisms = [];
    set.advancedFilters.sirProfiles = [];
    set.showAdvanced = false;
  }
  fetchAllMarkers();
}

function toggleAdvanced(set: FilterSet) {
  if (!set.showAdvanced) {
    set.advancedFilters.rivers = set.selectedFilters.riverName ? [set.selectedFilters.riverName] : [];
    set.advancedFilters.organisms = set.selectedFilters.organism ? [set.selectedFilters.organism] : [];
    set.advancedFilters.sirProfiles = set.selectedFilters.sirProfile ? [set.selectedFilters.sirProfile] : [];
    set.showAdvanced = true;
    fetchMarkersForSet(set);
    return;
  }

  set.selectedFilters.riverName = set.advancedFilters.rivers.length === 1 ? set.advancedFilters.rivers[0]! : '';
  set.selectedFilters.organism = set.advancedFilters.organisms.length === 1 ? set.advancedFilters.organisms[0]! : '';
  set.selectedFilters.sirProfile = set.advancedFilters.sirProfiles.length === 1 ? set.advancedFilters.sirProfiles[0]! : '';
  set.showAdvanced = false;
  fetchMarkersForSet(set);
}

function clearAdvancedGroup(set: FilterSet, group: 'rivers' | 'organisms' | 'sirProfiles') {
  set.advancedFilters[group] = [];
  fetchMarkersForSet(set);
}

function addFilterSet() {
  if (filterSets.value.length >= MAX_LAYERS) return;
  const newSet = createFilterSet(filterSets.value.length);
  filterSets.value.push(newSet);
  activeFilterSetId.value = newSet.id;
  fetchMarkersForSet(newSet);
}

function removeFilterSet(id: string) {
  if (filterSets.value.length <= 1) return;
  filterSets.value = filterSets.value.filter((set) => set.id !== id);
  if (activeFilterSetId.value === id) {
    activeFilterSetId.value = filterSets.value[0]!.id;
  }
  siteSummary.value = null;
}

function onLayerVisibilityChange() {
  siteSummary.value = null;
}

async function fetchSiteSummary(siteId: string) {
  try {
    const res = await fetch(`${API_BASE}/map/sites/${siteId}/summary`);
    if (res.ok) {
      const data = await res.json();
      siteSummary.value = {
        ...data,
        riskLevel: data.riskLevel ?? 'LOW RISK',
        riskColor: data.riskColor ?? '#059669',
        safetyHeadline: data.safetyHeadline ?? '',
        safetyDetail: data.safetyDetail ?? '',
        resistantPercent: data.resistantPercent ?? 0,
        resistantCount: data.resistantCount ?? 0,
        totalWgs: data.totalWgs ?? 0,
        latestPh: data.latestPh ?? null,
        latestDissolvedOxygen: data.latestDissolvedOxygen ?? null,
      };
      siteNameById.value[siteId] = data.locationName ?? siteId;
    }
  } catch (error) {
    console.error("Failed to load site summary", error);
  }
}

function goToRiverDetails(siteId: string) {
  router.push({ name: 'river', query: { siteId } });
}

function goToBacteriaDetails(organismName: string) {
  router.push({ name: 'bacteria-detail', params: { name: organismName } });
}

function setPinColorMode(mode: PinColorMode) {
  pinColorMode.value = mode;
}

function onMarkerClick(siteId: string) {
  fetchSiteSummary(siteId);
}

function onMapReady() {
  leafletMap.value = (mapRef.value as { leafletObject?: L.Map } | null)?.leafletObject ?? null;
}

function fitMapBounds() {
  const map = leafletMap.value;
  const markers = mapDisplayMarkers.value;
  if (!map || markers.length === 0) return;
  const bounds = L.latLngBounds(markers.map((m) => [m.latitude, m.longitude]));
  map.fitBounds(bounds, { padding: [48, 48], maxZoom: 13 });
}

function buildShareState(): MapShareState {
  return {
    pinMode: pinColorMode.value,
    showRiverLines: showRiverLines.value,
    layers: filterSets.value.map((set) => ({
      trip: set.trip,
      riverName: set.selectedFilters.riverName,
      organism: set.selectedFilters.organism,
      sirProfile: set.selectedFilters.sirProfile,
      showAdvanced: set.showAdvanced,
      rivers: [...set.advancedFilters.rivers],
      organisms: [...set.advancedFilters.organisms],
      sirProfiles: [...set.advancedFilters.sirProfiles],
      visible: set.visible,
    })),
  };
}

function applyStateFromUrl() {
  const encoded = route.query.s as string | undefined;
  const state = decodeMapState(encoded);
  if (!state?.layers?.length) return;

  pinColorMode.value = state.pinMode ?? 'layer';
  showRiverLines.value = state.showRiverLines ?? true;

  filterSets.value = state.layers.map((layer, index) => {
    const set = createFilterSet(index);
    set.trip = layer.trip ?? '';
    set.selectedFilters.riverName = layer.riverName ?? '';
    set.selectedFilters.organism = layer.organism ?? '';
    set.selectedFilters.sirProfile = layer.sirProfile ?? '';
    set.showAdvanced = layer.showAdvanced ?? false;
    set.advancedFilters.rivers = [...(layer.rivers ?? [])];
    set.advancedFilters.organisms = [...(layer.organisms ?? [])];
    set.advancedFilters.sirProfiles = [...(layer.sirProfiles ?? [])];
    set.visible = layer.visible ?? true;
    return set;
  });
  activeFilterSetId.value = filterSets.value[0]!.id;
}

function syncUrlFromState() {
  const encoded = encodeMapState(buildShareState());
  router.replace({ query: { ...route.query, s: encoded } });
}

async function copyShareLink() {
  const url = `${window.location.origin}${router.resolve({ name: 'map', query: { s: encodeMapState(buildShareState()) } }).href}`;
  try {
    await navigator.clipboard.writeText(url);
    shareToast.value = 'Link copied!';
    setTimeout(() => { shareToast.value = ''; }, 2500);
  } catch {
    shareToast.value = 'Could not copy link';
  }
}

function slotLabel(siteId: string) {
  return siteNameById.value[siteId] ? `${siteId} — ${siteNameById.value[siteId]}` : siteId;
}

function addToCompare(siteId: string) {
  if (compareSlots.value[0] === siteId || compareSlots.value[1] === siteId) return;
  if (!compareSlots.value[0]) {
    compareSlots.value[0] = siteId;
    return;
  }
  if (!compareSlots.value[1]) {
    compareSlots.value[1] = siteId;
    return;
  }
  compareSlots.value[1] = siteId;
}

function compareButtonLabel(siteId: string) {
  if (compareSlots.value[0] === siteId || compareSlots.value[1] === siteId) return 'Added to comparison';
  return 'Add to comparison';
}

function clearCompareSlot(index: 0 | 1) {
  compareSlots.value[index] = null;
}

function clearCompare() {
  compareSlots.value = [null, null];
}

function openComparison() {
  if (!canOpenComparison.value) return;
  router.push({
    name: 'compare',
    query: { siteA: compareSlots.value[0]!, siteB: compareSlots.value[1]! },
  });
}

function formatPhDo(summary: SiteSummary) {
  const ph = summary.latestPh != null ? summary.latestPh.toFixed(1) : '—';
  const dO = summary.latestDissolvedOxygen != null ? `${summary.latestDissolvedOxygen.toFixed(1)} mg/L` : '—';
  return `${ph} / ${dO}`;
}
</script>

<style scoped>
.map-view-container {
  display: flex;
  height: calc(100vh - 60px);
  position: relative;
  background: var(--c-bg);
  overflow: hidden;
}

.map-wrapper {
  flex: 1;
  width: 100%;
  height: 100%;
  z-index: 1;
}

/* --- Floating Filters Panel --- */
.floating-filters-panel {
  position: absolute;
  top: 20px;
  left: 60px;
  background: color-mix(in srgb, var(--c-card) 90%, transparent);
  backdrop-filter: blur(8px);
  padding: 16px 20px;
  border-radius: 12px;
  border: 1px solid var(--c-border);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  color: var(--c-text);
  width: auto;
  min-width: 300px;
  max-width: 340px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
}

.filters-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 2px solid var(--c-brand);
  padding-bottom: 8px;
}

.filters-header h2 {
  margin: 0;
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  font-weight: 700;
  color: var(--c-heading);
  display: flex;
  align-items: center;
  gap: 8px;
}

.reset-btn {
  background: transparent;
  border: none;
  color: var(--c-text-muted);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  border-radius: 4px;
  transition: all 0.2s ease;
}

.reset-btn:hover {
  background: var(--c-brand-dim);
  color: var(--c-brand);
}

.layer-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.layer-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 6px;
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  font-weight: 600;
  color: var(--c-text-muted);
  cursor: pointer;
  transition: all 0.2s ease;
}

.layer-tab:hover {
  border-color: var(--c-text-muted);
}

.layer-tab--active {
  background: var(--c-card);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.layer-tab-label {
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layer-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.8);
}

.layer-count {
  font-size: 10px;
  font-weight: 700;
  color: var(--c-text-muted);
  background: var(--c-bg);
  padding: 1px 5px;
  border-radius: 8px;
}

.layer-remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  padding: 0;
  margin-left: 2px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--c-text-muted);
  cursor: pointer;
  font-size: 9px;
}

.layer-remove-btn:hover {
  background: var(--c-red-dim);
  color: var(--c-red);
}

.layer-visibility {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  color: var(--c-text-muted);
  cursor: pointer;
}

.layer-visibility input {
  accent-color: var(--c-brand);
}

.filters-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-group label {
  font-family: 'DM Sans', sans-serif;
  font-size: 10px;
  font-weight: 600;
  color: var(--c-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.select-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.select-wrapper select {
  width: 100%;
  appearance: none;
  padding: 8px 32px 8px 12px;
  background: var(--c-bg);
  color: var(--c-text);
  border: 1px solid var(--c-border);
  border-radius: 6px;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  transition: all 0.2s ease;
  cursor: pointer;
}

.select-wrapper select:hover {
  border-color: var(--c-brand);
}

.select-wrapper select:focus {
  outline: none;
  border-color: var(--c-brand);
  box-shadow: 0 0 0 3px var(--c-brand-dim);
}

.select-icon {
  position: absolute;
  right: 12px;
  font-size: 10px;
  color: var(--c-text-muted);
  pointer-events: none;
}

.advanced-toggle-btn,
.add-layer-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  padding: 8px 12px;
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
  border-radius: 6px;
}

.advanced-toggle-btn {
  margin-top: 4px;
  background: var(--c-bg);
  color: var(--c-text-muted);
  border: 1px dashed var(--c-border);
}

.advanced-toggle-btn:hover {
  border-color: var(--c-brand);
  color: var(--c-brand);
  background: var(--c-brand-dim);
}

.advanced-toggle-btn--active {
  border-style: solid;
  border-color: var(--c-brand);
  color: var(--c-brand);
  background: var(--c-brand-dim);
}

.add-layer-btn {
  background: var(--c-brand-dim);
  color: var(--c-brand);
  border: 1px solid color-mix(in srgb, var(--c-brand) 30%, transparent);
}

.add-layer-btn:hover {
  background: var(--c-brand);
  color: white;
  border-color: var(--c-brand);
}

.advanced-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  color: white;
  font-size: 10px;
  font-weight: 700;
}

.advanced-filters {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 280px;
  overflow-y: auto;
  padding-right: 4px;
}

.advanced-group-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.link-btn {
  background: none;
  border: none;
  padding: 0;
  font-family: 'DM Sans', sans-serif;
  font-size: 10px;
  font-weight: 600;
  color: var(--c-brand);
  cursor: pointer;
}

.link-btn:hover {
  text-decoration: underline;
}

.checkbox-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 100px;
  overflow-y: auto;
  padding: 6px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  border-radius: 6px;
}

.checkbox-item {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  color: var(--c-text);
  cursor: pointer;
  padding: 4px 2px;
  border-radius: 4px;
}

.checkbox-item:hover {
  background: var(--c-brand-dim);
}

.checkbox-item input {
  margin-top: 2px;
  accent-color: var(--c-brand);
  flex-shrink: 0;
}

.filter-hint {
  margin: 4px 0 0;
  font-size: 10px;
  color: var(--c-text-muted);
  font-style: italic;
}

.tooltip-layers {
  font-size: 10px;
  color: var(--c-text-muted);
}

/* --- Live Stats Pill --- */
.custom-map-control {
  margin-bottom: 20px !important;
  margin-left: 20px !important;
}

.live-stats-pill {
  background: var(--c-card);
  padding: 8px 16px;
  border-radius: 20px;
  box-shadow: var(--c-shadow-md);
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  border: 1px solid var(--c-border);
}

.stats-layers {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stats-layer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
}

.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: var(--c-brand);
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(var(--c-brand-rgb), 0.7);
  animation: pulse 2s infinite;
  flex-shrink: 0;
}

@keyframes pulse {
  0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(0, 123, 255, 0.7); }
  70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(0, 123, 255, 0); }
  100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(0, 123, 255, 0); }
}

/* --- Slide-in Summary Panel --- */
.summary-panel {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 360px;
  max-height: calc(100vh - 100px);
  overflow-y: auto;
  background: var(--c-card);
  padding: 0;
  border-radius: 12px;
  border: 1px solid var(--c-border);
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.15);
  z-index: 1000;
}

.slide-fade-enter-active { transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-fade-leave-active { transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1); }
.slide-fade-enter-from, .slide-fade-leave-to {
  transform: translateX(50px);
  opacity: 0;
}

.close-btn {
  position: absolute;
  top: 16px;
  right: 16px;
  background: var(--c-bg);
  border: 1px solid var(--c-border);
  cursor: pointer;
  color: var(--c-text-dim);
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  transition: all 0.2s ease;
  z-index: 10;
}

.close-btn:hover {
  background: var(--c-brand);
  color: white;
  border-color: var(--c-brand);
  transform: rotate(90deg);
}

.summary-content {
  padding: 24px;
}

.summary-header-info {
  margin-bottom: 20px;
}

.badge {
  display: inline-block;
  background: var(--c-brand-dim);
  color: var(--c-brand);
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 9px;
  font-weight: 700;
  letter-spacing: 0.05em;
  margin-bottom: 8px;
}

.summary-content h3 {
  margin: 0;
  font-family: 'DM Sans', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--c-heading);
  line-height: 1.2;
}

.summary-stats-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px dashed var(--c-border);
}

.stat-box {
  background: var(--c-bg);
  padding: 12px;
  border-radius: 8px;
  border: 1px solid var(--c-border);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-box.full-width {
  grid-column: span 2;
}

.stat-box i {
  color: var(--c-brand);
  font-size: 14px;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 10px;
  color: var(--c-text-muted);
  text-transform: uppercase;
  font-weight: 600;
}

.stat-value {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-heading);
}

.summary-content h4 {
  margin: 0 0 12px 0;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-heading);
  display: flex;
  align-items: center;
  gap: 6px;
}

.organisms-list {
  list-style: none;
  padding: 0;
  margin: 0 0 24px 0;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.organisms-list li {
  background: var(--c-bg);
  border: 1px solid var(--c-border-strong);
  border-radius: 20px;
  transition: all 0.2s ease;
}

.organisms-list li:hover {
  border-color: var(--c-brand);
  background: var(--c-brand-dim);
}

.organism-link {
  display: inline-block;
  padding: 6px 12px;
  color: var(--c-text);
  text-decoration: none;
  font-size: 11px;
  font-weight: 500;
}

.empty-state {
  font-size: 12px;
  color: var(--c-text-muted);
  font-style: italic;
  margin-bottom: 24px;
}

.details-btn {
  width: 100%;
  padding: 12px;
  background: var(--c-brand);
  color: white;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.details-btn:hover {
  background: color-mix(in srgb, var(--c-brand) 85%, black);
  box-shadow: 0 4px 12px rgba(var(--c-brand-rgb), 0.3);
  transform: translateY(-2px);
}

.secondary-btn {
  width: 100%;
  padding: 10px;
  margin-bottom: 8px;
  background: var(--c-bg);
  color: var(--c-brand);
  border: 1px solid var(--c-brand);
  border-radius: 8px;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  transition: all 0.2s ease;
}

.secondary-btn:hover {
  background: var(--c-brand-dim);
}

.safety-banner {
  margin-bottom: 16px;
  padding: 12px;
  border-radius: 8px;
  border: 1px solid;
}

.safety-level {
  display: block;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.04em;
  margin-bottom: 6px;
}

.safety-headline {
  margin: 0 0 4px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-heading);
}

.safety-detail {
  margin: 0;
  font-size: 11px;
  color: var(--c-text-muted);
  line-height: 1.4;
}

.tooltip-risk {
  font-size: 10px;
  font-weight: 600;
}

.compare-bar {
  position: absolute;
  bottom: 24px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1001;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: center;
  max-width: calc(100% - 48px);
  padding: 10px 16px;
  background: color-mix(in srgb, var(--c-card) 95%, transparent);
  backdrop-filter: blur(8px);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

.compare-bar-label {
  font-size: 11px;
  font-weight: 700;
  color: var(--c-heading);
  display: flex;
  align-items: center;
  gap: 6px;
  white-space: nowrap;
}

.compare-slots {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.compare-slot {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 10px;
  background: var(--c-bg);
  border: 1px dashed var(--c-border);
  border-radius: 8px;
  font-size: 11px;
  color: var(--c-text-muted);
  min-width: 140px;
}

.compare-slot--filled {
  border-style: solid;
  color: var(--c-text);
}

.compare-slot-tag {
  font-weight: 700;
  color: var(--c-brand);
  font-size: 10px;
}

.compare-slot-clear {
  border: none;
  background: transparent;
  color: var(--c-text-muted);
  cursor: pointer;
  padding: 0;
  font-size: 10px;
}

.compare-go-btn {
  padding: 8px 14px;
  background: var(--c-brand);
  color: white;
  border: none;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.compare-go-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.compare-clear-all {
  padding: 8px 10px;
  background: transparent;
  border: none;
  color: var(--c-text-muted);
  font-size: 11px;
  font-weight: 600;
  cursor: pointer;
}

.share-toast {
  position: absolute;
  top: 80px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 1100;
  padding: 10px 18px;
  background: var(--c-card);
  border: 1px solid var(--c-brand);
  border-radius: 8px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-brand);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.25s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>

<style>
/* Leaflet controls (unscoped) */
.map-legend-control,
.map-toolbar-control {
  margin: 12px !important;
}

.map-legend {
  background: color-mix(in srgb, var(--c-card) 94%, transparent);
  backdrop-filter: blur(6px);
  padding: 10px 12px;
  border-radius: 10px;
  border: 1px solid var(--c-border);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
  min-width: 130px;
}

.map-legend-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 10px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--c-text-muted);
  margin-bottom: 8px;
}

.map-legend-mode {
  display: flex;
  gap: 4px;
  margin-bottom: 8px;
}

.map-legend-mode button {
  flex: 1;
  padding: 4px 6px;
  font-size: 10px;
  font-weight: 600;
  border: 1px solid var(--c-border);
  border-radius: 4px;
  background: var(--c-bg);
  color: var(--c-text-muted);
  cursor: pointer;
}

.map-legend-mode button.active {
  background: var(--c-brand-dim);
  border-color: var(--c-brand);
  color: var(--c-brand);
}

.legend-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  color: var(--c-text);
  margin-bottom: 4px;
}

.legend-row--muted {
  color: var(--c-text-muted);
}

.pin-dot--overlap-mini {
  background: conic-gradient(#2563eb 0deg 120deg, #dc2626 120deg 240deg, #059669 240deg 360deg);
}

.map-toolbar {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.map-toolbar button {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--c-border);
  border-radius: 8px;
  background: color-mix(in srgb, var(--c-card) 94%, transparent);
  color: var(--c-text);
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.map-toolbar button:hover,
.map-toolbar button.active {
  background: var(--c-brand-dim);
  border-color: var(--c-brand);
  color: var(--c-brand);
}
</style>

<style>
/* Leaflet divIcon pins (not scoped — rendered inside map pane) */
.map-color-pin {
  background: transparent !important;
  border: none !important;
}

.map-color-pin .pin-dot {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  border: 2px solid #fff;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.35);
}

.map-color-pin .pin-dot--overlap {
  width: 16px;
  height: 16px;
}
</style>
