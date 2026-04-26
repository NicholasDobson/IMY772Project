<template>
  <div class="map-view-container">
    
    <div class="map-wrapper">
      <l-map ref="map" v-model:zoom="zoom" :center="center" :use-global-leaflet="false" :zoomAnimation="true">
        <l-tile-layer
          url="https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
          layer-type="base"
          name="CartoDB Voyager"
          attribution="&copy; <a href='https://carto.com/'>CartoDB</a>"
        ></l-tile-layer>

        <l-marker
          v-for="marker in activeMarkers"
          :key="marker.siteId"
          :lat-lng="[marker.latitude, marker.longitude]"
          @click="fetchSiteSummary(marker.siteId)"
        >
          <l-tooltip>
            <strong>{{ marker.locationName }}</strong><br/>
            <span style="color: var(--c-text-muted)">{{ marker.riverName }}</span>
          </l-tooltip>
        </l-marker>

        <l-control position="bottomleft" class="custom-map-control">
          <div class="live-stats-pill">
            <span class="pulse-dot"></span>
            Showing <strong>{{ activeMarkers.length }}</strong> sites
          </div>
        </l-control>
      </l-map>
    </div>

    <div class="floating-filters-panel">
      <div class="filters-header">
        <h2><i class="pi pi-filter"></i> Map Filters</h2>
        <button 
          v-if="hasActiveFilters" 
          @click="resetFilters" 
          class="reset-btn" 
          title="Clear Filters"
        >
          <i class="pi pi-refresh"></i> Reset
        </button>
      </div>
      
      <div class="filters-content">
        <div class="filter-group">
          <label>River System</label>
          <div class="select-wrapper">
            <select v-model="selectedFilters.riverName" @change="fetchMarkers">
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
            <select v-model="selectedFilters.organism" @change="fetchMarkers">
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
            <select v-model="selectedFilters.sirProfile" @change="fetchMarkers">
              <option value="">All Profiles</option>
              <option v-for="sir in filterOptions.sirProfiles" :key="sir" :value="sir">
                {{ sir }}
              </option>
            </select>
            <i class="pi pi-chevron-down select-icon"></i>
          </div>
        </div>
      </div>
    </div>

    <Transition name="slide-fade">
      <div v-if="siteSummary" class="summary-panel">
        <button class="close-btn" @click="siteSummary = null"><i class="pi pi-times"></i></button>
        
        <div class="summary-content">
          <div class="summary-header-info">
            <span class="badge">SITE ID: {{ siteSummary.siteId }}</span>
            <h3>{{ siteSummary.locationName }}</h3>
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
import { ref, reactive, onMounted, computed } from 'vue';
import { useRouter } from 'vue-router';
// Notice the newly imported LTooltip and LControl
import { LMap, LTileLayer, LMarker, LTooltip, LControl } from '@vue-leaflet/vue-leaflet';

const router = useRouter();
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// Map State
const zoom = ref(10);
const center = ref<[number, number]>([ -25.747, 28.229 ]); 

// Data State
const filterOptions = reactive({ rivers: [], organisms: [], sirProfiles: [] });
const selectedFilters = reactive({ riverName: '', organism: '', sirProfile: '' });
const activeMarkers = ref<any[]>([]);
const siteSummary = ref<any>(null);

// Computed property to check if user has applied any filters
const hasActiveFilters = computed(() => {
  return selectedFilters.riverName !== '' || selectedFilters.organism !== '' || selectedFilters.sirProfile !== '';
});

onMounted(async () => {
  await fetchFilterOptions();
  await fetchMarkers();
});

async function fetchFilterOptions() {
  try {
    const res = await fetch(`${API_BASE}/map/filters`);
    if (res.ok) {
      const data = await res.json();
      filterOptions.rivers = data.rivers;
      filterOptions.organisms = data.organisms;
      filterOptions.sirProfiles = data.sirProfiles;
    }
  } catch (error) {
    console.error("Failed to load filters", error);
  }
}

async function fetchMarkers() {
  try {
    const params = new URLSearchParams();
    if (selectedFilters.riverName) params.append('riverName', selectedFilters.riverName);
    if (selectedFilters.organism) params.append('organism', selectedFilters.organism);
    if (selectedFilters.sirProfile) params.append('sirProfile', selectedFilters.sirProfile);

    const res = await fetch(`${API_BASE}/map/markers?${params.toString()}`);
    if (res.ok) {
      activeMarkers.value = await res.json();
      // If we fetch markers, we should close the panel as the old site might not be in the new filtered list
      siteSummary.value = null; 
    }
  } catch (error) {
    console.error("Failed to fetch markers", error);
  }
}

async function fetchSiteSummary(siteId: string) {
  try {
    const res = await fetch(`${API_BASE}/map/sites/${siteId}/summary`);
    if (res.ok) {
      siteSummary.value = await res.json();
    }
  } catch (error) {
    console.error("Failed to load site summary", error);
  }
}

function resetFilters() {
  selectedFilters.riverName = '';
  selectedFilters.organism = '';
  selectedFilters.sirProfile = '';
  fetchMarkers();
}

function goToRiverDetails(siteId: string) {
  router.push({ name: 'river', query: { siteId } });
}

function goToBacteriaDetails(organismName: string) {
  router.push({ name: 'bacteria-detail', params: { name: organismName } });
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
  left: 60px; /* Offset from map controls */
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
  display: flex;
  flex-direction: column;
  gap: 12px;
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

/* Custom Select styling to look cleaner */
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

.pulse-dot {
  width: 8px;
  height: 8px;
  background-color: var(--c-brand);
  border-radius: 50%;
  box-shadow: 0 0 0 0 rgba(var(--c-brand-rgb), 0.7);
  animation: pulse 2s infinite;
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

/* Vue Transition Classes */
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
</style>