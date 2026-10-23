import { useEffect, useState } from "react";

export type ServiceStatus =
  | { state: "checking" }
  | { state: "reachable"; checkedAt: Date }
  | { state: "unreachable"; checkedAt: Date };

/** Opaque cross-origin responses prove reachability, never backend health. */
export function useServiceStatus(url: string, timeoutMs = 6000): ServiceStatus {
  const [status, setStatus] = useState<ServiceStatus>({ state: "checking" });
  useEffect(() => {
    let active = true;
    const controller = new AbortController();
    const timer = window.setTimeout(() => controller.abort(), timeoutMs);
    setStatus({ state: "checking" });
    fetch(url, { signal: controller.signal, mode: "no-cors", cache: "no-store" })
      .then(() => {
        if (active) setStatus({ state: "reachable", checkedAt: new Date() });
      })
      .catch(() => {
        if (active) setStatus({ state: "unreachable", checkedAt: new Date() });
      })
      .finally(() => window.clearTimeout(timer));
    return () => {
      active = false;
      window.clearTimeout(timer);
      controller.abort();
    };
  }, [url, timeoutMs]);
  return status;
}
