import { copyFile, mkdir, readFile, writeFile } from "node:fs/promises";

const output = new URL("../../site/api/reference/index.html", import.meta.url);
const basePath = "/twitter-backend-java/api/reference";
const canonicalUrl = `https://subho.xyz${basePath}/`;
const description = "Interactive OpenAPI reference for the Moo Twitter Backend.";
const routes = ["authentication", "hashtags", "models", "posts", "search", "users"];
let html = await readFile(output, "utf8");
const scriptStart = html.indexOf('<script type="module">');

if (scriptStart < 0) throw new Error("Redoc module script was not found");

const document = html.slice(0, scriptStart).replaceAll('href="/', `href="${basePath}/`);
html = `${document}${html.slice(scriptStart)}`.replace(
  "</title>",
  `</title>\n  <meta name="description" content="${description}">\n  <meta property="og:title" content="Moo Twitter Backend API">\n  <meta property="og:description" content="${description}">\n  <link rel="canonical" href="${canonicalUrl}">`,
);

if (html.includes('href="/authentication"') || !html.includes(`href="${basePath}/authentication"`)) {
  throw new Error("Redoc routes were not rebased");
}
if (!html.includes(`rel="canonical" href="${canonicalUrl}"`)) {
  throw new Error("API reference metadata was not added");
}

await writeFile(output, html);

for (const route of routes) {
  const directory = new URL(`./${route}/`, output);
  await mkdir(directory, { recursive: true });
  await copyFile(output, new URL("index.html", directory));
}
