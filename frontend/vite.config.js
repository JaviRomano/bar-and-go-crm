import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// /api se reenvía al backend: frontend y API comparten origen (útil tras un túnel o proxy inverso).
// Para usarlo, construir con VITE_API_BASE_URL=/api.
const apiProxy = {
  '/api': {
    target: process.env.API_PROXY_TARGET || 'http://localhost:8080',
    changeOrigin: true,
    // Vista desde el navegador la petición es del mismo origen; sin quitar Origin,
    // el backend la trataría como cross-origin y la rechazaría por CORS
    configure: (proxy) => {
      proxy.on('proxyReq', (proxyReq) => proxyReq.removeHeader('origin'))
    }
  }
}

// Hosts externos admitidos (p. ej. el dominio de un túnel), separados por comas.
// Vite rechaza por defecto las peticiones con un Host desconocido.
const allowedHosts = process.env.VITE_ALLOWED_HOSTS
  ? process.env.VITE_ALLOWED_HOSTS.split(',').map((host) => host.trim())
  : []

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: apiProxy,
    allowedHosts
  },
  preview: {
    proxy: apiProxy,
    allowedHosts
  }
})
