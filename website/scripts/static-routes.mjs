import { mkdir, readFile, writeFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import path from "node:path";

const output = fileURLToPath(new URL("../dist/public/", import.meta.url));
const html = await readFile(path.join(output, "index.html"), "utf8");
// Pages cannot apply Vercel rewrites. Emit app routes for direct links/reloads.
for (const route of ["support", "download", "studio"]) {
  await mkdir(path.join(output, route), { recursive: true });
  await writeFile(path.join(output, route, "index.html"), html);
}
await writeFile(path.join(output, "404.html"), html);
