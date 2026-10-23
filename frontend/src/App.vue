<script setup>
import { ref, onMounted } from 'vue'
import { searchTransactions, fetchFlags } from './api.js'

const tab = ref('transactions')

// --- transactions / search ---
const SIZE = 20
const rows = ref([])
const info = ref({ number: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const page = ref(0)
const q = ref({ agency: '', recipient: '', minAmount: '' })
const loading = ref(false)
const error = ref('')
const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })

async function load() {
  loading.value = true; error.value = ''
  try {
    const data = await searchTransactions({ ...q.value, page: page.value, size: SIZE })
    rows.value = data.content; info.value = data
  } catch (e) { error.value = e.message } finally { loading.value = false }
}
function doSearch() { page.value = 0; load() }
function clearSearch() { q.value = { agency: '', recipient: '', minAmount: '' }; page.value = 0; load() }
function prev() { if (!info.value.first) { page.value--; load() } }
function next() { if (!info.value.last) { page.value++; load() } }

// --- flags ---
const flags = ref([])
const flagsLoading = ref(false)
const flagsError = ref('')
async function loadFlags() {
  flagsLoading.value = true; flagsError.value = ''
  try { flags.value = await fetchFlags() }
  catch (e) { flagsError.value = e.message } finally { flagsLoading.value = false }
}
function go(t) {
  tab.value = t
  if (t === 'flags' && flags.value.length === 0) loadFlags()
}

onMounted(load)
</script>

<template>
  <header>
    <h1>ClearSpend</h1>
    <p>Public-finance transparency — anomaly detection</p>
  </header>

  <main>
    <nav class="tabs">
      <button :class="{ active: tab === 'transactions' }" @click="go('transactions')">Transactions</button>
      <button :class="{ active: tab === 'flags' }" @click="go('flags')">⚑ Flags</button>
    </nav>

    <!-- TRANSACTIONS -->
    <section v-if="tab === 'transactions'">
      <div class="search">
        <input v-model="q.agency" placeholder="Agency" @keyup.enter="doSearch" />
        <input v-model="q.recipient" placeholder="Recipient" @keyup.enter="doSearch" />
        <input v-model="q.minAmount" type="number" placeholder="Min $" @keyup.enter="doSearch" />
        <button @click="doSearch">Search</button>
        <button class="ghost" @click="clearSearch">Clear</button>
      </div>
      <div class="bar">
        <button :disabled="info.first || loading" @click="prev">← Prev</button>
        <button :disabled="info.last || loading" @click="next">Next →</button>
        <span class="muted" v-if="!loading && !error">
          Trang {{ info.number + 1 }} / {{ info.totalPages }} · {{ info.totalElements.toLocaleString() }} giao dịch
        </span>
      </div>
      <p v-if="loading" class="muted">Loading…</p>
      <p v-else-if="error" class="muted">Lỗi: {{ error }}. Backend chạy chưa?</p>
      <table v-else>
        <thead><tr><th>Agency</th><th>Recipient</th><th class="right">Amount</th><th>Action date</th></tr></thead>
        <tbody>
          <tr v-for="t in rows" :key="t.id">
            <td>{{ t.agency ?? '—' }}</td>
            <td>{{ t.recipient ?? '—' }}</td>
            <td class="right amount">{{ t.amount != null ? money.format(t.amount) : '—' }}</td>
            <td>{{ t.actionDate ?? '—' }}</td>
          </tr>
          <tr v-if="rows.length === 0 && !loading"><td colspan="4" class="muted">Không có kết quả.</td></tr>
        </tbody>
      </table>
    </section>

    <!-- FLAGS -->
    <section v-else>
      <p v-if="flagsLoading" class="muted">Loading…</p>
      <p v-else-if="flagsError" class="muted">Lỗi: {{ flagsError }}</p>
      <p v-else-if="flags.length === 0" class="muted">Chưa có flag — chạy anomaly job (POST /api/internal/anomaly/run) trước.</p>
      <ul v-else class="flags">
        <li v-for="f in flags" :key="f.id" class="flag">
          <span class="badge" :class="f.severity?.toLowerCase()">{{ f.severity }}</span>
          <span class="rule">{{ f.rule }}</span>
          <p class="explanation">{{ f.explanation }}</p>
        </li>
      </ul>
    </section>
  </main>
</template>

<style scoped>
header { background: #0b5a4e; color: #fff; padding: 16px 24px; }
header h1 { margin: 0; font-size: 20px; }
header p { margin: 4px 0 0; opacity: .85; font-size: 13px; }
main { max-width: 1000px; margin: 24px auto; padding: 0 16px; }

.tabs { display: flex; gap: 8px; margin-bottom: 16px; border-bottom: 1px solid #ddd; }
.tabs button { border: none; background: none; padding: 8px 14px; cursor: pointer; font-size: 14px; color: #666; border-bottom: 2px solid transparent; }
.tabs button.active { color: #0b5a4e; border-bottom-color: #0b5a4e; font-weight: 600; }

.search { display: flex; gap: 8px; margin-bottom: 12px; flex-wrap: wrap; }
.search input { padding: 6px 10px; border: 1px solid #ccc; border-radius: 6px; font-size: 14px; }
.search button { padding: 6px 12px; border: 1px solid #0b5a4e; background: #0b5a4e; color: #fff; border-radius: 6px; cursor: pointer; }
.search button.ghost { background: #fff; color: #0b5a4e; }

.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.bar button { padding: 6px 12px; border: 1px solid #ccc; background: #fff; border-radius: 6px; cursor: pointer; }
.bar button:disabled { opacity: .4; cursor: default; }
.muted { color: #666; font-size: 13px; }

table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #eee; font-size: 14px; }
th { background: #f0f3f2; font-weight: 600; }
.right { text-align: right; }
td.amount { font-variant-numeric: tabular-nums; font-weight: 600; }

.flags { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
.flag { background: #fff; border-radius: 8px; padding: 12px 14px; box-shadow: 0 1px 3px rgba(0,0,0,.08); display: grid; grid-template-columns: auto 1fr; gap: 6px 10px; align-items: center; }
.badge { grid-row: 1; font-size: 11px; font-weight: 700; padding: 2px 8px; border-radius: 99px; color: #fff; }
.badge.high { background: #c0392b; }
.badge.medium { background: #d68910; }
.rule { grid-row: 1; font-size: 12px; color: #888; font-family: monospace; }
.explanation { grid-column: 1 / -1; margin: 0; font-size: 14px; }
</style>
