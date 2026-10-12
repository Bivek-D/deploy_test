const BASE = "/api/v1";
let token = null;
export const setToken = (t) => (token = t);

async function raw(path, opts = {}) {
  const headers = { "Content-Type": "application/json" };
  if (token) headers.Authorization = `Bearer ${token}`;
  const t0 = performance.now();
  const res = await fetch(BASE + path, { ...opts, headers, credentials: "include" });
  let body = null;
  try { body = await res.json(); } catch {}
  return { status: res.status, ms: Math.round(performance.now() - t0), body };
}

export async function api(path, opts) {
  let r = await raw(path, opts);
  if (r.status === 401 && !path.startsWith("/auth/")) {
    const rr = await raw("/auth/refresh", { method: "POST" });
    if (rr.status === 200) { token = rr.body.accessToken; r = await raw(path, opts); }
  }
  return r;
}