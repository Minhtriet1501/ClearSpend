<script setup>
import { ref, onMounted } from 'vue'
import { fetchTransactions } from './api.js'

const SIZE = 20
const rows = ref([])
const info = ref({ number: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const page = ref(0)
const loading = ref(false)
const error = ref('')

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })

async function load() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchTransactions({ page: page.value, size: SIZE })
    rows.value = data.content
    info.value = data
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

function prev() { if (!info.value.first) { page.value--; load() } }
function next() { if (!info.value.last) { page.value++; load() } }

onMounted(load)
</script>

<template>
  <header>
    <h1>ClearSpend</h1>
    <p>Public-finance transparency — USAspending awards</p>
  </header>

  <main>
    <div class="bar">
      <button :disabled="info.first || loading" @click="prev">← Prev</button>
      <button :disabled="info.last || loading" @click="next">Next →</button>
      <span class="muted" v-if="!loading && !error">
        Trang {{ info.number + 1 }} / {{ info.totalPages }} · {{ info.totalElements.toLocaleString() }} giao dịch
      </span>
    </div>

    <p v-if="loading" class="muted">Loading…</p>
    <p v-else-if="error" class="muted">Lỗi tải data: {{ error }}. Backend chạy chưa?</p>

    <table v-else>
      <thead>
      <tr>
        <th>Agency</th>
        <th>Recipient</th>
        <th class="right">Amount</th>
        <th>Action date</th>
      </tr>
      </thead>
      <tbody>
      <tr v-for="t in rows" :key="t.id">
        <td>{{ t.agency ?? '—' }}</td>
        <td>{{ t.recipient ?? '—' }}</td>
        <td class="right amount">{{ t.amount != null ? money.format(t.amount) : '—' }}</td>
        <td>{{ t.actionDate ?? '—' }}</td>
      </tr>
      </tbody>
    </table>
  </main>
</template>

<style scoped>
header { background: #0b5a4e; color: #fff; padding: 16px 24px; }
header h1 { margin: 0; font-size: 20px; }
header p { margin: 4px 0 0; opacity: .85; font-size: 13px; }
main { max-width: 1000px; margin: 24px auto; padding: 0 16px; }
.bar { display: flex; align-items: center; gap: 12px; margin-bottom: 12px; }
.bar button { padding: 6px 12px; border: 1px solid #ccc; background: #fff; border-radius: 6px; cursor: pointer; }
.bar button:disabled { opacity: .4; cursor: default; }
.muted { color: #666; font-size: 13px; }
table { width: 100%; border-collapse: collapse; background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 3px rgba(0,0,0,.08); }
th, td { text-align: left; padding: 10px 12px; border-bottom: 1px solid #eee; font-size: 14px; }
th { background: #f0f3f2; font-weight: 600; }
.right { text-align: right; }
td.amount { font-variant-numeric: tabular-nums; font-weight: 600; }
</style>