<script setup lang="ts">
/* -- Imports --------------------------------------------------- */
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getAllBlogs, searchBlogs, type Blog } from '@/api/blog'

/* -- Reactive state --------------------------------------------- */
const router = useRouter()
const blogs = ref<Blog[]>([])
const searchQuery = ref('')
const loading = ref(false)
const selectedBlog = ref<Blog | null>(null)

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
  } catch (error) {
    console.error('Failed to load blogs:', error)
  } finally {
    loading.value = false
  }
}

const truncateContent = (content: string): string => {
  const sentences = content.split('. ').slice(0, 2).join('. ')
  return sentences + (content.split('. ').length > 2 ? '...' : '')
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
}

const closeModal = () => {
  selectedBlog.value = null
}

/* -- Lifecycle -------------------------------------------------- */
onMounted(loadBlogs)
</script>

<template>
  <div class="blog-view">
    <div class="blog-header">
      <h1 class="blog-title">Blog</h1>
      <div class="header-right">
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

    <div v-if="loading" class="loading">Loading blogs...</div>

    <div v-else class="blogs-grid">
      <div
        v-for="blog in filteredBlogs"
        :key="blog.blogId"
        class="blog-card"
        @click="expandBlog(blog)"
      >
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

    <!-- Modal for full blog -->
    <div v-if="selectedBlog" class="modal-overlay" @click="closeModal">
      <div class="modal-content" @click.stop>
        <button class="close-button" @click="closeModal">&times;</button>
        <h2>{{ selectedBlog.title }}</h2>
        <img
          v-if="selectedBlog.image"
          :src="selectedBlog.image"
          :alt="selectedBlog.title"
          class="modal-image"
        />
        <div class="modal-meta">
          <span>By {{ selectedBlog.author }}</span>
          <span>{{ formatDate(selectedBlog.datePublished) }}</span>
        </div>
        <div class="modal-body" v-html="selectedBlog.content"></div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.blog-view {
  padding: 2rem;
  max-width: 1200px;
  margin: 0 auto;
}

.blog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2rem;
}

.blog-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  color: var(--c-heading);
  margin: 0;
}

.header-right {
  display: flex;
  gap: 1rem;
  align-items: center;
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
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 2rem;
}

.blog-card {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 0.75rem;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s ease;
  box-shadow: var(--c-shadow);
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

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: var(--c-surface);
  border-radius: 0.75rem;
  max-width: 800px;
  max-height: 90vh;
  overflow-y: auto;
  padding: 2rem;
  position: relative;
  box-shadow: var(--c-shadow-md);
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
  height: 300px;
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
</style>
