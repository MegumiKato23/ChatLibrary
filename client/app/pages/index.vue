<template>
  <div class="flex h-full w-full">
    <!-- Left Sidebar: 250px -->
    <div class="w-[250px] flex-shrink-0 h-full">
      <SidebarLeft />
    </div>

    <!-- Middle: Document Preview (Flexible) -->
    <div class="flex-1 h-full border-r border-gray-200 bg-gray-100 overflow-hidden relative">
      <DocumentPreview />
    </div>

    <!-- Right Sidebar: Chat (400px) -->
    <div class="w-[400px] flex-shrink-0 h-full">
      <SidebarRight />
    </div>
  </div>
</template>

<script setup lang="ts">
definePageMeta({
  layout: 'default'
})
</script>
