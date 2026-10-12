import { useState } from "react";
import { api, setToken } from "./api";

export default function App() {
  const [log, setLog] = useState([]);
  const run = async (label, path, opts) => {
    const r = await api(path, opts);
    if (path === "/auth/login" && r.status === 200) setToken(r.body.accessToken);
    setLog((l) => [`${label}: ${r.status} in ${r.ms} ms ${JSON.stringify(r.body)}`, ...l]);
  };
  return (
    <div style={{ padding: 24, fontFamily: "sans-serif" }}>
      <button onClick={() => run("Ping", "/ping")}>Ping</button>{" "}
      <button onClick={() => run("Login", "/auth/login", { method: "POST" })}>Login</button>{" "}
      <button onClick={() => run("Me", "/me")}>Call /me</button>
      <pre>{log.join("\n\n")}</pre>
    </div>
  );
}