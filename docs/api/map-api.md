# Geospatial Map API Documentation

This document defines the REST endpoints used by the Interactive Geospatial Map on the frontend. The base URL for these endpoints is `/api/v1/map`.

---

## 1. Get Filter Options
Fetches the dynamic, unique lists of rivers, organisms, and SIR profiles directly from the database to populate the frontend filter dropdowns.

* **Endpoint:** `GET /filters`
* **Access:** Public / Authenticated
* **Response (200 OK):**
```json
{
  "rivers": ["Apies River", "Crocodile River"],
  "organisms": ["Klebsiella pneumoniae", "Serratia fonticola", "Escherichia coli"],
  "sirProfiles": ["Susceptible", "Intermediate", "Resistant"]
}
```

---

## 2. Get Filtered Markers
Fetches the lightweight geolocation data for the map pins. It accepts optional query parameters. If a parameter is omitted, it does not filter by that criteria.

* **Endpoint:** `GET /markers`
* **Access:** Public / Authenticated
* **Query Parameters (All Optional):**
  * `riverName` (String)
  * `organism` (String)
  * `sirProfile` (String)
* **Example Request:** `GET /markers?organism=Klebsiella%20pneumoniae&sirProfile=Resistant`
* **Response (200 OK):**
```json
[
  {
    "siteId": "A10",
    "locationName": "Pretoria",
    "riverName": "Apies River",
    "latitude": -25.747,
    "longitude": 28.229
  },
  {
    "siteId": "B26",
    "locationName": "Hammanskraal",
    "riverName": "Apies River",
    "latitude": -25.750,
    "longitude": 28.230
  }
]
```

---

## 3. Get Site Summary (Clicking a Marker)
Fetches aggregated statistics and details for a specific site to populate the side panel when a map pin is clicked.

* **Endpoint:** `GET /sites/{siteId}/summary`
* **Access:** Public / Authenticated
* **Path Parameter:**
  * `siteId` (String) - The ID of the clicked site.
* **Response (200 OK):**
```json
{
  "siteId": "A10",
  "locationName": "Pretoria",
  "riverName": "Apies River",
  "totalWaterSamples": 1,
  "detectedOrganisms": ["Klebsiella pneumoniae"],
  "lastSampledDate": "2025-05-10"
}
```
* **Response (404 Not Found):** Returned if the `siteId` does not exist.