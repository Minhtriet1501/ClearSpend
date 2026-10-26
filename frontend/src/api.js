const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export async function fetchTransactions({ page = 0, size = 20, sort = 'amount,desc' } = {}) {
    const res = await fetch(`${API_BASE}/transactions?page=${page}&size=${size}&sort=${sort}`)
    if (!res.ok) throw new Error('HTTP ' + res.status)
    return res.json()

}
export async function fetchFlags() {
    const res = await fetch(`${API_BASE}/flags`)
    if (!res.ok) throw new Error('HTTP ' + res.status)
    return res.json()
}

export async function searchTransactions({ agency, recipient, minAmount, page = 0, size = 20, sort = 'amount,desc' } = {}) {
    const p = new URLSearchParams({ page, size, sort })
    if (agency) p.set('agency', agency)
    if (recipient) p.set('recipient', recipient)
    if (minAmount !== null && minAmount !== '' && minAmount !== undefined) p.set('minAmount', minAmount)
    const res = await fetch(`${API_BASE}/transactions/search?${p}`)
    if (!res.ok) throw new Error('HTTP ' + res.status)
    return res.json()
}

export async function fetchStats() {
    const res = await fetch(`${API_BASE}/stats`)
    if (!res.ok) throw new Error('HTTP ' + res.status)
    return res.json()
}