// https://nuxt.com/docs/api/configuration/nuxt-config
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
  modules: ['@element-plus/nuxt'],
  elementPlus: {
    icon: 'ElIcon',
    importStyle: 'scss',
  },
  vite: {
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "~/assets/scss/element/index.scss" as element;`,
        },
      },
    },
  },
})
