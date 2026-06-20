<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import Tag from 'primevue/tag'

interface CognitoUser {
  username: string
  email: string
  isAdmin: boolean
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api/v1'

const users = ref<CognitoUser[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const pending = ref<Set<string>>(new Set())

const totalUsers = computed(() => users.value.length)
const totalAdmins = computed(() => users.value.filter((u) => u.isAdmin).length)

async function fetchUsers() {
  loading.value = true
  error.value = null
  try {
    const res = await fetch(`${API_BASE}/admin/users`)
    if (!res.ok) throw new Error(`Failed to load users (${res.status})`)
    users.value = await res.json()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
  } finally {
    loading.value = false
  }
}

async function toggleAdmin(user: CognitoUser) {
  pending.value = new Set([...pending.value, user.username])
  const method = user.isAdmin ? 'DELETE' : 'POST'
  try {
    const res = await fetch(
      `${API_BASE}/admin/users/${encodeURIComponent(user.username)}/promote`,
      { method },
    )
    if (!res.ok) throw new Error(`Request failed (${res.status})`)
    user.isAdmin = !user.isAdmin
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : 'Unknown error'
  } finally {
    const next = new Set(pending.value)
    next.delete(user.username)
    pending.value = next
  }
}

onMounted(fetchUsers)
</script>

<template>
  <div class="um-page">
    <!-- Page header -->
    <div class="page-header">
      <h1 class="page-title">User Management</h1>
    </div>

    <!-- Summary cards -->
    <section class="summary-row">
      <div class="summary-card">
        <span class="summary-value">{{ totalUsers }}</span>
        <span class="summary-label">Total Users</span>
      </div>
      <div class="summary-card">
        <span class="summary-value summary-value--brand">{{ totalAdmins }}</span>
        <span class="summary-label">Admins</span>
      </div>
      <div class="summary-card">
        <span class="summary-value">{{ totalUsers - totalAdmins }}</span>
        <span class="summary-label">Standard Users</span>
      </div>
    </section>

    <!-- Error banner -->
    <div v-if="error" class="error-banner">
      <i class="pi pi-exclamation-triangle"></i> {{ error }}
    </div>

    <!-- Table panel -->
    <div class="panel">
      <div class="panel-header">
        <h2 class="panel-title">All Users</h2>
        <span class="panel-subtitle">Cognito User Pool · {{ totalUsers }} accounts</span>
      </div>

      <DataTable
        :value="users"
        :loading="loading"
        class="um-table"
        size="small"
        striped-rows
      >
        <Column field="email" header="Email">
          <template #body="{ data }">
            <span class="email-cell">{{ data.email || '—' }}</span>
          </template>
        </Column>

        <Column field="username" header="Username" style="width: 300px">
          <template #body="{ data }">
            <code class="username-cell">{{ data.username }}</code>
          </template>
        </Column>

        <Column field="isAdmin" header="Role" style="width: 90px">
          <template #body="{ data }">
            <Tag
              :value="data.isAdmin ? 'Admin' : 'User'"
              :severity="data.isAdmin ? 'info' : 'secondary'"
            />
          </template>
        </Column>

        <Column header="Action" style="width: 130px; text-align: right">
          <template #body="{ data }">
            <button
              class="action-btn"
              :class="data.isAdmin ? 'action-btn--remove' : 'action-btn--promote'"
              :disabled="pending.has(data.username)"
              @click="toggleAdmin(data)"
            >
              <i
                v-if="pending.has(data.username)"
                class="pi pi-spin pi-spinner"
                style="font-size: 11px"
              ></i>
              <template v-else>
                <i :class="data.isAdmin ? 'pi pi-minus-circle' : 'pi pi-plus-circle'"></i>
                {{ data.isAdmin ? 'Remove admin' : 'Make admin' }}
              </template>
            </button>
          </template>
        </Column>
      </DataTable>
    </div>
  </div>
</template>

<style scoped>
.um-page {
  padding: 0 28px 40px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* Header */
.page-header {
  padding: 22px 0 14px;
  text-align: center;
  border-bottom: 1px solid var(--c-border);
}

.page-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 18px;
  font-weight: 400;
  color: var(--c-heading);
  letter-spacing: 0.01em;
}

/* Summary cards */
.summary-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
}

.summary-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: var(--c-shadow);
  display: flex;
  flex-direction: column;
  gap: 4px;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.summary-value {
  font-family: 'DM Mono', monospace;
  font-size: 28px;
  font-weight: 600;
  color: var(--c-heading);
  line-height: 1;
}

.summary-value--brand {
  color: var(--c-brand);
}

.summary-label {
  font-size: 11.5px;
  color: var(--c-text-muted);
  font-weight: 400;
}

/* Error banner */
.error-banner {
  background: var(--c-red-dim, #fef2f2);
  border: 1px solid var(--c-risk-high, #ef4444);
  color: var(--c-risk-high, #ef4444);
  border-radius: 6px;
  padding: 10px 14px;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 8px;
}

/* Panel */
.panel {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 8px;
  padding: 18px 20px;
  box-shadow: var(--c-shadow);
  display: flex;
  flex-direction: column;
  gap: 14px;
  transition: background 0.2s ease, border-color 0.2s ease;
}

.panel-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
}

.panel-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-heading);
}

.panel-subtitle {
  font-size: 10.5px;
  color: var(--c-text-dim);
}

/* Table cells */
.um-table {
  font-family: 'DM Sans', sans-serif;
}

.email-cell {
  font-size: 13px;
  color: var(--c-heading);
}

.username-cell {
  font-family: 'DM Mono', 'JetBrains Mono', monospace;
  font-size: 11px;
  color: var(--c-text-muted);
  background: var(--c-nav-active);
  padding: 2px 6px;
  border-radius: 3px;
}

/* Action buttons */
.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 10px;
  border-radius: 5px;
  font-size: 11.5px;
  font-weight: 500;
  font-family: 'DM Sans', sans-serif;
  border: 1px solid transparent;
  cursor: pointer;
  transition: opacity 0.12s, background 0.12s;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.action-btn--promote {
  background: var(--c-brand);
  color: #fff;
}

.action-btn--promote:hover:not(:disabled) {
  opacity: 0.85;
}

.action-btn--remove {
  background: transparent;
  border-color: var(--c-risk-high, #ef4444);
  color: var(--c-risk-high, #ef4444);
}

.action-btn--remove:hover:not(:disabled) {
  background: var(--c-red-dim, #fef2f2);
}
</style>
