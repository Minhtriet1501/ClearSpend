const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

async function getJson(path) {
    const res = await fetch(`${API_BASE}${path}`)
    if (!res.ok) throw new Error('HTTP ' + res.status)
    return res.json()
}

export function fetchTransactions({ page = 0, size = 20, sort = 'amount,desc' } = {}) {
    return getJson(`/transactions?page=${page}&size=${size}&sort=${sort}`)
}

export function fetchFlags() {
    return getJson('/flags')
}

export function fetchStats() {
    return getJson('/stats')
}

export function fetchAgencies() {
    return getJson('/transactions/agencies')
}

export function searchTransactions({
    agency, recipient, minAmount, maxAmount, source, dateFrom, dateTo, severity,
    page = 0, size = 20, sort = 'amount,desc',
} = {}) {
    const p = new URLSearchParams({ page, size, sort })
    const set = (k, v) => { if (v !== null && v !== '' && v !== undefined) p.set(k, v) }
    set('agency', agency)
    set('recipient', recipient)
    set('minAmount', minAmount)
    set('maxAmount', maxAmount)
    set('source', source)
    set('dateFrom', dateFrom)
    set('dateTo', dateTo)
    set('severity', severity)
    return getJson(`/transactions/search?${p}`)
}
