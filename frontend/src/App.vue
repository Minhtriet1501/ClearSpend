<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { searchTransactions, fetchFlags, fetchStats } from './api.js'

const SIZE = 20
const view = ref('tx')

// transactions
const rows = ref([])
const info = ref({ number: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const page = ref(0)
const q = reactive({ agency: '', recipient: '', minAmount: '' })
const loading = ref(false)
const error = ref('')

// flags
const flags = ref([])
const flagsLoading = ref(false)
const flagsError = ref('')
const flagsByTx = ref({})

// stats (summary totals)
const stats = ref(null)

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })
const nfmt = v => (v ?? 0).toLocaleString('en-US')
const fmtDate = s => s ? new Date(s).toISOString().slice(0, 10) : ''

async function load() {
  loading.value = true; error.value = ''
  try {
    const d = await searchTransactions({ ...q, page: page.value, size: SIZE })
    rows.value = d.content; info.value = d
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

async function loadFlags() {
  flagsLoading.value = true; flagsError.value = ''
  try {
    const f = await fetchFlags()
    flags.value = f
    const map = {}
    for (const x of f) {
      map[x.transactionId] = (map[x.transactionId] === 'HIGH' || x.severity === 'HIGH') ? 'HIGH' : x.severity
    }
    flagsByTx.value = map
  } catch (e) { flagsError.value = e.message } finally { flagsLoading.value = false }
}

async function loadStats() {
  try { stats.value = await fetchStats() } catch (e) { stats.value = null }
}

function sevOf(t) { return flagsByTx.value[t.id] || null }
function doSearch() { page.value = 0; load() }
function clearSearch() { q.agency = ''; q.recipient = ''; q.minAmount = ''; page.value = 0; load() }
function prev() { if (!info.value.first) { page.value--; load() } }
function next() { if (!info.value.last) { page.value++; load() } }

// summary with graceful fallback when /stats is not available yet
const sTransactions = computed(() => stats.value?.transactions ?? info.value.totalElements)
const sTotalValue = computed(() => stats.value ? money.format(stats.value.totalValue) : '…')
const sFlagged = computed(() => stats.value?.flags ?? flags.value.length)
const sHigh = computed(() => stats.value?.flagsHigh ?? flags.value.filter(f => f.severity === 'HIGH').length)

onMounted(() => { loadStats(); loadFlags(); load() })
</script>

<template>
  <div class="page">
    <header class="masthead">
      <div class="wordmark">CLEARSPEND</div>
      <div class="tagline">Public spending, itemized</div>
    </header>

    <nav class="switch">
      <button class="seg" :aria-pressed="view === 'tx'" @click="view = 'tx'">Transactions</button>
      <button class="seg" :aria-pressed="view === 'flags'" @click="view = 'flags'">
        Flags <span class="c">{{ nfmt(sFlagged) }}</span>
      </button>
    </nav>

    <main class="receipt">
      <div class="rhead"><span>Statement &middot; FY 2024</span><span>No. 0001</span></div>
      <hr class="dash">

      <div class="summary">
        <div class="trow"><span class="k">Transactions</span><span class="dots"></span><span class="v">{{ nfmt(sTransactions) }}</span></div>
        <div class="trow em"><span class="k">Total value</span><span class="dots"></span><span class="v">{{ sTotalValue }}</span></div>
        <div class="trow"><span class="k">Flagged</span><span class="dots"></span><span class="v">{{ nfmt(sFlagged) }}</span></div>
        <div class="trow"><span class="k">High severity</span><span class="dots"></span><span class="v">{{ nfmt(sHigh) }}</span></div>
      </div>
      <hr class="dash">

      <!-- Transactions -->
      <section v-show="view === 'tx'">
        <div class="filters">
          <div class="fld"><label for="f-ag">Agency</label><input id="f-ag" class="ipt" v-model="q.agency" @keyup.enter="doSearch" placeholder="All agencies"></div>
          <div class="fld"><label for="f-rc">Recipient</label><input id="f-rc" class="ipt" v-model="q.recipient" @keyup.enter="doSearch" placeholder="All recipients"></div>
          <div class="fld"><label for="f-mn">Min amount</label><input id="f-mn" class="ipt" v-model="q.minAmount" inputmode="numeric" @keyup.enter="doSearch" placeholder="Any amount"></div>
          <div class="acts"><button class="btn primary" @click="doSearch">Search</button><button class="btn" @click="clearSearch">Reset</button></div>
        </div>

        <p v-if="loading" class="state">Loading…</p>
        <p v-else-if="error" class="state">Could not load transactions. Is the API running? ({{ error }})</p>
        <template v-else>
          <div class="table-wrap">
            <table>
              <thead><tr><th>Agency / Recipient</th><th class="num">Amount</th><th>Date</th><th class="num">Status</th></tr></thead>
              <tbody>
                <tr v-for="t in rows" :key="t.id">
                  <td><div class="agency">{{ t.agency ?? '' }}</div><div class="recipient">{{ t.recipient ?? '' }}</div></td>
                  <td class="num amount">{{ t.amount != null ? money.format(t.amount) : '' }}</td>
                  <td class="date">{{ t.actionDate ?? '' }}</td>
                  <td class="num">
                    <span v-if="sevOf(t) === 'HIGH'" class="tag high">High</span>
                    <span v-else-if="sevOf(t) === 'MEDIUM'" class="tag med">Medium</span>
                    <span v-else class="tag normal">Normal</span>
                  </td>
                </tr>
                <tr v-if="rows.length === 0"><td colspan="4" class="state">No results.</td></tr>
              </tbody>
            </table>
          </div>
          <div class="subtotal"><span>Shown {{ rows.length }}</span><span class="dots"></span><span>of {{ nfmt(info.totalElements) }}</span></div>
          <div class="pager">
            <button :disabled="info.first" @click="prev">&lsaquo; Prev</button>
            <span class="pos">Page {{ info.number + 1 }} / {{ info.totalPages || 1 }}</span>
            <button :disabled="info.last" @click="next">Next &rsaquo;</button>
          </div>
        </template>
      </section>

      <!-- Flags -->
      <section v-show="view === 'flags'">
        <p v-if="flagsLoading" class="state">Loading…</p>
        <p v-else-if="flagsError" class="state">Could not load flags. ({{ flagsError }})</p>
        <p v-else-if="flags.length === 0" class="state">No flags yet. Run the anomaly job to populate them.</p>
        <template v-else>
          <div class="flags">
            <div v-for="f in flags" :key="f.id" class="slip">
              <div class="top">
                <span v-if="f.severity === 'HIGH'" class="tag high">High</span>
                <span v-else class="tag med">Medium</span>
                <span class="rule">{{ f.rule }}</span>
              </div>
              <p class="explain">{{ f.explanation }}</p>
              <div class="meta"><span><b>Flagged</b> {{ fmtDate(f.createdAt) }}</span></div>
            </div>
          </div>
          <div class="subtotal"><span>Shown {{ flags.length }}</span><span class="dots"></span><span>of {{ nfmt(sFlagged) }} flags</span></div>
        </template>
      </section>
    </main>

    <footer class="foot">ClearSpend &copy; 2026 &middot; Public spending transparency</footer>
  </div>
</template>

<style scoped>
.page { max-width: 720px; margin: 0 auto; padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); padding-block: var(--s4); }

.masthead { text-align: center; margin-bottom: var(--s3); }
.wordmark { font-family: var(--mono); font-weight: 700; font-size: 30px; letter-spacing: .18em; text-indent: .18em; }
.tagline { font-family: var(--mono); font-weight: 500; font-size: 11px; letter-spacing: .26em; text-indent: .26em; color: var(--ink-3); text-transform: uppercase; margin-top: 6px; }

.switch { display: flex; gap: var(--s2); margin-bottom: var(--s3); }
.seg { flex: 1; min-width: 0; height: 56px; border: 1.5px solid var(--ink); border-radius: var(--radius); background: var(--paper); color: var(--ink); font-family: var(--mono); font-weight: 700; font-size: 15px; letter-spacing: .08em; text-transform: uppercase; display: flex; align-items: center; justify-content: center; gap: var(--s1); cursor: pointer; }
.seg .c { font-weight: 500; font-size: 12px; color: var(--ink-2); }
.seg:hover { background: var(--bg); }
.seg[aria-pressed="true"] { background: var(--ink); color: var(--paper); }
.seg[aria-pressed="true"] .c { color: rgba(255,255,255,.7); }

.receipt { position: relative; background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius) var(--radius) 0 0; border-bottom: 0; padding: var(--s4); }
.receipt::after { content: ""; position: absolute; left: -1px; right: -1px; bottom: -9px; height: 10px; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='10'%3E%3Cpath d='M0,0H14L7,10Z' fill='%23ffffff'/%3E%3C/svg%3E"); background-repeat: repeat-x; background-size: 14px 10px; }
.rhead { display: flex; justify-content: space-between; font-family: var(--mono); font-size: 11px; letter-spacing: .14em; text-transform: uppercase; color: var(--ink-2); }
.dash { border: 0; border-top: 1.5px dashed var(--ink); margin: var(--s3) 0; opacity: .85; }

.summary { display: grid; gap: 10px; font-family: var(--mono); font-size: 13px; }
.trow { display: flex; align-items: baseline; gap: var(--s1); }
.trow .k { color: var(--ink-2); letter-spacing: .06em; text-transform: uppercase; }
.trow .dots { flex: 1; border-bottom: 1.5px dotted var(--line); transform: translateY(-4px); }
.trow .v { font-weight: 600; font-variant-numeric: tabular-nums; }
.trow.em .v { font-size: 15px; }

.filters { display: flex; align-items: flex-end; gap: var(--s2); flex-wrap: wrap; margin-bottom: var(--s3); }
.fld { display: flex; flex-direction: column; gap: 6px; flex: 1 1 150px; min-width: 0; }
.fld label { font-family: var(--mono); font-size: 11px; letter-spacing: .1em; text-transform: uppercase; color: var(--ink-2); }
.ipt { height: 38px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--paper); padding: 0 11px; font-family: var(--mono); font-size: 13px; color: var(--ink); width: 100%; }
.ipt::placeholder { color: var(--ink-3); }
.ipt:focus-visible { outline: none; border-color: var(--ink); }
.acts { display: flex; gap: var(--s1); }
.btn { height: 38px; padding: 0 18px; border-radius: var(--radius); border: 1.5px solid var(--ink); background: var(--paper); color: var(--ink); font-family: var(--mono); font-weight: 600; font-size: 13px; letter-spacing: .04em; cursor: pointer; }
.btn:hover { background: var(--bg); }
.btn.primary { background: var(--ink); color: var(--paper); }
.btn.primary:hover { opacity: .88; }

.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; min-width: 560px; }
thead th { text-align: left; font-family: var(--mono); font-size: 11px; letter-spacing: .1em; text-transform: uppercase; color: var(--ink-2); font-weight: 600; padding: 0 var(--s2) 10px 0; border-bottom: 1.5px solid var(--ink); white-space: nowrap; }
thead th:last-child, td:last-child { padding-right: 0; }
th.num, td.num { text-align: right; }
tbody td { padding: 13px var(--s2) 13px 0; border-bottom: 1px dashed var(--line); vertical-align: baseline; }
tbody tr:last-child td { border-bottom: 0; }
.agency { font-weight: 600; }
.recipient { color: var(--ink-2); font-size: 13px; }
td.amount { font-family: var(--mono); font-weight: 500; font-variant-numeric: tabular-nums; white-space: nowrap; }
td.date { font-family: var(--mono); font-size: 12px; color: var(--ink-2); white-space: nowrap; }

.tag { display: inline-flex; align-items: center; height: 20px; padding: 0 7px; border-radius: var(--radius); font-family: var(--mono); font-size: 10px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
.tag.high { background: var(--ink); color: var(--paper); }
.tag.med { border: 1.5px solid var(--ink); color: var(--ink); }
.tag.normal { color: var(--ink-3); }

.subtotal { display: flex; align-items: baseline; gap: var(--s1); font-family: var(--mono); font-size: 12px; color: var(--ink-2); margin-top: var(--s3); }
.subtotal .dots { flex: 1; border-bottom: 1.5px dotted var(--line); transform: translateY(-4px); }
.pager { display: flex; align-items: center; justify-content: center; gap: var(--s2); margin-top: var(--s3); font-family: var(--mono); font-size: 13px; }
.pager button { border: 1px solid var(--line); background: var(--paper); color: var(--ink); font-family: var(--mono); font-size: 13px; padding: 6px 12px; border-radius: var(--radius); cursor: pointer; }
.pager button:hover:not(:disabled) { border-color: var(--ink); }
.pager button:disabled { color: var(--ink-3); cursor: default; }
.pager .pos { color: var(--ink-2); letter-spacing: .04em; }

.flags { display: flex; flex-direction: column; gap: var(--s2); }
.slip { border: 1px solid var(--line); border-radius: var(--radius); padding: var(--s3); }
.slip .top { display: flex; align-items: center; gap: var(--s1); margin-bottom: 10px; flex-wrap: wrap; }
.slip .rule { font-family: var(--mono); font-size: 12px; letter-spacing: .06em; color: var(--ink-3); }
.slip .explain { margin: 0; font-size: 14px; }
.slip .explain b { font-weight: 600; }
.slip .meta { display: flex; gap: var(--s3); flex-wrap: wrap; margin-top: var(--s2); padding-top: var(--s2); border-top: 1px dashed var(--line); font-family: var(--mono); font-size: 11px; letter-spacing: .04em; color: var(--ink-3); }
.slip .meta b { color: var(--ink-2); font-weight: 500; }

.state { font-family: var(--mono); font-size: 13px; color: var(--ink-3); text-align: center; padding: var(--s4) 0; }

.foot { text-align: center; font-family: var(--mono); font-size: 11px; letter-spacing: .14em; text-transform: uppercase; color: var(--ink-3); margin-top: var(--s4); }

@media (max-width: 560px) {
  .wordmark { font-size: 24px; }
  .receipt { padding: var(--s3) var(--s2); }
  .seg { height: 50px; font-size: 13px; flex-direction: column; gap: 2px; }
}
</style>
