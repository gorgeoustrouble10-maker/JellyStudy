/**
 * 将 UUID 等长 ID 格式化为更易读的短编号（非乱码，仅为展示）
 */
export function formatShortId(id) {
  if (!id) return '';
  if (/^q-demo-|^a-demo-/.test(id)) {
    return id;
  }
  if (id.length <= 12) {
    return id;
  }
  return `…${id.slice(-8)}`;
}
