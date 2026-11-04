import { useEffect, useState } from 'react';

/**
 * Theo dõi request chậm (Render free cold start > 3s) để hiện banner "máy chủ đang khởi động".
 * @reference docs/plan-app/01-phase-1.md §6
 */
const SLOW_AFTER_MS = 3000;

let activeRequests = 0;
let slow = false;
let timer: ReturnType<typeof setTimeout> | null = null;
const listeners = new Set<(slow: boolean) => void>();

function emit(): void {
  listeners.forEach((listener) => listener(slow));
}

export function beginRequest(): void {
  activeRequests += 1;
  if (activeRequests === 1 && !timer) {
    timer = setTimeout(() => {
      slow = true;
      emit();
    }, SLOW_AFTER_MS);
  }
}

export function endRequest(): void {
  activeRequests = Math.max(0, activeRequests - 1);
  if (activeRequests === 0) {
    if (timer) {
      clearTimeout(timer);
      timer = null;
    }
    if (slow) {
      slow = false;
      emit();
    }
  }
}

export function subscribeServerSlow(listener: (slow: boolean) => void): () => void {
  listeners.add(listener);
  return () => {
    listeners.delete(listener);
  };
}

/** Hook: true khi có request đang chạy quá 3 giây. */
export function useServerSlow(): boolean {
  const [value, setValue] = useState(slow);
  useEffect(() => subscribeServerSlow(setValue), []);
  return value;
}
