<template>
  <div class="stub-page">
    <div class="stub-content">
      <i class="pi pi-book stub-icon"></i>
      <h2>Education Blog</h2>
      <p>Research summaries, AMR explainers, and public health outreach articles. Coming soon.</p>
    </div>
  </div>
</template>

<style scoped>
.stub-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
}
.stub-content {
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 14px;
}
.stub-icon {
  font-size: 44px;
  color: var(--c-brand);
  opacity: 0.45;
}
h2 {
  font-family: 'Inter', sans-serif;
  font-size: 18px;
  font-weight: 700;
  color: var(--c-heading);
}
p {
  font-size: 13px;
  color: var(--c-text-muted);
  max-width: 300px;
}
</style>
