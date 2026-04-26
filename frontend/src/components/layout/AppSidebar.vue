<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import upLogo from '@/assets/up-logo.png'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()

const baseNavItems = [
  { name: 'Dashboard', path: '/', icon: 'pi-objects-column' },
  { name: 'Map', path: '/map', icon: 'pi-map' },
  { name: 'River detail', path: '/river', icon: 'pi-chart-line' },
  { name: 'Education Blog', path: '/education', icon: 'pi-book' },
  { name: 'Data Upload', path: '/upload', icon: 'pi-upload', adminOnly: true },
]

const navItems = computed(() =>
  baseNavItems.filter((item) => !item.adminOnly || auth.isAdmin),
)

function isActive(path: string) {
  return path === '/' ? route.path === '/' : route.path.startsWith(path)
}
</script>

<template>
  <aside class="sidebar">
    <!-- University crest + brand -->
    <div class="sidebar-brand">
      <div class="university-crest">
        <img :src="upLogo" alt="University of Pretoria" class="up-logo" />
      </div>
      <div class="amr-logo">
        <svg width="32" height="22" viewBox="0 0 32 22" fill="none">
          <path
            d="M2 16 C5 8, 9 5, 13 11 S21 19, 25 13 S30 4, 30 4"
            stroke="var(--c-brand)"
            stroke-width="2.2"
            stroke-linecap="round"
            fill="none"
          />
          <path
            d="M2 20 C5 12, 9 9, 13 15 S21 23, 25 17 S30 8, 30 8"
            stroke="var(--c-brand)"
            stroke-width="1.2"
            stroke-linecap="round"
            fill="none"
            opacity="0.4"
          />
        </svg>
        <span class="amr-wordmark">
          <span class="amr-part">AMR</span><span class="watch-part">Watch</span>
        </span>
      </div>
    </div>

    <!-- Navigation -->
    <nav class="sidebar-nav">
      <RouterLink
        v-for="item in navItems"
        :key="item.path"
        :to="item.path"
        class="nav-item"
        :class="{ 'nav-item--active': isActive(item.path) }"
      >
        <i :class="`pi ${item.icon}`" class="nav-icon"></i>
        <span>{{ item.name }}</span>
      </RouterLink>
    </nav>

    <!-- Footer -->
    <div class="sidebar-footer">
      <div class="footer-divider"></div>
      <RouterLink
        to="/settings"
        class="nav-item"
        :class="{ 'nav-item--active': isActive('/settings') }"
      >
        <i class="pi pi-cog nav-icon"></i>
        <span>Settings</span>
      </RouterLink>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 185px;
  min-height: 100vh;
  height: 100vh;
  position: sticky;
  top: 0;
  background: var(--c-sidebar);
  border-right: 1px solid var(--c-sidebar-border);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  transition:
    background 0.2s ease,
    border-color 0.2s ease;
}

/* Brand */
.sidebar-brand {
  padding: 16px 14px 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  border-bottom: 1px solid var(--c-border);
}

.university-crest {
  display: flex;
  justify-content: center;
  align-items: center;
}

.up-logo {
  height: 72px;
  width: auto;
  object-fit: contain;
}

.amr-logo {
  display: flex;
  align-items: center;
  gap: 8px;
}

.amr-wordmark {
  font-family: 'Zen Dots', sans-serif;
  font-size: 17px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.2px;
}

.amr-part {
  color: var(--c-brand);
}
.watch-part {
  color: var(--c-heading);
}

/* Nav */
.sidebar-nav {
  flex: 1;
  padding: 10px 8px;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 9px;
  padding: 8px 10px;
  border-radius: 6px;
  color: var(--c-text-muted);
  font-size: 13px;
  font-weight: 400;
  text-decoration: none;
  transition:
    background 0.12s,
    color 0.12s;
  white-space: nowrap;
}

.nav-item:hover {
  background: var(--c-nav-active);
  color: var(--c-nav-active-text);
}

.nav-item--active {
  background: var(--c-nav-active);
  color: var(--c-nav-active-text);
  font-weight: 500;
}

.nav-icon {
  font-size: 14px;
  flex-shrink: 0;
  width: 15px;
  text-align: center;
}

/* Footer */
.sidebar-footer {
  padding-bottom: 10px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.footer-divider {
  height: 1px;
  background: var(--c-border);
  margin: 4px 12px 8px;
}

.sidebar-footer .nav-item {
  margin: 0 8px;
}
</style>
