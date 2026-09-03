const API_BASE = import.meta.env.VITE_API_BASE ?? 'http://localhost:8080'

export async function fetchTransactions({ page = 0, size = 20, sort = 'amount,desc' } = {}) {
    const res = await fetch(`${API_BASE}/transactions?page=${page}&size=${size}&sort=${sort}`)
    if (!res.ok) throw new Error('HTTP ' + res.status)
    return res.json()
}