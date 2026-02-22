import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@shared": path.resolve(__dirname, "../shared"),
    },
  },
  server: {
    port: 5173,
    host: '0.0.0.0', // Allow external connections
    fs: {
      allow: [path.resolve(__dirname, "..")],
    },
    hmr: {
      host: 'localhost', // Will be updated with actual IP for mobile
    }
  }
});
