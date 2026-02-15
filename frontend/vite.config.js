import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    host: '0.0.0.0', // Allow external connections
    hmr: {
      host: 'localhost', // Will be updated with actual IP for mobile
    }
  }
});
