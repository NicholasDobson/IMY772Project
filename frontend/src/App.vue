<script setup lang="ts">
import { onMounted } from 'vue'
import AppSidebar from '@/components/layout/AppSidebar.vue'
import FloatingAdvisor from '@/components/advisor/FloatingAdvisor.vue'
import { useThemeStore } from '@/stores/theme'
import { useAuthStore } from '@/stores/auth'

const themeStore = useThemeStore()
const authStore = useAuthStore()

onMounted(async () => {
  themeStore.init()
  await authStore.initialize()
})
</script>

<template>
  <div class="app-shell">
    <AppSidebar />
    <main class="app-main">
      <RouterView />
    </main>
    <FloatingAdvisor />
  </div>
</template>

<style scoped>
.app-shell {
  display: flex;
  width: 100%;
  min-height: 100vh;
}

.app-main {
  flex: 1;
  min-width: 0;
  background: var(--c-bg);
  overflow-y: auto;
  transition: background 0.2s ease;
}
</style>
