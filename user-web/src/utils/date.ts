export function formatLocalDate(date: Date): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

export function addDays(dateStr: string, days: number): string {
  const parts = dateStr.split('-').map(Number)
  const date = new Date(parts[0], parts[1] - 1, parts[2])
  date.setDate(date.getDate() + days)
  return formatLocalDate(date)
}

export function defaultCheckIn(): string {
  return addDays(formatLocalDate(new Date()), 1)
}

export function defaultCheckOut(): string {
  return addDays(formatLocalDate(new Date()), 2)
}
