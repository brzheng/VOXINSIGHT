export function durationLabel(topic) {
  if (topic.durationMin === topic.durationMax) return `建议时长 ${topic.durationMin} 分钟`
  return `建议时长 ${topic.durationMin}–${topic.durationMax} 分钟`
}
