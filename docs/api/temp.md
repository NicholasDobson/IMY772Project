### 2. The `MapView.vue` Component

Here is your `MapView.vue` file using Vue 3 Composition API (`<script setup>`). It handles fetching the dynamic dropdown options, rendering the active map pins, re-fetching markers whenever a filter changes, and pulling the summary panel data when a pin is clicked.

I have set up a placeholder grid for the map area where your frontend team can easily drop in the Mapbox or Google Maps SDK later.

**`frontend/src/views/MapView.vue`**

```vue
<template>
  <div class="map-page-container">
    
    <header class="filter-bar">
      <h3>Map Filters</h3>
      
      <div class="filters">
        <select v-model="activeFilters.riverName" @change="fetchMarkers">
          <option value="">All Rivers</option>
          <option v-for="river in filterOptions.rivers" :key="river" :value="river">
            {{ river }}
          </option>
        </select>

        <select v-model="activeFilters.organism" @change="fetchMarkers">
          <option value="">All Organisms</option>
          <option v-for="org in filterOptions.organisms" :key="org" :value="org">
            {{ org }}
          </option>
        </select>

        <select v-model="activeFilters.sirProfile" @change="fetchMarkers">
          <option value="">All SIR Profiles</option>
          <option v-for="profile in filterOptions.sirProfiles" :key="profile" :value="profile">
            {{ profile }}
          </option>
        </select>
        
        <button @click="resetFilters">Reset</button>
      </div>
    </header>

    <div class="main-content">
      <main class="map-area">
        <div v-if="loadingMarkers" class="loading">Loading map data...</div>
        <div v-else class="marker-container">
          <p class="instruction">Select a site marker to view details:</p>
          
          <div class="mock-map-grid">
            <button 
              v-for="marker in markers" 
              :key="marker.siteId"
              class="map-pin-btn"
              :class="{ active: selectedSiteId === marker.siteId }"
              @click="fetchSiteSummary(marker.siteId)"
            >
              📍 {{ marker.siteId }} - {{ marker.locationName }}
              <small>({{ marker.latitude }}, {{ marker.longitude }})</small>
            </button>
          </div>
          <p v-if="markers.length === 0">No sites found matching the current filters.</p>
        </div>
      </main>

      <aside class="summary-panel" v-if="selectedSiteSummary">
        <h2>Site Details</h2>
        <div class="summary-card">
          <p><strong>Site ID:</strong> {{ selectedSiteSummary.siteId }}</p>
          <p><strong>Location:</strong> {{ selectedSiteSummary.locationName }}</p>
          <p><strong>River:</strong> {{ selectedSiteSummary.riverName }}</p>
          <p><strong>Total Samples:</strong> {{ selectedSiteSummary.totalWaterSamples }}</p>
          <p><strong>Last Sampled:</strong> {{ selectedSiteSummary.lastSampledDate }}</p>
          
          <div class="organisms-list">
            <strong>Detected Organisms:</strong>
            <ul v-if="selectedSiteSummary.detectedOrganisms.length > 0">
              <li v-for="org in selectedSiteSummary.detectedOrganisms" :key="org">{{ org }}</li>
            </ul>
            <span v-else> None</span>
          </div>
        </div>
        <button class="close-btn" @click="selectedSiteSummary = null; selectedSiteId = null">Close Panel</button>
      </aside>
    </div>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue';

// Base URL - ensure this matches your Spring Boot port
const API_BASE_URL = 'http://localhost:8080/api/v1/map';

// State
const filterOptions = reactive({ rivers: [], organisms: [], sirProfiles: [] });
const activeFilters = reactive({ riverName: '', organism: '', sirProfile: '' });
const markers = ref([]);
const loadingMarkers = ref(false);
const selectedSiteId = ref<string | null>(null);
const selectedSiteSummary = ref<any>(null);

// 1. Fetch the dropdown options on mount
const fetchFilterOptions = async () => {
  try {
    const response = await fetch(`${API_BASE_URL}/filters`);
    const data = await response.json();
    filterOptions.rivers = data.rivers || [];
    filterOptions.organisms = data.organisms || [];
    filterOptions.sirProfiles = data.sirProfiles || [];
  } catch (error) {
    console.error("Failed to load filter options:", error);
  }
};

// 2. Fetch the markers based on selected filters
const fetchMarkers = async () => {
  loadingMarkers.value = true;
  selectedSiteSummary.value = null; // Close panel when filters change
  selectedSiteId.value = null;

  try {
    // Construct query string, ignoring empty filters
    const params = new URLSearchParams();
    if (activeFilters.riverName) params.append('riverName', activeFilters.riverName);
    if (activeFilters.organism) params.append('organism', activeFilters.organism);
    if (activeFilters.sirProfile) params.append('sirProfile', activeFilters.sirProfile);

    const response = await fetch(`${API_BASE_URL}/markers?${params.toString()}`);
    markers.value = await response.json();
  } catch (error) {
    console.error("Failed to load map markers:", error);
  } finally {
    loadingMarkers.value = false;
  }
};

// 3. Fetch specific site summary when a pin is clicked
const fetchSiteSummary = async (siteId: string) => {
  selectedSiteId.value = siteId;
  selectedSiteSummary.value = null;

  try {
    const response = await fetch(`${API_BASE_URL}/sites/${siteId}/summary`);
    if (response.ok) {
      selectedSiteSummary.value = await response.json();
    } else {
      console.error("Failed to fetch summary for site:", siteId);
    }
  } catch (error) {
    console.error("Network error fetching site summary:", error);
  }
};

// Reset utility
const resetFilters = () => {
  activeFilters.riverName = '';
  activeFilters.organism = '';
  activeFilters.sirProfile = '';
  fetchMarkers();
};

// Initialize
onMounted(() => {
  fetchFilterOptions();
  fetchMarkers(); // Fetch initial unfiltered map points
});
</script>

<style scoped>
/* Basic functional styling - Frontend team will overwrite this */
.map-page-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  padding: 20px;
}

.filter-bar {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  margin-bottom: 20px;
  border: 1px solid #ddd;
}

.filters {
  display: flex;
  gap: 15px;
  margin-top: 10px;
}

.filters select, .filters button {
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #ccc;
}

.main-content {
  display: flex;
  flex: 1;
  gap: 20px;
  overflow: hidden;
}

.map-area {
  flex: 1;
  background: #e9ecef;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #ddd;
  overflow-y: auto;
}

.mock-map-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-top: 15px;
}

.map-pin-btn {
  padding: 15px;
  background: white;
  border: 1px solid #ccc;
  border-radius: 6px;
  cursor: pointer;
  text-align: left;
  display: flex;
  flex-direction: column;
}

.map-pin-btn.active {
  border-color: #007bff;
  background: #e7f1ff;
}

.summary-panel {
  width: 350px;
  background: white;
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  box-shadow: -2px 0 5px rgba(0,0,0,0.05);
  overflow-y: auto;
}

.summary-card p {
  margin: 10px 0;
}

.close-btn {
  margin-top: 20px;
  width: 100%;
  padding: 10px;
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}
</style>
```