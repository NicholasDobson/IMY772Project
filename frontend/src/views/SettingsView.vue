<script setup lang="ts">
import { computed } from 'vue'
import { useThemeStore, type ThemeMode } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const themeStore = useThemeStore()
const authStore = useAuthStore()

const themeOptions: {
  value: ThemeMode
  label: string
  description: string
}[] = [
  {
    value: 'light',
    label: 'Light',
    description: 'Always use light appearance',
  },
  {
    value: 'dark',
    label: 'Dark',
    description: 'Always use dark appearance',
  },
  {
    value: 'system',
    label: 'System',
    description: 'Match your device setting',
  },
]

const userEmail = computed(() => (authStore.user?.email as string) ?? 'Not available')

const userRole = computed(() => {
  const groups = (authStore.user?.['cognito:groups'] as string[]) ?? []
  if (groups.includes('PISupervisor')) return 'PI / Supervisor'
  if (groups.includes('LabScientist')) return 'Lab Scientist'
  if (groups.includes('FieldResearcher')) return 'Field Researcher'
  if (groups.includes('Admins')) return 'Administrator'
  return 'Public User'
})
</script>

<template>
  <div class="settings-page">
    <!-- Page header -->
    <div class="page-header">
      <h1 class="page-title">Settings</h1>
    </div>

    <div class="settings-body">
      <!-- Appearance section -->
      <section class="settings-section">
        <div class="section-header">
          <h2 class="section-title">Appearance</h2>
          <p class="section-desc">Choose how AMRWatch looks on your device.</p>
        </div>

        <div class="theme-radio-group" role="radiogroup" aria-label="Theme selection">
          <label
            v-for="opt in themeOptions"
            :key="opt.value"
            class="theme-card"
            :class="{ 'theme-card--selected': themeStore.mode === opt.value }"
          >
            <input
              type="radio"
              name="theme"
              :value="opt.value"
              :checked="themeStore.mode === opt.value"
              class="sr-only"
              @change="themeStore.setMode(opt.value)"
            />

            <!-- Mini window preview -->
            <div class="preview" :class="`preview--${opt.value}`">
              <!-- Sidebar strip -->
              <div class="preview-sidebar">
                <div class="preview-logo"></div>
                <div class="preview-nav">
                  <div class="preview-nav-item preview-nav-item--active"></div>
                  <div class="preview-nav-item"></div>
                  <div class="preview-nav-item"></div>
                  <div class="preview-nav-item"></div>
                </div>
              </div>
              <!-- Content area -->
              <div class="preview-content">
                <div class="preview-stats">
                  <div class="preview-stat"></div>
                  <div class="preview-stat"></div>
                  <div class="preview-stat"></div>
                </div>
                <div class="preview-block preview-block--wide"></div>
                <div class="preview-blocks-row">
                  <div class="preview-block"></div>
                  <div class="preview-block"></div>
                </div>
              </div>
            </div>

            <!-- Radio indicator + label -->
            <div class="theme-card-footer">
              <span
                class="radio-dot"
                :class="{ 'radio-dot--checked': themeStore.mode === opt.value }"
              >
                <span class="radio-inner"></span>
              </span>
              <div class="theme-card-text">
                <span class="theme-label">{{ opt.label }}</span>
                <span class="theme-desc">{{ opt.description }}</span>
              </div>
            </div>
          </label>
        </div>
      </section>

      <div class="section-divider"></div>

      <!-- Account section -->
      <section class="settings-section">
        <div class="section-header">
          <h2 class="section-title">Account</h2>
          <p class="section-desc">Your profile and role information.</p>
        </div>

        <div class="account-card">
          <div class="account-avatar">
            <i class="pi pi-user"></i>
          </div>
          <div class="account-cols">
            <div class="account-col">
              <span class="account-label">Email</span>
              <span class="account-value">{{ userEmail }}</span>
            </div>
            <div class="account-col-divider"></div>
            <div class="account-col">
              <span class="account-label">Role</span>
              <span class="account-badge">{{ userRole }}</span>
            </div>
          </div>
        </div>

      </section>
    </div>
  </div>
</template>

<style scoped>
/* ── Page shell ───────────────────────────────────────── */
.settings-page {
  padding: 0 28px 60px;
  max-width: 780px;
}

.page-header {
  padding: 22px 0 14px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 32px;
}

.page-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 18px;
  font-weight: 400;
  color: var(--c-heading);
}

/* ── Section ──────────────────────────────────────────── */
.settings-body {
  display: flex;
  flex-direction: column;
  gap: 0;
}

.settings-section {
  padding: 0 0 32px;
}

.section-header {
  margin-bottom: 20px;
}

.section-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-heading);
  margin-bottom: 3px;
}

.section-desc {
  font-size: 12.5px;
  color: var(--c-text-muted);
}

.section-divider {
  height: 1px;
  background: var(--c-border);
  margin-bottom: 32px;
}

/* ── Theme Radio Group ────────────────────────────────── */
.theme-radio-group {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
}

.theme-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
  cursor: pointer;
  width: 168px;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* ── Preview window ───────────────────────────────────── */
.preview {
  border-radius: 10px;
  border: 2px solid var(--c-border);
  overflow: hidden;
  display: flex;
  height: 108px;
  transition:
    border-color 0.18s,
    box-shadow 0.18s;
  box-shadow: var(--c-shadow);
}

.theme-card--selected .preview {
  border-color: var(--c-brand);
  box-shadow: 0 0 0 3px color-mix(in srgb, var(--c-brand) 20%, transparent);
}

.theme-card:hover:not(.theme-card--selected) .preview {
  border-color: var(--c-border-strong);
}

/* Light preview */
.preview--light { background: #f4f6f8; }
.preview--light .preview-sidebar { background: #ffffff; border-right: 1px solid #e5e7eb; }
.preview--light .preview-logo { background: #e5e7eb; }
.preview--light .preview-nav-item { background: #f3f4f6; }
.preview--light .preview-nav-item--active { background: #dbeafe; }
.preview--light .preview-stat { background: #ffffff; border: 1px solid #e5e7eb; }
.preview--light .preview-block { background: #e5e7eb; }
.preview--light .preview-block--wide { background: #dbeafe; }

/* Dark preview */
.preview--dark { background: #0a0f1a; }
.preview--dark .preview-sidebar { background: #0c1420; border-right: 1px solid rgba(255,255,255,0.07); }
.preview--dark .preview-logo { background: rgba(255,255,255,0.08); }
.preview--dark .preview-nav-item { background: rgba(255,255,255,0.05); }
.preview--dark .preview-nav-item--active { background: rgba(59,130,246,0.15); }
.preview--dark .preview-stat { background: #101d2e; border: 1px solid rgba(255,255,255,0.08); }
.preview--dark .preview-block { background: rgba(255,255,255,0.08); }
.preview--dark .preview-block--wide { background: rgba(59,130,246,0.12); }

/* System preview */
.preview--system { position: relative; }
.preview--system .preview-sidebar { background: linear-gradient(to bottom, #ffffff 50%, #0c1420 50%); border-right: 1px solid #e5e7eb; }
.preview--system .preview-logo { background: linear-gradient(to bottom, #e5e7eb 50%, rgba(255,255,255,0.08) 50%); }
.preview--system .preview-nav-item { background: linear-gradient(to bottom, #f3f4f6 50%, rgba(255,255,255,0.05) 50%); }
.preview--system .preview-nav-item--active { background: linear-gradient(to bottom, #dbeafe 50%, rgba(59,130,246,0.15) 50%); }
.preview--system .preview-content { background: linear-gradient(to bottom, #f4f6f8 50%, #0a0f1a 50%); }
.preview--system .preview-stat { background: linear-gradient(to bottom, #ffffff 50%, #101d2e 50%); border: 1px solid #e5e7eb; }
.preview--system .preview-block { background: linear-gradient(to bottom, #e5e7eb 50%, rgba(255,255,255,0.08) 50%); }
.preview--system .preview-block--wide { background: linear-gradient(to bottom, #dbeafe 50%, rgba(59,130,246,0.12) 50%); }

/* ── Preview internals ────────────────────────────────── */
.preview-sidebar {
  width: 32px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 6px 4px;
}

.preview-logo {
  height: 8px;
  border-radius: 2px;
  margin-bottom: 4px;
}

.preview-nav-item {
  height: 5px;
  border-radius: 2px;
}

.preview-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 5px;
  padding: 6px 5px;
}

.preview-stats {
  display: flex;
  gap: 3px;
}

.preview-stat {
  flex: 1;
  height: 18px;
  border-radius: 3px;
}

.preview-block {
  flex: 1;
  height: 12px;
  border-radius: 3px;
}

.preview-block--wide {
  height: 12px;
  border-radius: 3px;
  flex: unset;
  width: 100%;
}

.preview-blocks-row {
  display: flex;
  gap: 3px;
}

/* ── Radio indicator ──────────────────────────────────── */
.theme-card-footer {
  display: flex;
  align-items: center;
  gap: 10px;
}

.radio-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: 1.5px solid var(--c-border-strong);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: border-color 0.15s;
  background: var(--c-card);
}

.radio-dot--checked {
  border-color: var(--c-brand);
  border-width: 2px;
}

.radio-inner {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: transparent;
  transition: background 0.15s;
}

.radio-dot--checked .radio-inner {
  background: var(--c-brand);
}

.theme-card-text {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.theme-label {
  font-size: 13px;
  font-weight: 500;
  color: var(--c-heading);
  line-height: 1;
}

.theme-desc {
  font-size: 11px;
  color: var(--c-text-muted);
  line-height: 1.3;
}

/* ── Account card ─────────────────────────────────────── */
.account-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 10px;
  margin-bottom: 12px;
}

.account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--c-brand-dim);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.account-avatar .pi {
  font-size: 18px;
  color: var(--c-brand);
}

.account-cols {
  flex: 1;
  display: flex;
  align-items: flex-start;
  gap: 0;
}

.account-col {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.account-col-divider {
  width: 1px;
  background: var(--c-border);
  align-self: stretch;
  margin: 0 20px;
}

.account-label {
  font-size: 12.5px;
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
}

.account-value {
  font-size: 13px;
  color: var(--c-heading);
  font-family: 'DM Sans', sans-serif;
  font-weight: 500;
}

.account-badge {
  font-size: 11.5px;
  font-family: 'DM Sans', sans-serif;
  font-weight: 600;
  color: var(--c-brand);
  background: var(--c-brand-dim);
  padding: 3px 10px;
  border-radius: 999px;
}

</style>