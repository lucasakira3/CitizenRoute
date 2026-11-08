import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// O React chama /api/... e o Vite repassa para a API Java (HttpServer do JDK) na porta 8080.
const api = {
  "/api": {
    target: "http://127.0.0.1:8080",
    changeOrigin: true,
  },
};

export default defineConfig({
  plugins: [react()],
  server: { port: 5173, proxy: api },
  preview: { port: 4173, proxy: api },
});
