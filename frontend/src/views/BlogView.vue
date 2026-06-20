<script setup lang="ts">
/* -- Imports --------------------------------------------------- */
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAllBlogs, getBlogById, type Blog } from '@/api/blog'

/* -- Reactive state --------------------------------------------- */
const router = useRouter()
const route = useRoute()
const blogs = ref<Blog[]>([])
const searchQuery = ref('')
const loading = ref(false)
const selectedBlog = ref<Blog | null>(null)

/* Transient "copied" toast */
const toast = ref<{ msg: string; error: boolean } | null>(null)
let toastTimer: ReturnType<typeof setTimeout> | undefined

/* -- Computed --------------------------------------------------- */
const filteredBlogs = computed(() => {
  if (!searchQuery.value) return blogs.value
  return blogs.value.filter(blog =>
    blog.title.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
    blog.content.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

/* -- Methods ---------------------------------------------------- */
const loadBlogs = async () => {
  loading.value = true
  try {
    blogs.value = await getAllBlogs()
    await openFromQuery()
  } catch (error) {
    console.error('Failed to load blogs:', error)
  } finally {
    loading.value = false
  }
}

/* Open a specific blog when the URL carries ?blog=<id> (shared deep link) */
const openFromQuery = async () => {
  const raw = Array.isArray(route.query.blog) ? route.query.blog[0] : route.query.blog
  if (!raw) return
  let blog = blogs.value.find(b => String(b.blogId) === String(raw))
  if (!blog) {
    try {
      blog = await getBlogById(Number(raw))
    } catch {
      return // unknown/deleted blog id — just show the grid
    }
  }
  if (blog) selectedBlog.value = blog
}

/* Build the absolute, shareable URL for a blog */
const blogShareUrl = (blog: Blog): string => {
  const href = router.resolve({ name: 'education', query: { blog: blog.blogId } }).href
  return new URL(href, window.location.origin).toString()
}

const showToast = (msg: string, error = false) => {
  toast.value = { msg, error }
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { toast.value = null }, 2200)
}

const copyShareLink = async (blog: Blog) => {
  const url = blogShareUrl(blog)
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(url)
    } else {
      // Fallback for non-secure contexts (e.g. plain http during local dev)
      const ta = document.createElement('textarea')
      ta.value = url
      ta.style.position = 'fixed'
      ta.style.opacity = '0'
      document.body.appendChild(ta)
      ta.focus()
      ta.select()
      document.execCommand('copy')
      document.body.removeChild(ta)
    }
    showToast('Link copied to clipboard')
  } catch {
    showToast('Could not copy link', true)
  }
}

const truncateContent = (content: string): string => {
  const words = content.split(/\s+/).filter(Boolean)
  return words.length <= 10 ? content : words.slice(0, 10).join(' ') + '...'
}

const formatDate = (dateString: string): string => {
  return new Date(dateString).toLocaleDateString('en-US', {
    year: 'numeric',
    month: 'long',
    day: 'numeric'
  })
}

const expandBlog = (blog: Blog) => {
  selectedBlog.value = blog
  // Reflect the open blog in the URL so it can be refreshed/shared.
  router.replace({ name: 'education', query: { blog: blog.blogId } })
}

const closeDetail = () => {
  selectedBlog.value = null
  router.replace({ name: 'education', query: {} })
}

const relatedBlogs = computed(() => {
  if (!selectedBlog.value) return []

  const otherBlogs = blogs.value.filter(blog => blog.blogId !== selectedBlog.value?.blogId)
  const shuffled = [...otherBlogs]
  for (let i = shuffled.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1))
    ;[shuffled[i], shuffled[j]] = [shuffled[j]!, shuffled[i]!]
  }
  return shuffled.slice(0, 5)
})

/* -- Lifecycle -------------------------------------------------- */
onMounted(loadBlogs)
</script>

<template>
  <div class="blog-view">
    <header class="page-header">
      <div class="page-header-main">
        <div>
          <h1 class="page-title">AMR Blogs</h1>
          <p class="page-subtitle">Learn more with the latest blogs, published by our esteemed researchers</p>
        </div>
        <div class="page-header-actions">
          <div class="search-container">
            <input
              v-model="searchQuery"
              type="text"
              placeholder="Search blogs..."
              class="search-input"
            />
          </div>
          <button class="btn-create" @click="router.push('/blog/create')">
            + Create Blog
          </button>
        </div>
      </div>
    </header>

    <div v-if="loading" class="loading">Loading blogs...</div>

    <div v-else-if="!selectedBlog" class="blogs-grid">
      <div
        v-for="blog in filteredBlogs"
        :key="blog.blogId"
        class="blog-card"
        @click="expandBlog(blog)"
      >
        <button
          class="card-share-btn"
          title="Copy share link"
          aria-label="Copy share link"
          @click.stop="copyShareLink(blog)"
        >
          <i class="pi pi-share-alt"></i>
        </button>
        <img
          v-if="blog.image"
          :src="blog.image"
          :alt="blog.title"
          class="blog-image"
        />
        <div class="blog-content">
          <h3 class="blog-card-title">{{ blog.title }}</h3>
          <p class="blog-preview">{{ truncateContent(blog.content) }}</p>
          <span class="read-more">Read more</span>
          <div class="blog-meta">
            <span class="author">By {{ blog.author }}</span>
            <span class="date">{{ formatDate(blog.datePublished) }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="detail-page">
      <div class="detail-header">
        <button class="back-button" @click="closeDetail">← Back to blogs</button>
        <button class="share-button" @click="copyShareLink(selectedBlog)">
          <i class="pi pi-share-alt"></i> Share
        </button>
      </div>
      <div class="detail-grid">
        <article class="detail-main">
          <h1 class="detail-title">{{ selectedBlog.title }}</h1>
          <img
            v-if="selectedBlog.image"
            :src="selectedBlog.image"
            :alt="selectedBlog.title"
            class="detail-image"
          />
          <div class="detail-meta">
            <span>Created by {{ selectedBlog.author }}</span>
            <span>{{ formatDate(selectedBlog.datePublished) }}</span>
          </div>
          <div class="detail-body" v-html="selectedBlog.content"></div>
        </article>

        <aside class="detail-sidebar">
          <h2 class="sidebar-title">You might like</h2>
          <div class="mini-cards">
            <article
              v-for="blog in relatedBlogs"
              :key="blog.blogId"
              class="mini-card"
              @click="expandBlog(blog)"
            >
              <img
                v-if="blog.image"
                :src="blog.image"
                :alt="blog.title"
                class="mini-card-image"
              />
              <div class="mini-card-title">{{ blog.title }}</div>
            </article>
          </div>
        </aside>
      </div>
    </div>

    <Transition name="toast-fade">
      <div
        v-if="toast"
        class="share-toast"
        :class="{ 'share-toast--error': toast.error }"
        role="status"
        aria-live="polite"
      >
        <i class="pi" :class="toast.error ? 'pi-times-circle' : 'pi-check-circle'"></i>
        {{ toast.msg }}
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.blog-view {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  padding: 22px 0 14px;
  border-bottom: 1px solid var(--c-border);
  margin-bottom: 2rem;
}

.page-header-main {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
}

.page-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 18px;
  font-weight: 400;
  color: var(--c-heading);
  letter-spacing: 0.01em;
  margin: 0;
}

.page-subtitle {
  margin: 0.5rem 0 0;
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
  font-size: 0.95rem;
}

.page-header-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  align-items: center;
}

.blog-header {
  display: none;
}

.search-container {
  width: 300px;
}

.search-input {
  width: 100%;
  padding: 0.75rem 1rem;
  border: 1px solid var(--c-border);
  border-radius: 0.5rem;
  background: var(--c-surface);
  color: var(--c-text);
  font-family: 'DM Sans', sans-serif;
  font-size: 1rem;
}

.search-input:focus {
  outline: none;
  border-color: var(--c-brand);
  box-shadow: 0 0 0 3px var(--c-brand-dim);
}

.btn-create {
  padding: 0.75rem 1.5rem;
  background: var(--c-brand);
  color: white;
  border: none;
  border-radius: 0.5rem;
  font-family: 'DM Sans', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.2s ease;
  white-space: nowrap;
}

.btn-create:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: var(--c-shadow-md);
}

.loading {
  text-align: center;
  padding: 2rem;
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
}

.blogs-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
  align-items: stretch;
}

.blog-card {
  position: relative;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  min-height: 430px;
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 0.75rem;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--c-shadow);
}

.card-share-btn {
  position: absolute;
  top: 0.75rem;
  right: 0.75rem;
  z-index: 2;
  width: 2.25rem;
  height: 2.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  border-radius: 50%;
  cursor: pointer;
  color: #fff;
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(2px);
  font-size: 0.95rem;
  opacity: 0;
  transition: opacity 0.2s ease, background 0.2s ease, transform 0.2s ease;
}

.blog-card:hover .card-share-btn,
.card-share-btn:focus-visible {
  opacity: 1;
}

.card-share-btn:hover {
  background: var(--c-brand);
  transform: translateY(-1px);
}

/* Touch devices have no hover — keep the button visible there */
@media (hover: none) {
  .card-share-btn {
    opacity: 1;
  }
}

.blog-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--c-shadow-md);
  border-color: var(--c-brand);
}

.blog-image {
  width: 100%;
  height: 200px;
  object-fit: cover;
}

.blog-content {
  padding: 1.5rem;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  flex: 1;
}

.blog-card-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--c-heading);
  margin: 0 0 0.75rem 0;
  line-height: 1.4;
}

.blog-preview {
  color: var(--c-text);
  font-family: 'DM Sans', sans-serif;
  font-size: 0.9rem;
  line-height: 1.6;
  margin: 0 0 1rem 0;
}

.read-more {
  color: var(--c-brand);
  font-family: 'DM Sans', sans-serif;
  font-weight: 600;
  font-size: 0.9rem;
  cursor: pointer;
}

.blog-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.8rem;
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
  margin-top: 1rem;
}

.detail-page {
  position: fixed;
  inset: 0;
  z-index: 1000;
  background: var(--c-surface);
  overflow-y: auto;
  padding: 2rem;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.share-button {
  display: inline-flex;
  align-items: center;
  gap: 0.5rem;
  background: var(--c-brand);
  border: 1px solid var(--c-brand);
  color: #fff;
  padding: 0.75rem 1.25rem;
  border-radius: 0.5rem;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  font-weight: 600;
  font-size: 0.95rem;
  transition: all 0.2s ease;
}

.share-button:hover {
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: var(--c-shadow-md);
}

.back-button {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  color: var(--c-text);
  padding: 0.75rem 1rem;
  border-radius: 0.5rem;
  cursor: pointer;
  font-family: 'DM Sans', sans-serif;
  font-weight: 600;
}

.back-button:hover {
  background: var(--c-surface-muted);
}

.detail-grid {
  display: grid;
  grid-template-columns: 3fr 1fr;
  gap: 2rem;
  align-items: start;
}

.detail-main {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.detail-image {
  width: 100%;
  aspect-ratio: 3 / 1;
  object-fit: cover;
  border-radius: 0.75rem;
}

.detail-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  color: var(--c-heading);
  margin: 0;
}

.detail-meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 0.5rem;
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
  font-size: 0.95rem;
}

.detail-body {
  color: var(--c-text);
  font-family: 'DM Sans', sans-serif;
  line-height: 1.8;
  font-size: 1rem;
}

.detail-sidebar {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}

.sidebar-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 1.1rem;
  font-weight: 700;
  margin: 0;
}

.mini-cards {
  display: grid;
  gap: 1rem;
}

.mini-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 0.75rem;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  display: flex;
  gap: 0.75rem;
  align-items: center;
  padding: 0.75rem;
}

.mini-card:hover {
  transform: translateY(-1px);
  box-shadow: var(--c-shadow-md);
}

.mini-card-image {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 0.5rem;
  flex-shrink: 0;
}

.mini-card-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 0.95rem;
  color: var(--c-heading);
  line-height: 1.3;
}

@media (max-width: 1200px) {
  .detail-grid {
    grid-template-columns: 1.5fr 1fr;
  }
}

@media (max-width: 900px) {
  .detail-grid {
    display: block;
  }
}

.modal-content {
  background: var(--c-surface);
  border-radius: 0.75rem;
  width: min(1100px, 96vw);
  min-height: 70vh;
  max-height: 92vh;
  overflow-y: auto;
  padding: 2rem;
  position: relative;
  box-shadow: var(--c-shadow-md);
}

@media (max-width: 800px) {
  .modal-content {
    width: calc(100vw - 24px);
    min-height: 60vh;
    padding: 1.5rem;
  }
}

.close-button {
  position: absolute;
  top: 1rem;
  right: 1rem;
  background: none;
  border: none;
  font-size: 2rem;
  color: var(--c-text-muted);
  cursor: pointer;
  padding: 0;
  width: 2rem;
  height: 2rem;
  display: flex;
  align-items: center;
  justify-content: center;
}

.close-button:hover {
  color: var(--c-text);
}

.modal-content h2 {
  font-family: 'DM Sans', sans-serif;
  font-size: 1.75rem;
  color: var(--c-heading);
  margin: 0 0 1rem 0;
}

.modal-image {
  width: 100%;
  height: 360px;
  object-fit: cover;
  border-radius: 0.5rem;
  margin-bottom: 1rem;
}

.modal-meta {
  display: flex;
  justify-content: space-between;
  font-size: 0.9rem;
  color: var(--c-text-muted);
  margin-bottom: 1.5rem;
}

.modal-body {
  color: var(--c-text);
  font-family: 'DM Sans', sans-serif;
  line-height: 1.7;
  font-size: 1rem;
}

/* -- Share toast ------------------------------------------------ */
.share-toast {
  position: fixed;
  bottom: 2rem;
  left: 50%;
  transform: translateX(-50%);
  z-index: 2000;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1.25rem;
  background: var(--c-heading);
  color: var(--c-surface);
  border-radius: 0.5rem;
  font-family: 'DM Sans', sans-serif;
  font-size: 0.9rem;
  font-weight: 600;
  box-shadow: var(--c-shadow-md);
}

.share-toast .pi {
  font-size: 1rem;
}

.share-toast--error {
  background: var(--c-red);
  color: #fff;
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}
</style>
