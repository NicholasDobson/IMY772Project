<script setup lang="ts">
/* ── Imports ─────────────────────────────────────────────────── */
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { createBlog, getBlogById, updateBlog } from '@/api/blog'

/* ── Reactive state ───────────────────────────────────────────── */
const router = useRouter()
const route = useRoute()

const title = ref('')
const author = ref('')
const content = ref('')
const selectedFile = ref<File | null>(null)
const imagePreview = ref<string | null>(null)
const fileError = ref('')
const loading = ref(false)
const error = ref('')
const success = ref(false)

/* ── Edit mode ────────────────────────────────────────────────── */
const editId = computed(() => route.params.id ? Number(route.params.id) : null)
const isEditMode = computed(() => editId.value !== null)

/* ── Methods ──────────────────────────────────────────────────── */
const handleFileChange = (event: Event) => {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  if (!file) {
    selectedFile.value = null
    imagePreview.value = null
    fileError.value = ''
    return
  }

  if (!file.type.startsWith('image/')) {
    fileError.value = 'Please select a valid image file'
    selectedFile.value = null
    imagePreview.value = null
    return
  }

  selectedFile.value = file
  fileError.value = ''
  imagePreview.value = URL.createObjectURL(file)
}

const handleSubmit = async () => {
  if (!title.value || !author.value || !content.value) {
    error.value = 'Please fill in all required fields'
    return
  }

  loading.value = true
  error.value = ''

  try {
    if (isEditMode.value && editId.value) {
      await updateBlog(editId.value, {
        title: title.value,
        author: author.value,
        content: content.value,
      })
    } else {
      const formData = new FormData()
      formData.append('title', title.value)
      formData.append('author', author.value)
      formData.append('content', content.value)
      if (selectedFile.value) {
        formData.append('image', selectedFile.value)
      }
      await createBlog(formData)
    }

    success.value = true
    router.push({ name: 'education' })
  } catch (err) {
    error.value = err instanceof Error ? err.message : isEditMode.value ? 'Failed to update blog' : 'Failed to create blog'
  } finally {
    loading.value = false
  }
}

const handleCancel = () => {
  router.push({ name: 'education' })
}

/* ── Lifecycle ────────────────────────────────────────────────── */
onMounted(async () => {
  if (editId.value) {
    try {
      const blog = await getBlogById(editId.value)
      title.value = blog.title
      author.value = blog.author
      content.value = blog.content
      if (blog.image) {
        imagePreview.value = blog.image
      }
    } catch (err) {
      error.value = 'Failed to load blog post for editing'
    }
  }
})
</script>

<template>
  <div class="create-blog-view">
    <div class="form-header">
      <h1 class="form-title">{{ isEditMode ? 'Edit Blog Post' : 'Create New Blog Post' }}</h1>
      <p class="form-subtitle">Share your insights and knowledge about antimicrobial resistance</p>
    </div>

    <div class="form-container">
      <form @submit.prevent="handleSubmit">
        <!-- Success Message -->
        <div v-if="success" class="success-message">
          <span class="success-icon">✓</span>
          Blog post {{ isEditMode ? 'updated' : 'created' }} successfully! Redirecting...
        </div>

        <!-- Error Message -->
        <div v-if="error" class="error-message">
          {{ error }}
        </div>

        <!-- Title Field -->
        <div class="form-group">
          <label for="title" class="form-label">Title <span class="required">*</span></label>
          <input
            id="title"
            v-model="title"
            type="text"
            class="form-input"
            placeholder="Enter blog title"
            :disabled="loading || success"
          />
        </div>

        <!-- Author Field -->
        <div class="form-group">
          <label for="author" class="form-label">Author <span class="required">*</span></label>
          <input
            id="author"
            v-model="author"
            type="text"
            class="form-input"
            placeholder="Your name or organization"
            :disabled="loading || success"
          />
        </div>

        <!-- Image Upload Field -->
        <div class="form-group">
          <label for="image" class="form-label">Featured Image</label>
          <input
            id="image"
            type="file"
            accept="image/*"
            class="form-input"
            @change="handleFileChange"
            :disabled="loading || success"
          />
          <p class="form-hint">Optional: Upload a local image file for the blog card</p>
          <div v-if="fileError" class="error-message">
            {{ fileError }}
          </div>
          <div v-if="imagePreview" class="image-preview-container">
            <img :src="imagePreview" alt="Selected image preview" class="image-preview" />
          </div>
        </div>

        <!-- Content Field -->
        <div class="form-group">
          <label for="content" class="form-label">Content <span class="required">*</span></label>
          <textarea
            id="content"
            v-model="content"
            class="form-textarea"
            placeholder="Write your blog post here..."
            rows="12"
            :disabled="loading || success"
          ></textarea>
          <p class="form-hint">{{ content.length }} characters</p>
        </div>

        <!-- Form Actions -->
        <div class="form-actions">
          <button
            type="button"
            class="btn btn-secondary"
            @click="handleCancel"
            :disabled="loading || success"
          >
            Cancel
          </button>
          <button
            type="submit"
            class="btn btn-primary"
            :disabled="loading || success"
          >
            <span v-if="loading">{{ isEditMode ? 'Saving...' : 'Creating...' }}</span>
            <span v-else>{{ isEditMode ? 'Save Changes' : 'Create Blog Post' }}</span>
          </button>
        </div>
      </form>
    </div>
  </div>
</template>

<style scoped>
.create-blog-view {
  padding: 2rem;
  max-width: 800px;
  margin: 0 auto;
}

.form-header {
  margin-bottom: 2.5rem;
  text-align: center;
}

.form-title {
  font-family: 'DM Sans', sans-serif;
  font-size: 2rem;
  font-weight: 700;
  color: var(--c-heading);
  margin: 0 0 0.5rem 0;
}

.form-subtitle {
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
  font-size: 1rem;
  margin: 0;
}

.form-container {
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 0.75rem;
  padding: 2rem;
  box-shadow: var(--c-shadow);
}

.form-group {
  margin-bottom: 1.5rem;
  display: flex;
  flex-direction: column;
}

.form-label {
  font-family: 'DM Sans', sans-serif;
  font-size: 0.95rem;
  font-weight: 600;
  color: var(--c-heading);
  margin-bottom: 0.5rem;
}

.required {
  color: var(--c-red);
}

.form-input,
.form-textarea {
  font-family: 'DM Sans', sans-serif;
  font-size: 1rem;
  color: var(--c-text);
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: 0.5rem;
  padding: 0.75rem 1rem;
  transition: all 0.2s ease;
}

.form-input:focus,
.form-textarea:focus {
  outline: none;
  border-color: var(--c-brand);
  box-shadow: 0 0 0 3px var(--c-brand-dim);
}

.form-input:disabled,
.form-textarea:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.form-textarea {
  resize: vertical;
  line-height: 1.6;
}

.form-hint {
  font-size: 0.85rem;
  color: var(--c-text-muted);
  font-family: 'DM Sans', sans-serif;
  margin-top: 0.25rem;
}

.image-preview-container {
  margin-top: 0.75rem;
}

.image-preview {
  display: block;
  max-width: 240px;
  width: 100%;
  border-radius: 0.5rem;
  border: 1px solid var(--c-border);
}

.success-message {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: var(--c-green-dim);
  border: 1px solid var(--c-green);
  border-radius: 0.5rem;
  color: var(--c-green-text);
  font-family: 'DM Sans', sans-serif;
  margin-bottom: 1.5rem;
}

.success-icon {
  font-size: 1.5rem;
  font-weight: bold;
}

.error-message {
  padding: 1rem;
  background: var(--c-red-dim);
  border: 1px solid var(--c-red);
  border-radius: 0.5rem;
  color: var(--c-red-text);
  font-family: 'DM Sans', sans-serif;
  margin-bottom: 1.5rem;
}

.form-actions {
  display: flex;
  gap: 1rem;
  justify-content: flex-end;
  margin-top: 2rem;
}

.btn {
  font-family: 'DM Sans', sans-serif;
  font-weight: 600;
  font-size: 0.95rem;
  padding: 0.75rem 1.5rem;
  border-radius: 0.5rem;
  border: none;
  cursor: pointer;
  transition: all 0.2s ease;
}

.btn-primary {
  background: var(--c-brand);
  color: white;
}

.btn-primary:hover:not(:disabled) {
  background: var(--c-brand);
  opacity: 0.9;
  transform: translateY(-1px);
  box-shadow: var(--c-shadow-md);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn-secondary {
  background: var(--c-surface);
  color: var(--c-text);
  border: 1px solid var(--c-border);
}

.btn-secondary:hover:not(:disabled) {
  background: var(--c-card-hover);
  border-color: var(--c-brand);
  transform: translateY(-1px);
}

.btn-secondary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
</style>