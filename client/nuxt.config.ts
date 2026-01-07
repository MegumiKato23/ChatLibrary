// https://nuxt.com/docs/api/configuration/nuxt-config
import { defineNuxtConfig } from 'nuxt/config'

export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  devtools: { enabled: true },
  css: ['~/assets/css/tailwind.css'],
  postcss: {
    plugins: {
      '@tailwindcss/postcss': {},
      autoprefixer: {},
    },
  },
  modules: ['@element-plus/nuxt', '@pinia/nuxt', 'nuxt-auth-utils'],
  elementPlus: {
    icon: 'ElIcon',
    importStyle: 'scss',
  },
  runtimeConfig: {
    public: {
      apiBase: 'http://localhost:8080'
    }
  },
  vite: {
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "~/assets/scss/main.scss" as element;`,
        },
      },
    },
  },
  app: {
    pageTransition: { name: 'page', mode: 'out-in' }
  }
})