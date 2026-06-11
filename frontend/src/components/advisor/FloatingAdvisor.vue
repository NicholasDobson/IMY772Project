<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue'
import { askAdvisor, type AdvisorContextType, type SourceReference } from '@/api/advisor'

interface ChatMessage {
  role: 'user' | 'assistant' | 'system'
  text: string
  sources?: SourceReference[]
}

const expandedSources = ref<Record<number, boolean>>({})

const GREETING = 'Hi! I\'m your AMRWatch Advisor. Ask me about water safety, MDRO organisms, or whether a river site is risky. For specific sites or organisms, pick a context above.'

const open      = ref(false)
const loading   = ref(false)
const input     = ref('')
const ctxType   = ref<AdvisorContextType>('general')
const ctxId     = ref('')
const messages  = ref<ChatMessage[]>([{ role: 'system', text: GREETING }])
const bodyRef   = ref<HTMLDivElement | null>(null)

const MAX_CHARS = 500
const remaining = computed(() => MAX_CHARS - input.value.length)
const canSend   = computed(() => !loading.value && input.value.trim().length > 0 && remaining.value >= 0)

function toggle(): void {
  open.value = !open.value
}

function newChat(): void {
  messages.value = [{ role: 'system', text: GREETING }]
  input.value = ''
  expandedSources.value = {}
  loading.value = false
}

async function send(): Promise<void> {
  if (!canSend.value) return
  const msg = input.value.trim()
  messages.value.push({ role: 'user', text: msg })
  input.value = ''
  loading.value = true
  await scrollBottom()

  try {
    const res = await askAdvisor({
      message: msg,
      contextType: ctxType.value,
      contextId: ctxId.value.trim() || undefined,
    })
    if (res.ok && res.reply) {
      messages.value.push({
        role: 'assistant',
        text: res.reply,
        sources: res.sources?.length ? res.sources : undefined,
      })
    } else {
      messages.value.push({
        role: 'assistant',
        text: res.error ?? 'Sorry, the advisor is unavailable right now.',
      })
    }
  } catch {
    messages.value.push({
      role: 'assistant',
      text: 'Network error — please try again in a moment.',
    })
  } finally {
    loading.value = false
    await scrollBottom()
  }
}

async function scrollBottom(): Promise<void> {
  await nextTick()
  if (bodyRef.value) bodyRef.value.scrollTop = bodyRef.value.scrollHeight
}

watch(open, (v) => { if (v) scrollBottom() })

function onKeydown(e: KeyboardEvent): void {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    send()
  }
}

const placeholderForCtx = computed(() => {
  if (ctxType.value === 'site')     return 'Site ID (e.g. B26)'
  if (ctxType.value === 'organism') return 'Organism name (e.g. Klebsiella pneumoniae)'
  return ''
})
</script>

<template>
  <div class="advisor">
    <!-- Toggle bubble -->
    <button
      class="advisor-bubble"
      :class="{ 'advisor-bubble--open': open }"
      :aria-label="open ? 'Close advisor' : 'Open advisor'"
      @click="toggle"
    >
      <i :class="open ? 'pi pi-times' : 'pi pi-comments'"></i>
    </button>

    <!-- Chat panel -->
    <Transition name="advisor-slide">
      <div v-if="open" class="advisor-panel" role="dialog" aria-label="AMRWatch Advisor">
        <header class="advisor-header">
          <div>
            <h3 class="advisor-title">AMRWatch Advisor</h3>
            <p class="advisor-sub">Water safety &amp; AMR guidance</p>
          </div>
          <div class="advisor-header-actions">
            <button class="advisor-new" aria-label="New chat" title="New chat" @click="newChat">
              <i class="pi pi-plus"></i>
            </button>
            <button class="advisor-close" aria-label="Close" @click="toggle">
              <i class="pi pi-times"></i>
            </button>
          </div>
        </header>

        <!-- Context selector -->
        <div class="advisor-ctx">
          <label>
            Context:
            <select v-model="ctxType">
              <option value="general">General</option>
              <option value="site">Site</option>
              <option value="organism">Organism</option>
            </select>
          </label>
          <input
            v-if="ctxType !== 'general'"
            v-model="ctxId"
            type="text"
            :placeholder="placeholderForCtx"
            maxlength="80"
          />
        </div>

        <!-- Message list -->
        <div ref="bodyRef" class="advisor-body">
          <div
            v-for="(m, i) in messages"
            :key="i"
            class="advisor-msg"
            :class="`advisor-msg--${m.role}`"
          >
            <div>
              <div class="advisor-bubble-text">{{ m.text }}</div>
              <div v-if="m.sources?.length" class="advisor-sources">
                <button class="sources-toggle" @click="expandedSources[i] = !expandedSources[i]">
                  <i class="pi pi-book"></i>
                  {{ m.sources.length }} source(s)
                  <i :class="expandedSources[i] ? 'pi pi-chevron-up' : 'pi pi-chevron-down'" style="font-size:9px"></i>
                </button>
                <div v-if="expandedSources[i]" class="sources-list">
                  <div v-for="src in m.sources" :key="src.documentId" class="source-item">
                    <span class="source-title">{{ src.documentTitle }}</span>
                    <span v-if="src.pageNumbers" class="source-pages">pp. {{ src.pageNumbers }}</span>
                    <span class="source-score">{{ (src.similarity * 100).toFixed(0) }}%</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <div v-if="loading" class="advisor-msg advisor-msg--assistant">
            <div class="advisor-bubble-text advisor-typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <!-- Input -->
        <form class="advisor-input" @submit.prevent="send">
          <textarea
            v-model="input"
            rows="2"
            :maxlength="MAX_CHARS"
            placeholder="Ask about a river site, organism, or water safety…"
            :disabled="loading"
            @keydown="onKeydown"
          ></textarea>
          <div class="advisor-input-row">
            <span class="advisor-count" :class="{ 'advisor-count--warn': remaining < 50 }">
              {{ remaining }} chars
            </span>
            <button type="submit" :disabled="!canSend">
              <i class="pi pi-send"></i>
              Send
            </button>
          </div>
        </form>

        <footer class="advisor-foot">
          AI-generated. Not medical advice — consult NICD or DWS for serious concerns.
        </footer>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.advisor {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 9000;
  font-family: 'DM Sans', sans-serif;
}

/* ── Toggle bubble ────────────────────────────────────────── */
.advisor-bubble {
  width: 52px;
  height: 52px;
  border-radius: 50%;
  border: none;
  background: var(--c-brand);
  color: white;
  font-size: 22px;
  cursor: pointer;
  box-shadow: 0 6px 18px rgba(0, 0, 0, 0.22);
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.18s ease, background 0.18s ease;
}
.advisor-bubble:hover { transform: scale(1.06); }
.advisor-bubble--open { background: var(--c-text-dim); }

/* ── Panel ────────────────────────────────────────────────── */
.advisor-panel {
  position: absolute;
  bottom: 68px;
  right: 0;
  width: 420px;
  max-width: calc(100vw - 36px);
  height: 620px;
  max-height: calc(100vh - 100px);
  background: var(--c-card);
  border: 1px solid var(--c-border);
  border-radius: 12px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.advisor-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-brand-dim);
}
.advisor-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-heading);
  margin: 0;
}
.advisor-sub {
  font-size: 11px;
  color: var(--c-text-dim);
  margin: 2px 0 0;
}
.advisor-header-actions {
  display: flex;
  align-items: center;
  gap: 2px;
}
.advisor-new,
.advisor-close {
  background: transparent;
  border: none;
  color: var(--c-text-dim);
  cursor: pointer;
  font-size: 14px;
  padding: 4px 6px;
}
.advisor-new:hover,
.advisor-close:hover { color: var(--c-heading); }

/* ── Context selector ─────────────────────────────────────── */
.advisor-ctx {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  border-bottom: 1px solid var(--c-border);
  font-size: 12px;
  color: var(--c-text);
}
.advisor-ctx label {
  display: flex;
  align-items: center;
  gap: 6px;
}
.advisor-ctx select,
.advisor-ctx input {
  padding: 4px 8px;
  font-size: 12px;
  border: 1px solid var(--c-border);
  border-radius: 4px;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: inherit;
}
.advisor-ctx input { flex: 1; }

/* ── Message body ─────────────────────────────────────────── */
.advisor-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.advisor-msg { display: flex; }
.advisor-msg--user { justify-content: flex-end; }
.advisor-msg--assistant,
.advisor-msg--system { justify-content: flex-start; }

.advisor-bubble-text {
  max-width: 82%;
  padding: 8px 11px;
  border-radius: 10px;
  font-size: 12.5px;
  line-height: 1.45;
  white-space: pre-wrap;
  word-wrap: break-word;
}
.advisor-msg--user .advisor-bubble-text {
  background: var(--c-brand);
  color: white;
  border-bottom-right-radius: 3px;
}
.advisor-msg--assistant .advisor-bubble-text {
  background: var(--c-brand-dim);
  color: var(--c-heading);
  border-bottom-left-radius: 3px;
}
.advisor-msg--system .advisor-bubble-text {
  background: transparent;
  border: 1px dashed var(--c-border);
  color: var(--c-text-dim);
  font-style: italic;
  font-size: 11.5px;
}

/* ── Sources ─────────────────────────────────────────────── */
.advisor-sources {
  max-width: 82%;
  margin-top: 4px;
}
.sources-toggle {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: var(--c-text-muted);
  font-size: 10.5px;
  cursor: pointer;
  padding: 2px 0;
}
.sources-toggle:hover {
  color: var(--c-brand);
}
.sources-list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
  padding: 6px 8px;
  background: var(--c-brand-dim);
  border-radius: 6px;
  border: 1px solid var(--c-border);
}
.source-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: var(--c-text-muted);
}
.source-title {
  font-weight: 500;
  color: var(--c-text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 160px;
}
.source-pages {
  font-size: 9.5px;
  color: var(--c-text-dim);
}
.source-score {
  font-size: 9.5px;
  font-weight: 600;
  color: var(--c-green);
  margin-left: auto;
}

/* ── Typing dots ──────────────────────────────────────────── */
.advisor-typing {
  display: flex;
  gap: 3px;
}
.advisor-typing span {
  width: 6px;
  height: 6px;
  background: var(--c-text-dim);
  border-radius: 50%;
  animation: advisor-blink 1.2s infinite;
}
.advisor-typing span:nth-child(2) { animation-delay: 0.2s; }
.advisor-typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes advisor-blink {
  0%, 80%, 100% { opacity: 0.3; }
  40%           { opacity: 1;   }
}

/* ── Input area ───────────────────────────────────────────── */
.advisor-input {
  border-top: 1px solid var(--c-border);
  padding: 10px 12px;
}
.advisor-input textarea {
  width: 100%;
  border: 1px solid var(--c-border);
  border-radius: 6px;
  padding: 8px 10px;
  font-size: 12.5px;
  background: var(--c-bg);
  color: var(--c-text);
  font-family: inherit;
  resize: none;
}
.advisor-input textarea:focus {
  outline: none;
  border-color: var(--c-brand);
}
.advisor-input-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 6px;
}
.advisor-count {
  font-size: 10.5px;
  color: var(--c-text-dim);
}
.advisor-count--warn { color: var(--c-red); }
.advisor-input button {
  background: var(--c-brand);
  color: white;
  border: none;
  border-radius: 5px;
  padding: 5px 12px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 5px;
}
.advisor-input button:disabled {
  background: var(--c-text-dim);
  cursor: not-allowed;
}

/* ── Footer ───────────────────────────────────────────────── */
.advisor-foot {
  padding: 6px 12px 8px;
  font-size: 10px;
  color: var(--c-text-dim);
  text-align: center;
  border-top: 1px solid var(--c-border);
  background: var(--c-bg);
}

/* ── Slide transition ─────────────────────────────────────── */
.advisor-slide-enter-active,
.advisor-slide-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.advisor-slide-enter-from,
.advisor-slide-leave-to {
  opacity: 0;
  transform: translateY(10px) scale(0.97);
}
</style>
