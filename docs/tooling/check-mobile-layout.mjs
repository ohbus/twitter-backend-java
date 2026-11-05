import { spawn } from "node:child_process";
import { mkdtemp, readFile, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";

const chrome =
  process.env.CHROME_PATH ??
  (process.platform === "darwin"
    ? "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
    : "google-chrome");
const profile = await mkdtemp(join(tmpdir(), "moo-docs-chrome-"));
const server = spawn("python3", ["-m", "http.server", "4173", "-d", "../../site"], {
  cwd: import.meta.dirname,
  stdio: "ignore",
});
for (let attempt = 0; attempt < 50; attempt++) {
  try {
    const response = await fetch("http://127.0.0.1:4173/");
    if (response.ok) break;
  } catch {}
  await new Promise((resolve) => setTimeout(resolve, 100));
}
const browser = spawn(
  chrome,
  [
    "--headless=new",
    "--no-sandbox",
    "--disable-dev-shm-usage",
    "--remote-debugging-port=0",
    `--user-data-dir=${profile}`,
    "http://127.0.0.1:4173/",
  ],
  { stdio: "ignore" },
);

const waitForTabs = async () => {
  for (let attempt = 0; attempt < 50; attempt++) {
    try {
      const [port] = (await readFile(join(profile, "DevToolsActivePort"), "utf8")).split("\n");
      const response = await fetch(`http://127.0.0.1:${port}/json`);
      if (response.ok) return response.json();
    } catch {}
    await new Promise((resolve) => setTimeout(resolve, 100));
  }
  throw new Error("Chrome did not start");
};

try {
  const tabs = await waitForTabs();
  const tab = tabs.find(({ type }) => type === "page");
  const socket = new WebSocket(tab.webSocketDebuggerUrl);
  const pending = new Map();
  let requestId = 0;
  socket.onmessage = ({ data }) => {
    const message = JSON.parse(data);
    pending.get(message.id)?.(message);
    pending.delete(message.id);
  };
  await new Promise((resolve) => (socket.onopen = resolve));
  const call = (method, params = {}) =>
    new Promise((resolve) => {
      const id = ++requestId;
      pending.set(id, resolve);
      socket.send(JSON.stringify({ id, method, params }));
    });

  for (const width of [320, 375]) {
    await call("Emulation.setDeviceMetricsOverride", {
      width,
      height: 812,
      deviceScaleFactor: 1,
      mobile: true,
    });
    await call("Page.reload");
    let layout;
    for (let attempt = 0; attempt < 50 && !layout; attempt++) {
      const result = await call("Runtime.evaluate", {
        expression: `(() => {
        const root = document.documentElement;
        const article = document.querySelector("article");
        if (!article) return null;
        const bounds = article.getBoundingClientRect();
        const clipped = [...article.querySelectorAll("*")]
          .filter((element) => {
            const rect = element.getBoundingClientRect();
            return !element.closest("pre") && rect.width > 0 && (rect.left < bounds.left - 1 || rect.right > bounds.right + 1);
          })
          .map((element) => element.tagName + "." + element.className)
          .slice(0, 5);
        return { rootFits: root.scrollWidth <= root.clientWidth, clipped };
      })()`,
        returnByValue: true,
      });
      layout = result.result.result.value;
      if (!layout) await new Promise((resolve) => setTimeout(resolve, 100));
    }
    if (!layout) throw new Error(`${width}px layout did not load`);
    if (!layout.rootFits || layout.clipped.length) {
      throw new Error(`${width}px layout overflows: ${layout.clipped.join(", ")}`);
    }
  }
  socket.close();
  console.log("Mobile layout fits at 320px and 375px");
} finally {
  browser.kill();
  server.kill();
  await Promise.all(
    [browser, server].map(
      (process) => new Promise((resolve) => process.once("exit", resolve)),
    ),
  );
  await rm(profile, { recursive: true, force: true, maxRetries: 5, retryDelay: 100 });
}
