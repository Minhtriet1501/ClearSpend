<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from 'vue'
import { searchTransactions, fetchFlags, fetchStats, fetchAgencies } from './api.js'

const SIZE = 20
const view = ref('tx')

// transactions
const rows = ref([])
const info = ref({ number: 0, totalPages: 0, totalElements: 0, first: true, last: true })
const page = ref(0)
const loading = ref(false)
const error = ref('')

// filters
const q = reactive({ agency: '', recipient: '', minAmount: '', maxAmount: '', source: '', dateFrom: '', dateTo: '', sort: 'amount,desc' })
const severity = ref('')
const activeChip = ref('all')
const agencies = ref([])

// flags
const flags = ref([])
const flagsLoading = ref(false)
const flagsError = ref('')
const flagsByTx = ref({})
const flagsFull = ref({})

// stats + detail
const stats = ref(null)
const selected = ref(null)

const money = new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })
const nfmt = v => (v ?? 0).toLocaleString('en-US')
const fmtDate = s => s ? new Date(s).toISOString().slice(0, 10) : ''
const sourceLabel = s => s === 'USAspending' ? 'USAspending.gov' : s === 'MesaAZ' ? 'City of Mesa, AZ' : (s || '')

function agencyIcon(name) {
  const n = (name || '').toLowerCase()
  if (n.includes('defense') || n.includes('army') || n.includes('navy') || n.includes('air force')) return '#ic-defense'
  if (n.includes('energy')) return '#ic-energy'
  if (n.includes('health') || n.includes('human services')) return '#ic-health'
  if (n.includes('agricultur')) return '#ic-agri'
  if (n.includes('transport')) return '#ic-transport'
  if (n.includes('veteran')) return '#ic-veterans'
  if (n.includes('justice')) return '#ic-justice'
  if (n.includes('treasury') || n.includes('financ')) return '#ic-finance'
  if (n.includes('city of') || n.includes('mesa') || n.includes('county') || n.includes('town')) return '#ic-city'
  return '#ic-doc'
}

async function load() {
  loading.value = true; error.value = ''
  try {
    const d = await searchTransactions({ ...q, severity: severity.value, page: page.value, size: SIZE })
    rows.value = d.content; info.value = d
  } catch (e) { error.value = e.message } finally { loading.value = false }
}

async function loadFlags() {
  flagsLoading.value = true; flagsError.value = ''
  try {
    const f = await fetchFlags()
    flags.value = f
    const sev = {}, full = {}
    for (const x of f) {
      (full[x.transactionId] ||= []).push(x)
      sev[x.transactionId] = (sev[x.transactionId] === 'HIGH' || x.severity === 'HIGH') ? 'HIGH' : x.severity
    }
    flagsByTx.value = sev; flagsFull.value = full
  } catch (e) { flagsError.value = e.message } finally { flagsLoading.value = false }
}

async function loadStats() { try { stats.value = await fetchStats() } catch (e) { stats.value = null } }
async function loadAgencies() { try { agencies.value = await fetchAgencies() } catch (e) { agencies.value = [] } }

function sevOf(t) { return flagsByTx.value[t.id] || null }
const selectedFlags = computed(() => selected.value ? (flagsFull.value[selected.value.id] || []) : [])
const detailStatus = computed(() => {
  const fs = selectedFlags.value
  return fs.length ? (fs.some(f => f.severity === 'HIGH') ? 'High' : 'Medium') : 'Normal'
})

function doSearch() { page.value = 0; load() }
function prev() { if (!info.value.first) { page.value--; load() } }
function next() { if (!info.value.last) { page.value++; load() } }

function chip(key) {
  activeChip.value = key
  if (key === 'all') { severity.value = '' }
  else if (key === 'flagged') { severity.value = 'ANY' }
  else if (key === 'high') { severity.value = 'HIGH' }
  else if (key === 'big') { severity.value = ''; q.minAmount = '100000000' }
  else if (key === 'year') { severity.value = ''; const y = new Date().getFullYear(); q.dateFrom = `${y}-01-01`; q.dateTo = `${y}-12-31` }
  doSearch()
}

function resetFilters() {
  q.agency = ''; q.recipient = ''; q.minAmount = ''; q.maxAmount = ''; q.source = ''; q.dateFrom = ''; q.dateTo = ''; q.sort = 'amount,desc'
  severity.value = ''; activeChip.value = 'all'; page.value = 0; load()
}

const sTransactions = computed(() => stats.value?.transactions ?? info.value.totalElements)
const sTotalValue = computed(() => stats.value ? money.format(stats.value.totalValue) : '…')
const sFlagged = computed(() => stats.value?.flags ?? flags.value.length)
const sHigh = computed(() => stats.value?.flagsHigh ?? flags.value.filter(f => f.severity === 'HIGH').length)

function onKey(e) { if (e.key === 'Escape') selected.value = null }
onMounted(() => { loadStats(); loadAgencies(); loadFlags(); load(); window.addEventListener('keydown', onKey) })
onUnmounted(() => window.removeEventListener('keydown', onKey))
</script>

<template>
  <svg width="0" height="0" style="position:absolute" aria-hidden="true">
    <symbol id="ic-defense" viewBox="0 0 24 24"><path d="M12 3l7 2.5v5.5c0 4.3-2.9 7.3-7 8.5-4.1-1.2-7-4.2-7-8.5V5.5z"/></symbol>
    <symbol id="ic-energy" viewBox="0 0 24 24"><path d="M13 2.5 5.5 13H10l-1 8.5L18.5 10H13.5z"/></symbol>
    <symbol id="ic-health" viewBox="0 0 24 24"><path d="M9.5 3.5h5V9H20v5h-5.5v5.5h-5V14H4V9h5.5z"/></symbol>
    <symbol id="ic-agri" viewBox="0 0 24 24"><path d="M5 19C5 11 11 5 19 5c0 8-6 14-14 14z"/><path d="M6 18C10 14 14 10 18 7"/></symbol>
    <symbol id="ic-transport" viewBox="0 0 24 24"><path d="M2.5 6.5h11v9h-11z"/><path d="M13.5 9.5h4l3 3v3h-7z"/><circle cx="6.5" cy="17" r="1.6"/><circle cx="17.5" cy="17" r="1.6"/></symbol>
    <symbol id="ic-veterans" viewBox="0 0 24 24"><circle cx="12" cy="8.5" r="4.5"/><path d="M12 6.3l.9 1.8 2 .3-1.5 1.4.4 2-1.8-1-1.8 1 .4-2L9.1 8.4l2-.3z"/><path d="M9.3 12.4 7.8 20 12 17.8 16.2 20l-1.5-7.6"/></symbol>
    <symbol id="ic-justice" viewBox="0 0 24 24"><path d="M12 4v16M6.5 20h11M6 7.5l6-1.5 6 1.5"/><path d="M4 15a2 2 0 0 0 4 0l-2-5.5zM16 15a2 2 0 0 0 4 0l-2-5.5z"/></symbol>
    <symbol id="ic-finance" viewBox="0 0 24 24"><path d="M4 10h16M4 10l8-5 8 5M6.5 10v7M10.2 10v7M13.8 10v7M17.5 10v7M3.5 19.5h17"/></symbol>
    <symbol id="ic-city" viewBox="0 0 24 24"><path d="M4 20.5V8l6-3v15.5M10 20.5V11l6 3v6.5M3.5 20.5h17"/><path d="M6.5 9v0M6.5 12v0M13 15v0"/></symbol>
    <symbol id="ic-doc" viewBox="0 0 24 24"><path d="M7 3.5h7l4 4v13H7z"/><path d="M14 3.5v4h4"/><path d="M10 12.5h6M10 15.5h6"/></symbol>
  </svg>

  <div class="shell">
    <!-- detail (left) -->
    <aside class="detail-col" :class="{ open: !!selected }" aria-label="Transaction detail">
      <template v-if="selected">
        <div class="dhead">
          <span class="eyebrow">Transaction detail</span>
          <button class="xbtn" @click="selected = null" aria-label="Close detail"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button>
        </div>
        <div class="damt">{{ selected.amount != null ? money.format(selected.amount) : '' }}</div>
        <div class="dparty">
          <svg class="aic"><use :href="agencyIcon(selected.agency)"/></svg>
          <div><span class="dagency">{{ selected.agency }}</span><span class="drecipient">{{ selected.recipient }}</span></div>
        </div>
        <hr class="dash">
        <div class="summary">
          <div class="trow"><span class="k">Source</span><span class="dots"></span><span class="v">{{ sourceLabel(selected.source) }}</span></div>
          <div class="trow"><span class="k">Action date</span><span class="dots"></span><span class="v">{{ selected.actionDate ?? '' }}</span></div>
          <div class="trow"><span class="k">External ID</span><span class="dots"></span><span class="v dwrap">{{ selected.externalId }}</span></div>
          <div class="trow"><span class="k">Status</span><span class="dots"></span><span class="v">{{ detailStatus }}</span></div>
        </div>
        <hr class="dash">
        <div class="dsec-label">Anomaly flags</div>
        <div v-if="selectedFlags.length === 0" class="dnone">No anomalies flagged on this transaction.</div>
        <div v-for="fl in selectedFlags" :key="fl.id" class="dflag" :class="{ high: fl.severity === 'HIGH' }">
          <div class="top"><span class="tag" :class="fl.severity === 'HIGH' ? 'high' : 'med'">{{ fl.severity === 'HIGH' ? 'High' : 'Medium' }}</span><span class="rule">{{ fl.rule }}</span></div>
          <p>{{ fl.explanation }}</p>
        </div>
      </template>
    </aside>

    <!-- main (right) -->
    <div class="main-col">
      <header class="masthead">
        <svg class="brandmark" viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
          <path d="M6 5.5a1 1 0 0 1 1-1h8.5a1 1 0 0 1 1 1V17"/>
          <path d="M6 5.5V25l2-1.4 2 1.4 2-1.4"/>
          <path d="M9 9.5h5M9 12.8h5"/>
          <circle cx="20" cy="19.5" r="5.2" fill="#ffffff"/>
          <path d="M23.8 23.3 27.5 27"/>
        </svg>
        <div class="wordmark">CLEARSPEND</div>
        <div class="tagline">Public spending, itemized</div>
      </header>

      <nav class="switch">
        <button class="seg" :aria-pressed="view === 'tx'" @click="view = 'tx'">Transactions</button>
        <button class="seg" :aria-pressed="view === 'flags'" @click="view = 'flags'">Flags <span class="c">{{ nfmt(sFlagged) }}</span></button>
      </nav>

      <main class="receipt">
        <div class="rhead"><span>Statement &middot; FY 2024</span><span>No. 0001</span></div>
        <hr class="dash">
        <div class="summary">
          <div class="trow"><span class="k">Transactions</span><span class="dots"></span><span class="v">{{ nfmt(sTransactions) }}</span></div>
          <div class="trow em"><span class="k">Total value</span><span class="dots"></span><span class="v">{{ sTotalValue }}</span></div>
          <div class="trow alert"><span class="k">Flagged</span><span class="dots"></span><span class="v">{{ nfmt(sFlagged) }}</span></div>
          <div class="trow alert"><span class="k">High severity</span><span class="dots"></span><span class="v">{{ nfmt(sHigh) }}</span></div>
        </div>
        <hr class="dash">

        <!-- Transactions -->
        <section v-show="view === 'tx'">
          <div class="filters">
            <div class="chips">
              <button class="chip" :class="{ active: activeChip === 'all' }" @click="chip('all')">All</button>
              <button class="chip" :class="{ active: activeChip === 'flagged' }" @click="chip('flagged')">Flagged</button>
              <button class="chip" :class="{ active: activeChip === 'high' }" @click="chip('high')">High severity</button>
              <button class="chip" :class="{ active: activeChip === 'big' }" @click="chip('big')">Over $100M</button>
              <button class="chip" :class="{ active: activeChip === 'year' }" @click="chip('year')">This year</button>
            </div>
            <div class="fgrid">
              <div class="fld"><label>Agency</label>
                <select class="ipt" v-model="q.agency" @change="doSearch">
                  <option value="">All agencies</option>
                  <option v-for="a in agencies" :key="a" :value="a">{{ a }}</option>
                </select>
              </div>
              <div class="fld"><label>Recipient</label><input class="ipt" v-model="q.recipient" @keyup.enter="doSearch" placeholder="All recipients"></div>
              <div class="fld"><label>Source</label>
                <select class="ipt" v-model="q.source" @change="doSearch">
                  <option value="">All sources</option>
                  <option value="USAspending">USAspending.gov</option>
                  <option value="MesaAZ">City of Mesa, AZ</option>
                </select>
              </div>
              <div class="fld"><label>Min amount</label><input class="ipt" v-model="q.minAmount" inputmode="numeric" @keyup.enter="doSearch" placeholder="Any"></div>
              <div class="fld"><label>Max amount</label><input class="ipt" v-model="q.maxAmount" inputmode="numeric" @keyup.enter="doSearch" placeholder="Any"></div>
              <div class="fld"><label>From date</label><input class="ipt" type="date" v-model="q.dateFrom" @change="doSearch"></div>
              <div class="fld"><label>To date</label><input class="ipt" type="date" v-model="q.dateTo" @change="doSearch"></div>
              <div class="fld"><label>Sort</label>
                <select class="ipt" v-model="q.sort" @change="doSearch">
                  <option value="amount,desc">Amount high to low</option>
                  <option value="amount,asc">Amount low to high</option>
                  <option value="actionDate,desc">Date newest</option>
                  <option value="actionDate,asc">Date oldest</option>
                </select>
              </div>
            </div>
            <div class="acts"><button class="btn primary" @click="doSearch">Search</button><button class="btn" @click="resetFilters">Reset</button></div>
          </div>

          <p v-if="loading" class="state">Loading…</p>
          <p v-else-if="error" class="state">Could not load transactions. Is the API running? ({{ error }})</p>
          <template v-else>
            <div class="table-wrap">
              <table>
                <thead><tr><th>Agency / Recipient</th><th class="num">Amount</th><th>Date</th><th class="num">Status</th></tr></thead>
                <tbody>
                  <tr v-for="t in rows" :key="t.id" :class="{ sel: selected && selected.id === t.id }" @click="selected = t">
                    <td>
                      <div class="agency"><svg class="aic"><use :href="agencyIcon(t.agency)"/></svg>{{ t.agency ?? '' }}</div>
                      <div class="recipient">{{ t.recipient ?? '' }}</div>
                    </td>
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
              <div v-for="f in flags" :key="f.id" class="slip" :class="{ high: f.severity === 'HIGH' }">
                <div class="top">
                  <span class="tag" :class="f.severity === 'HIGH' ? 'high' : 'med'">{{ f.severity === 'HIGH' ? 'High' : 'Medium' }}</span>
                  <span class="rule">{{ f.rule }}</span>
                </div>
                <p class="explain">{{ f.explanation }}</p>
                <div class="meta"><span>Flagged {{ fmtDate(f.createdAt) }}</span></div>
              </div>
            </div>
            <div class="subtotal"><span>Shown {{ flags.length }}</span><span class="dots"></span><span>of {{ nfmt(sFlagged) }} flags</span></div>
          </template>
        </section>
      </main>

      <footer class="foot">ClearSpend &copy; 2026 &middot; Public spending transparency</footer>
    </div>
  </div>

  <div class="backdrop" :class="{ open: !!selected }" @click="selected = null"></div>
</template>

<style scoped>
.shell { display: grid; grid-template-columns: 1fr minmax(0, 720px) 1fr; gap: var(--s3); align-items: start; max-width: 1400px; margin: 0 auto; padding-left: max(16px, env(safe-area-inset-left, 0px)); padding-right: max(16px, env(safe-area-inset-right, 0px)); padding-block: var(--s4); }
.main-col { grid-column: 2; min-width: 0; }

.masthead { text-align: center; margin-bottom: var(--s3); }
.brandmark { width: 40px; height: 40px; color: var(--ink); margin-bottom: 10px; }
.wordmark { font-family: var(--mono); font-weight: 700; font-size: 30px; letter-spacing: .18em; text-indent: .18em; }
.tagline { font-family: var(--mono); font-weight: 500; font-size: 11px; letter-spacing: .26em; text-indent: .26em; color: var(--ink-3); text-transform: uppercase; margin-top: 6px; }

.switch { display: flex; gap: var(--s2); margin-bottom: var(--s3); }
.seg { flex: 1; min-width: 0; height: 56px; border: 1.5px solid var(--ink); border-radius: var(--radius); background: var(--paper); color: var(--ink); font-family: var(--mono); font-weight: 700; font-size: 15px; letter-spacing: .08em; text-transform: uppercase; display: flex; align-items: center; justify-content: center; gap: var(--s1); cursor: pointer; transition: background .12s, transform .04s; }
.seg .c { font-weight: 600; font-size: 12px; color: var(--red); }
.seg:hover { background: var(--bg); }
.seg:active { background: var(--press); transform: translateY(1px); }
.seg:focus-visible { outline: 2px solid var(--ink); outline-offset: 2px; }
.seg[aria-pressed="true"] { background: var(--ink); color: var(--paper); }
.seg[aria-pressed="true"] .c { color: #ff8a7a; }
.seg[aria-pressed="true"]:hover { background: #000; }

.receipt { position: relative; background: var(--paper); border: 1px solid var(--line); border-radius: var(--radius) var(--radius) 0 0; border-bottom: 0; padding: var(--s4); }
.receipt::after { content: ""; position: absolute; left: -1px; right: -1px; bottom: -9px; height: 10px; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='10'%3E%3Cpath d='M0,0H14L7,10Z' fill='%23ffffff'/%3E%3C/svg%3E"); background-repeat: repeat-x; background-size: 14px 10px; }
.rhead { display: flex; justify-content: space-between; font-family: var(--mono); font-size: 11px; letter-spacing: .14em; text-transform: uppercase; color: var(--ink-2); }
.dash { border: 0; border-top: 1.5px dashed var(--ink); margin: var(--s3) 0; opacity: .85; }

.summary { display: grid; gap: 10px; font-family: var(--mono); font-size: 13px; }
.trow { display: flex; align-items: baseline; gap: var(--s1); }
.trow .k { color: var(--ink-2); letter-spacing: .06em; text-transform: uppercase; }
.trow .dots { flex: 1; border-bottom: 1.5px dotted var(--line); transform: translateY(-4px); }
.trow .v { font-weight: 600; font-variant-numeric: tabular-nums; }
.trow .v.dwrap { overflow-wrap: anywhere; }
.trow.em .v { font-size: 15px; }
.trow.alert .k, .trow.alert .v { color: var(--red); }

.filters { margin-bottom: var(--s3); }
.chips { display: flex; gap: var(--s1); flex-wrap: wrap; margin-bottom: var(--s2); }
.chip { height: 28px; padding: 0 12px; border: 1px solid var(--line); border-radius: 999px; background: var(--paper); color: var(--ink-2); font-family: var(--mono); font-size: 11px; font-weight: 600; letter-spacing: .04em; text-transform: uppercase; cursor: pointer; transition: background .12s, transform .04s, color .12s; }
.chip:hover { background: var(--bg); border-color: var(--ink-3); color: var(--ink); }
.chip:active { transform: translateY(1px); }
.chip:focus-visible { outline: 2px solid var(--ink); outline-offset: 2px; }
.chip.active { background: var(--ink); border-color: var(--ink); color: var(--paper); }
.fgrid { display: grid; grid-template-columns: repeat(auto-fit, minmax(140px, 1fr)); gap: var(--s2); margin-bottom: var(--s3); }
.fld { display: flex; flex-direction: column; gap: 6px; min-width: 0; }
.fld label { font-family: var(--mono); font-size: 11px; letter-spacing: .1em; text-transform: uppercase; color: var(--ink-2); }
.ipt { height: 38px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--paper); padding: 0 11px; font-family: var(--mono); font-size: 13px; color: var(--ink); width: 100%; transition: border-color .12s; }
.ipt::placeholder { color: var(--ink-3); }
.ipt:hover { border-color: var(--ink-3); }
.ipt:focus-visible { outline: none; border-color: var(--ink); box-shadow: 0 0 0 2px rgba(26,26,26,.1); }
select.ipt { appearance: none; -webkit-appearance: none; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6'%3E%3Cpath d='M1 1l4 4 4-4' fill='none' stroke='%235c5c5c' stroke-width='1.3'/%3E%3C/svg%3E"); background-repeat: no-repeat; background-position: right 11px center; padding-right: 28px; cursor: pointer; }
.acts { display: flex; gap: var(--s1); }

.btn { height: 38px; padding: 0 18px; border-radius: var(--radius); border: 1.5px solid var(--ink); background: var(--paper); color: var(--ink); font-family: var(--mono); font-weight: 600; font-size: 13px; letter-spacing: .04em; cursor: pointer; transition: background .12s, transform .04s, opacity .12s; }
.btn:hover { background: var(--bg); }
.btn:active { background: var(--press); transform: translateY(1px); }
.btn:focus-visible { outline: 2px solid var(--ink); outline-offset: 2px; }
.btn:disabled { opacity: .4; cursor: not-allowed; background: var(--paper); transform: none; }
.btn.primary { background: var(--ink); color: var(--paper); }
.btn.primary:hover { background: #000; }
.btn.primary:active { background: #000; transform: translateY(1px); }

.table-wrap { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; min-width: 480px; }
thead th { text-align: left; font-family: var(--mono); font-size: 11px; letter-spacing: .1em; text-transform: uppercase; color: var(--ink-2); font-weight: 600; padding: 0 var(--s2) 10px 0; border-bottom: 1.5px solid var(--ink); white-space: nowrap; }
thead th:last-child, td:last-child { padding-right: 0; }
th.num, td.num { text-align: right; }
tbody tr { cursor: pointer; transition: background .1s; }
tbody tr:hover { background: var(--bg); }
tbody tr:active { background: var(--press); }
tbody tr.sel { background: var(--press); }
tbody td { padding: 13px var(--s2) 13px 0; border-bottom: 1px dashed var(--line); vertical-align: baseline; }
tbody tr:last-child td { border-bottom: 0; }
.agency { font-weight: 600; display: flex; align-items: center; gap: 7px; }
.recipient { color: var(--ink-2); font-size: 13px; padding-left: 21px; }
td.amount { font-family: var(--mono); font-weight: 500; font-variant-numeric: tabular-nums; white-space: nowrap; }
td.date { font-family: var(--mono); font-size: 12px; color: var(--ink-2); white-space: nowrap; }
.aic { width: 14px; height: 14px; flex: none; color: var(--ink-2); fill: none; stroke: currentColor; stroke-width: 1.7; stroke-linecap: round; stroke-linejoin: round; }

.tag { display: inline-flex; align-items: center; height: 20px; padding: 0 7px; border-radius: var(--radius); font-family: var(--mono); font-size: 10px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; }
.tag.high { background: var(--ink); color: var(--paper); }
.tag.med { border: 1.5px solid var(--ink); color: var(--ink); }
.tag.normal { color: var(--ink-3); }

.subtotal { display: flex; align-items: baseline; gap: var(--s1); font-family: var(--mono); font-size: 12px; color: var(--ink-2); margin-top: var(--s3); }
.subtotal .dots { flex: 1; border-bottom: 1.5px dotted var(--line); transform: translateY(-4px); }
.pager { display: flex; align-items: center; justify-content: center; gap: var(--s2); margin-top: var(--s3); font-family: var(--mono); font-size: 13px; }
.pager button { border: 1px solid var(--line); background: var(--paper); color: var(--ink); font-family: var(--mono); font-size: 13px; padding: 6px 12px; border-radius: var(--radius); cursor: pointer; transition: background .12s, transform .04s; }
.pager button:hover:not(:disabled) { background: var(--bg); border-color: var(--ink-3); }
.pager button:active:not(:disabled) { background: var(--press); transform: translateY(1px); }
.pager button:focus-visible { outline: 2px solid var(--ink); outline-offset: 2px; }
.pager button:disabled { color: var(--ink-3); cursor: not-allowed; opacity: .55; }
.pager .pos { color: var(--ink-2); letter-spacing: .04em; }

.flags { display: flex; flex-direction: column; gap: var(--s2); }
.slip { border: 1px solid var(--line); border-left: 3px solid var(--line); border-radius: var(--radius); padding: var(--s3); }
.slip.high { border-left-color: var(--red); }
.slip .top { display: flex; align-items: center; gap: var(--s1); margin-bottom: 10px; flex-wrap: wrap; }
.slip .rule { font-family: var(--mono); font-size: 12px; letter-spacing: .06em; color: var(--ink-3); }
.slip .explain { margin: 0; font-size: 14px; }
.slip .explain b { font-weight: 600; }
.slip .meta { display: flex; gap: var(--s3); flex-wrap: wrap; margin-top: var(--s2); padding-top: var(--s2); border-top: 1px dashed var(--line); font-family: var(--mono); font-size: 11px; letter-spacing: .04em; color: var(--ink-3); }

.state { font-family: var(--mono); font-size: 13px; color: var(--ink-3); text-align: center; padding: var(--s4) 0; }
.foot { text-align: center; font-family: var(--mono); font-size: 11px; letter-spacing: .14em; text-transform: uppercase; color: var(--ink-3); margin-top: var(--s4); }

/* detail: left column on wide screens, drawer on narrow */
.detail-col { grid-column: 1; justify-self: end; align-self: start; width: min(330px, 100%); min-width: 0; position: sticky; top: var(--s4); background: var(--paper); border: 1px solid var(--line); border-bottom: 0; border-radius: var(--radius) var(--radius) 0 0; padding: var(--s3); display: flex; flex-direction: column; gap: var(--s2); opacity: 0; transform: translateX(-12px); pointer-events: none; transition: opacity .18s ease, transform .2s ease; }
.detail-col.open { opacity: 1; transform: none; pointer-events: auto; }
.detail-col::after { content: ""; position: absolute; left: -1px; right: -1px; bottom: -9px; height: 10px; background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='14' height='10'%3E%3Cpath d='M0,0H14L7,10Z' fill='%23ffffff'/%3E%3C/svg%3E"); background-repeat: repeat-x; background-size: 14px 10px; }
.dhead { display: flex; align-items: center; justify-content: space-between; }
.eyebrow { font-family: var(--mono); font-size: 11px; letter-spacing: .16em; text-transform: uppercase; color: var(--ink-2); }
.xbtn { width: 32px; height: 32px; border: 1px solid var(--line); border-radius: var(--radius); background: var(--paper); color: var(--ink); display: grid; place-items: center; cursor: pointer; transition: background .12s, transform .04s; }
.xbtn:hover { background: var(--bg); border-color: var(--ink-3); }
.xbtn:active { background: var(--press); transform: translateY(1px); }
.xbtn:focus-visible { outline: 2px solid var(--ink); outline-offset: 2px; }
.xbtn svg { width: 15px; height: 15px; fill: none; stroke: currentColor; stroke-width: 1.8; stroke-linecap: round; }
.damt { font-family: var(--mono); font-size: 26px; font-weight: 700; font-variant-numeric: tabular-nums; }
.dparty { display: flex; align-items: flex-start; gap: 9px; }
.dparty .aic { width: 16px; height: 16px; margin-top: 3px; }
.dparty .dagency { font-weight: 600; display: block; }
.dparty .drecipient { color: var(--ink-2); font-size: 13px; display: block; }
.dsec-label { font-family: var(--mono); font-size: 11px; letter-spacing: .12em; text-transform: uppercase; color: var(--ink-3); margin-bottom: var(--s1); }
.dnone { font-family: var(--mono); font-size: 12px; color: var(--ink-3); }
.dflag { border: 1px solid var(--line); border-left: 3px solid var(--line); border-radius: var(--radius); padding: var(--s2); margin-top: var(--s1); }
.dflag.high { border-left-color: var(--red); }
.dflag .top { display: flex; align-items: center; gap: var(--s1); margin-bottom: 7px; }
.dflag .rule { font-family: var(--mono); font-size: 11px; color: var(--ink-3); letter-spacing: .05em; }
.dflag p { margin: 0; font-size: 13px; }

.backdrop { position: fixed; inset: 0; background: rgba(26,26,26,.38); opacity: 0; pointer-events: none; transition: opacity .18s; z-index: 40; display: none; }
.backdrop.open { opacity: 1; pointer-events: auto; }

@media (max-width: 1199px) {
  .detail-col { position: fixed; top: 0; left: 0; height: 100%; width: min(420px, 92vw); border: 0; border-right: 1px solid var(--line); border-radius: 0; padding: var(--s4) var(--s3); overflow-y: auto; z-index: 41; opacity: 1; transform: translateX(-100%); transition: transform .24s ease; }
  .detail-col.open { transform: translateX(0); }
  .detail-col::after { display: none; }
  .backdrop { display: block; }
}
@media (max-width: 560px) {
  .wordmark { font-size: 24px; }
  .receipt { padding: var(--s3) var(--s2); }
  .seg { height: 50px; font-size: 13px; flex-direction: column; gap: 2px; }
}
@media (prefers-reduced-motion: reduce) { * { transition: none !important; } }
</style>
