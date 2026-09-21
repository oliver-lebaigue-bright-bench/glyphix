import { createRoot } from "react-dom/client";
import App from "./App";
import "./index.css";

createRoot(document.getElementById("root")!).render(<App />);

// Load this after React has mounted. The banner script injects its markup into
// the document; loading it before the root is rendered lets React remove it
// during the initial reconciliation.
requestAnimationFrame(() => {
  if (document.querySelector('script[data-keep-android-open]')) return;

  const script = document.createElement("script");
  script.src = "https://keepandroidopen.org/banner.js";
  script.dataset.keepAndroidOpen = "true";
  script.async = false;
  document.body.appendChild(script);
});
