import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    port: 5173,
  },
  // sockjs-client assumes a Node-style `global` (auto-shimmed by Webpack/CRA, but not by Vite/esbuild),
  // and throws "global is not defined" at import time in a real browser without this.
  define: {
    global: 'globalThis',
  },
})
