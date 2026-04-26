<script setup lang="ts">
import { ref, onMounted } from 'vue'

interface CognitoUser {
  username: string
  email: string
  isAdmin: boolean
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1'

const users = ref<CognitoUser[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const pending = ref<Set<string>>(new Set())

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
    const res = await fetch(`${API_BASE}/admin/users/${encodeURIComponent(user.username)}/promote`, { method })
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
  <div class="page">
    <div class="page-header">
      <h1 class="page-title">User Management</h1>
      <p class="page-subtitle">Manage Cognito users and admin group membership</p>
    </div>

    <div v-if="loading" class="state-msg">Loading users…</div>
    <div v-else-if="error" class="state-msg state-msg--error">{{ error }}</div>

    <div v-else class="table-wrap">
      <table class="user-table">
        <thead>
          <tr>
            <th>Email</th>
            <th>Username</th>
            <th>Role</th>
            <th>Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="user in users" :key="user.username">
            <td>{{ user.email || '—' }}</td>
            <td class="username-cell">{{ user.username }}</td>
            <td>
              <span class="badge" :class="user.isAdmin ? 'badge--admin' : 'badge--user'">
                {{ user.isAdmin ? 'Admin' : 'User' }}
              </span>
            </td>
            <td>
              <button
                class="btn"
                :class="user.isAdmin ? 'btn--danger' : 'btn--promote'"
                :disabled="pending.has(user.username)"
                @click="toggleAdmin(user)"
              >
                {{ pending.has(user.username) ? '…' : user.isAdmin ? 'Remove admin' : 'Make admin' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.page {
  padding: 28px 32px;
  max-width: 900px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--c-heading);
  margin: 0 0 4px;
}

.page-subtitle {
  font-size: 13px;
  color: var(--c-text-muted);
  margin: 0;
}

.state-msg {
  font-size: 14px;
  color: var(--c-text-muted);
  padding: 24px 0;
}

.state-msg--error {
  color: #e05252;
}

.table-wrap {
  border: 1px solid var(--c-border);
  border-radius: 8px;
  overflow: hidden;
}

.user-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.user-table th {
  background: var(--c-sidebar);
  color: var(--c-text-muted);
  font-weight: 500;
  padding: 10px 16px;
  text-align: left;
  border-bottom: 1px solid var(--c-border);
}

.user-table td {
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
  color: var(--c-text);
}

.user-table tr:last-child td {
  border-bottom: none;
}

.username-cell {
  font-family: monospace;
  font-size: 11px;
  color: var(--c-text-muted);
}

.badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.badge--admin {
  background: #dbeafe;
  color: #1d4ed8;
}

.badge--user {
  background: var(--c-nav-active);
  color: var(--c-text-muted);
}

.btn {
  padding: 5px 12px;
  border-radius: 5px;
  font-size: 12px;
  font-weight: 500;
  border: 1px solid transparent;
  cursor: pointer;
  transition: opacity 0.12s;
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn--promote {
  background: var(--c-brand);
  color: #fff;
}

.btn--danger {
  background: transparent;
  border-color: #e05252;
  color: #e05252;
}
</style>
