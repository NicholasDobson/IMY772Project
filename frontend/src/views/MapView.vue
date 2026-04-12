<template>
  <div class="map-view-container">
    <div class="filters-panel">
      <div class="filters-header">
        <h2>Map Filters</h2>
      </div>

      <div class="filters-content">
        <div class="filter-group">
          <label>River System</label>
          <select v-model="selectedFilters.riverName" @change="fetchMarkers">
            <option value="">All Rivers</option>
            <option v-for="river in filterOptions.rivers" :key="river" :value="river">
              {{ river }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label>Organism</label>
          <select v-model="selectedFilters.organism" @change="fetchMarkers">
            <option value="">All Organisms</option>
            <option v-for="org in filterOptions.organisms" :key="org" :value="org">
              {{ org }}
            </option>
          </select>
        </div>

        <div class="filter-group">
          <label>SIR Profile</label>
          <select v-model="selectedFilters.sirProfile" @change="fetchMarkers">
            <option value="">All Profiles</option>
            <option v-for="sir in filterOptions.sirProfiles" :key="sir" :value="sir">
              {{ sir }}
            </option>
          </select>
        </div>
      </div>
    </div>

    <div class="map-wrapper">
      <l-map ref="map" v-model:zoom="zoom" :center="center" :use-global-leaflet="false">
        <l-tile-layer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          layer-type="base"
          name="OpenStreetMap"
          attribution="&copy; <a href='https://www.openstreetmap.org/copyright'>OpenStreetMap</a> contributors"
        ></l-tile-layer>

        <l-marker
          v-for="marker in activeMarkers"
          :key="marker.siteId"
          :lat-lng="[marker.latitude, marker.longitude]"
          @click="fetchSiteSummary(marker.siteId)"
        >
        </l-marker>
      </l-map>
    </div>

    <div v-if="siteSummary" class="summary-panel">
      <button class="close-btn" @click="siteSummary = null">✖</button>
      <div class="summary-content">
        <h3>{{ siteSummary.locationName }}</h3>
        <div class="summary-detail">
          <strong>River:</strong> <span>{{ siteSummary.riverName }}</span>
        </div>
        <div class="summary-detail">
          <strong>Total Samples:</strong> <span>{{ siteSummary.totalWaterSamples }}</span>
        </div>
        <div class="summary-detail">
          <strong>Last Sampled:</strong> <span>{{ siteSummary.lastSampledDate }}</span>
        </div>

        <h4>Detected Organisms:</h4>
        <ul class="organisms-list">
          <li v-for="org in siteSummary.detectedOrganisms" :key="org">
            <a href="#" @click.prevent="goToBacteriaDetails(org)" class="organism-link">{{
              org
            }}</a>
          </li>
        </ul>

        <div class="summary-actions">
          <button type="button" class="details-btn" @click="goToRiverDetails(siteSummary.siteId)">
            View River Details
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
// Import Leaflet components
import { LMap, LTileLayer, LMarker } from '@vue-leaflet/vue-leaflet'

const router = useRouter()
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

// Map State
const zoom = ref(10)
const center = ref([-25.747, 28.229]) // Pretoria Coordinates [Lat, Lng]

// Data State
const filterOptions = reactive({ rivers: [], organisms: [], sirProfiles: [] })
const selectedFilters = reactive({ riverName: '', organism: '', sirProfile: '' })
const activeMarkers = ref<Record<string, unknown>[]>([])
const siteSummary = ref<Record<string, unknown> | null>(null)

// Lifecycle
onMounted(async () => {
  await fetchFilterOptions()
  await fetchMarkers()
})

// Fetch Dropdown Options
async function fetchFilterOptions() {
  try {
    const res = await fetch(`${API_BASE}/map/filters`)
    if (res.ok) {
      const data = await res.json()
      filterOptions.rivers = data.rivers
      filterOptions.organisms = data.organisms
      filterOptions.sirProfiles = data.sirProfiles
    }
  } catch (error) {
    console.error('Failed to load filters', error)
  }
}

// Fetch Markers based on Filters
async function fetchMarkers() {
  try {
    const params = new URLSearchParams()
    if (selectedFilters.riverName) params.append('riverName', selectedFilters.riverName)
    if (selectedFilters.organism) params.append('organism', selectedFilters.organism)
    if (selectedFilters.sirProfile) params.append('sirProfile', selectedFilters.sirProfile)

    const res = await fetch(`${API_BASE}/map/markers?${params.toString()}`)
    if (res.ok) {
      // Just assign the fetched DTOs to the array, Vue handles the reactivity and drawing!
      activeMarkers.value = await res.json()
    }
  } catch (error) {
    console.error('Failed to fetch markers', error)
  }
}

// Fetch specific site details when a marker is clicked
async function fetchSiteSummary(siteId: string) {
  try {
    const res = await fetch(`${API_BASE}/map/sites/${siteId}/summary`)
    if (res.ok) {
      siteSummary.value = await res.json()
    }
  } catch (error) {
    console.error('Failed to load site summary', error)
  }
}

// Navigation functions
function goToRiverDetails(siteId: string) {
  router.push({ name: 'river-detail', params: { id: siteId } })
}

function goToBacteriaDetails(organismName: string) {
  router.push({ name: 'bacteria', params: { name: organismName } })
}
</script>

<style scoped>
.map-view-container {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 60px);
  position: relative;
  background: var(--c-bg);
}

.filters-panel {
  width: 100%;
  background: var(--c-card);
  padding: 16px 28px;
  border-bottom: 1px solid var(--c-border);
  z-index: 1000;
  color: var(--c-text);
}

.filters-header {
  margin-bottom: 12px;
  border-bottom: 2px solid var(--c-brand);
  padding-bottom: 8px;
}

.filters-header h2 {
  margin: 0;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-heading);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.filters-content {
  display: flex;
  gap: 24px;
  align-items: flex-end;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.filter-group label {
  font-family: 'DM Sans', sans-serif;
  font-size: 9.5px;
  font-weight: 600;
  color: var(--c-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.filter-group select {
  padding: 8px 12px;
  background: var(--c-input-bg);
  color: var(--c-text);
  border: 1px solid var(--c-border);
  border-radius: 6px;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  transition: all 0.2s ease;
  min-width: 180px;
}

.filter-group select:hover {
  border-color: var(--c-brand);
}

.filter-group select:focus {
  outline: none;
  border-color: var(--c-brand);
  box-shadow: 0 0 0 3px var(--c-brand-dim);
}

.filter-group select option {
  background: var(--c-input-bg);
  color: var(--c-text);
}

.map-wrapper {
  flex: 1;
  width: 100%;
  height: 100%;
}

.summary-panel {
  position: absolute;
  top: 24px;
  right: 24px;
  width: 340px;
  background: var(--c-card);
  padding: 0;
  border-radius: 8px;
  border: 1px solid var(--c-border);
  box-shadow: var(--c-shadow-md);
  z-index: 1000;
  overflow: hidden;
}

.close-btn {
  position: absolute;
  top: 12px;
  right: 12px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 20px;
  color: var(--c-text-dim);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: all 0.2s ease;
  z-index: 10;
}

.close-btn:hover {
  background: var(--c-brand-dim);
  color: var(--c-text);
}

.summary-content {
  padding: 24px;
  color: var(--c-text);
}

.summary-content h3 {
  margin: 0 0 18px 0;
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-heading);
  border-bottom: 2px solid var(--c-brand);
  padding-bottom: 12px;
}

.summary-detail {
  display: flex;
  justify-content: space-between;
  margin-bottom: 12px;
  font-family: 'DM Sans', sans-serif;
  font-size: 12px;
  color: var(--c-text);
}

.summary-detail strong {
  color: var(--c-text-muted);
  font-weight: 600;
}

.summary-detail span {
  color: var(--c-text);
  text-align: right;
  flex: 1;
  margin-left: 8px;
}

.summary-content h4 {
  margin: 18px 0 10px 0;
  font-family: 'DM Sans', sans-serif;
  font-size: 9.5px;
  font-weight: 600;
  color: var(--c-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.1em;
}

.organisms-list {
  list-style: none;
  padding: 0;
  margin: 0 0 18px 0;
}

.organisms-list li {
  padding: 6px 10px;
  margin-bottom: 6px;
  background: var(--c-brand-dim);
  border-left: 3px solid var(--c-brand);
  font-family: 'DM Sans', sans-serif;
  font-size: 11px;
  color: var(--c-text);
  border-radius: 3px;
}

.organism-link {
  color: var(--c-brand);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.organism-link:hover {
  color: var(--c-brand-hover);
  text-decoration: underline;
}

.summary-actions {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.details-btn {
  width: 100%;
  padding: 10px 12px;
  background: var(--c-brand);
  color: white;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  transition: all 0.2s ease;
}

.details-btn:hover {
  opacity: 0.9;
  box-shadow: var(--c-shadow-md);
  transform: translateY(-2px);
}

.bacteria-details-btn {
  width: 100%;
  padding: 10px 12px;
  background: transparent;
  color: var(--c-brand);
  border: 1px solid var(--c-border);
  border-radius: 6px;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  font-size: 10px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  transition: all 0.2s ease;
}

.bacteria-details-btn:hover {
  background: var(--c-brand-dim);
  border-color: var(--c-brand);
  color: var(--c-heading);
}
</style>
