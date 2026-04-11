<template>
  <div class="map-view-container">
    <div class="filters-panel">
      <h2>Map Filters</h2>
      
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
      <h3>{{ siteSummary.locationName }}</h3>
      <p><strong>River:</strong> {{ siteSummary.riverName }}</p>
      <p><strong>Total Samples:</strong> {{ siteSummary.totalWaterSamples }}</p>
      <p><strong>Last Sampled:</strong> {{ siteSummary.lastSampledDate }}</p>
      
      <h4>Detected Organisms:</h4>
      <ul>
        <li v-for="org in siteSummary.detectedOrganisms" :key="org">{{ org }}</li>
      </ul>

      <div class="summary-actions">
        <button type="button" class="details-btn" @click="goToRiverDetails(siteSummary.siteId)">
          View River Details
        </button>
        <button type="button" class="bacteria-details-btn" @click="viewBacteriaDetails">
          View Bacteria Details
        </button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
// Import Leaflet components
import { LMap, LTileLayer, LMarker } from '@vue-leaflet/vue-leaflet';

const router = useRouter();
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1';

// Map State
const zoom = ref(10);
const center = ref([ -25.747, 28.229 ]); // Pretoria Coordinates [Lat, Lng]

// Data State
const filterOptions = reactive({ rivers: [], organisms: [], sirProfiles: [] });
const selectedFilters = reactive({ riverName: '', organism: '', sirProfile: '' });
const activeMarkers = ref<any[]>([]);
const siteSummary = ref<any>(null);

// Lifecycle
onMounted(async () => {
  await fetchFilterOptions();
  await fetchMarkers();
});

// Fetch Dropdown Options
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

// Fetch Markers based on Filters
async function fetchMarkers() {
  try {
    const params = new URLSearchParams();
    if (selectedFilters.riverName) params.append('riverName', selectedFilters.riverName);
    if (selectedFilters.organism) params.append('organism', selectedFilters.organism);
    if (selectedFilters.sirProfile) params.append('sirProfile', selectedFilters.sirProfile);

    const res = await fetch(`${API_BASE}/map/markers?${params.toString()}`);
    if (res.ok) {
      // Just assign the fetched DTOs to the array, Vue handles the reactivity and drawing!
      activeMarkers.value = await res.json();
    }
  } catch (error) {
    console.error("Failed to fetch markers", error);
  }
}

// Fetch specific site details when a marker is clicked
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

// Mocked Redirect
function goToRiverDetails(siteId: string) {
  alert(`Mock Redirect: In the future, this will navigate to /river-detail/${siteId}`);
  // Future implementation:
  // router.push({ name: 'RiverDetail', params: { id: siteId } });
}

function viewBacteriaDetails() {
  // Placeholder until bacteria details view is wired up.
}
</script>

<style scoped>
.map-view-container {
  display: flex;
  height: calc(100vh - 60px); /* Adjust based on your header height */
  position: relative;
}

.filters-panel {
  width: 250px;
  background: #f8f9fa;
  padding: 20px;
  border-right: 1px solid #ddd;
  z-index: 1000; /* Leaflet tiles have a high z-index, so this needs to be higher */
}

.filter-group {
  margin-bottom: 15px;
}

.filter-group select {
  width: 100%;
  padding: 8px;
  margin-top: 5px;
}

.map-wrapper {
  flex-grow: 1;
  width: 100%;
  height: 100%;
}

.summary-panel {
  position: absolute;
  top: 20px;
  right: 50px;
  width: 300px;
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  z-index: 1000; /* Keeps it above the map tiles */
}

.close-btn {
  float: right;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 16px;
}

.summary-actions {
  margin-top: 15px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.details-btn {
  width: 100%;
  padding: 10px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.details-btn:hover {
  background: #0056b3;
}

.bacteria-details-btn {
  width: 100%;
  padding: 10px;
  background: white;
  color: #007bff;
  border: 1px solid #007bff;
  border-radius: 4px;
  cursor: pointer;
}

.bacteria-details-btn:hover {
  background: #f0f7ff;
}
</style>