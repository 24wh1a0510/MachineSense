export function riskClass(risk) {
  if (!risk) return 'unknown';
  return risk.toLowerCase();
}

export function healthColor(score) {
  if (score == null) return 'var(--text-muted)';
  if (score >= 70) return 'var(--status-healthy)';
  if (score >= 40) return 'var(--status-warning)';
  return 'var(--status-critical)';
}

export function riskLabel(risk) {
  return risk || 'UNKNOWN';
}
