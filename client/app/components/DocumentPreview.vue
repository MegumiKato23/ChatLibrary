<template>
  <div class="h-full w-full bg-slate-50 flex flex-col relative overflow-hidden">
    <div
      v-if="!documentStore.currentDocument"
      class="flex-1 flex flex-col items-center justify-center text-slate-400 select-none"
    >
      <div
        class="w-24 h-24 bg-white rounded-3xl shadow-sm flex items-center justify-center mb-6"
      >
        <el-icon :size="48" class="text-slate-200"><Document /></el-icon>
      </div>
      <h3 class="text-lg font-medium text-slate-600 mb-2">暂无预览文档</h3>
      <p class="text-sm max-w-xs text-center text-slate-400">
        请从左侧文档列表中选择一个文件，支持 PDF、Word、TXT 等格式在线预览。
      </p>
    </div>

    <!-- Preview Content -->
    <div v-else class="h-full flex flex-col">
      <div
        class="h-14 px-4 border-b border-gray-200 flex justify-between items-center bg-white/80 backdrop-blur-sm z-10"
      >
        <div class="flex items-center gap-3 overflow-hidden">
          <div
            class="w-8 h-8 rounded-lg bg-slate-100 flex items-center justify-center flex-shrink-0 text-slate-500"
          >
            <el-icon><Document /></el-icon>
          </div>
          <div class="flex flex-col overflow-hidden">
            <h3 class="font-medium text-slate-700 truncate text-sm">
              {{ documentStore.currentDocument.originalFilename }}
            </h3>
            <span class="text-[10px] text-slate-400">{{
              formatSize(documentStore.currentDocument.fileSize)
            }}</span>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <el-tooltip content="下载原文件" placement="bottom">
            <el-button
              circle
              size="small"
              :icon="Download"
              @click="handleDownload"
            />
          </el-tooltip>
          <el-button
            circle
            size="small"
            :icon="Close"
            @click="documentStore.currentDocument = null"
          />
        </div>
      </div>

      <!-- Content Area -->
      <div
        class="flex-1 overflow-auto bg-slate-100/50 p-6 relative flex justify-center"
        ref="previewContainer"
      >
        <!-- Loading State -->
        <div
          v-if="loading"
          class="absolute inset-0 flex items-center justify-center bg-white/50 z-20 backdrop-blur-sm"
        >
          <div class="flex flex-col items-center gap-3">
            <el-icon class="is-loading text-primary" :size="32"
              ><Loading
            /></el-icon>
            <span class="text-sm text-slate-500">正在加载文档内容...</span>
          </div>
        </div>

        <!-- PDF Viewer (Iframe fallback) -->
        <iframe
          v-if="isPdf"
          :src="documentStore.previewContent"
          class="w-full h-full rounded-lg shadow-lg bg-white border border-slate-200"
        ></iframe>

        <!-- Image -->
        <img
          v-else-if="isImage"
          :src="documentStore.previewContent"
          class="max-w-full max-h-full object-contain rounded-lg shadow-lg bg-white"
        />

        <!-- DOCX Container -->
        <div
          v-else-if="isDocx"
          class="w-full max-w-4xl bg-white shadow-lg rounded-lg min-h-full p-8 md:p-12 docx-wrapper"
        >
        </div>

        <!-- HTML Content (DOC/TXT via Backend) -->
        <div
          v-else-if="htmlContent"
          class="w-full max-w-4xl bg-white shadow-lg rounded-lg min-h-full p-8 md:p-12 overflow-auto"
        >
          <!-- Text file as pre -->
          <pre
            v-if="isTxt"
            class="whitespace-pre-wrap font-mono text-sm text-slate-700 leading-relaxed"
            >{{ htmlContent }}</pre
          >
          <!-- HTML content (from Tika for doc) -->
          <div
            v-else
            class="prose max-w-none text-slate-700"
            v-html="htmlContent"
          ></div>
        </div>

        <!-- Fallback Text -->
        <div
          v-else
          class="w-full max-w-4xl bg-white shadow-lg rounded-lg min-h-full p-8 md:p-12 whitespace-pre-wrap font-mono text-sm text-slate-700 leading-relaxed border border-slate-200"
        >
          {{ textContent }}
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useDocumentApi } from "~/composables/api/useDocumentApi";
import { Document, Close, Download, Loading } from "@element-plus/icons-vue";

import { renderAsync } from "docx-preview";

const api = useApi();
const documentStore = useDocumentStore();
const documentApi = useDocumentApi();
const docxContainer = ref<HTMLElement | null>(null);
const textContent = ref("");
const htmlContent = ref("");
const loading = ref(false);
const error = ref("");

// --- 文件类型判断 ---

const currentDocType = computed(
  () => documentStore.currentDocument?.fileType?.toLowerCase() ?? ""
);
const currentDocName = computed(
  () => documentStore.currentDocument?.documentName?.toLowerCase() ?? ""
);

const isPdf = computed(
  () => currentDocType.value === "pdf" || currentDocName.value.endsWith(".pdf")
);

const isImage = computed(() =>
  ["jpg", "jpeg", "png", "gif", "webp"].includes(currentDocType.value)
);

const isDocx = computed(
  () =>
    currentDocType.value === "docx" || currentDocName.value.endsWith(".docx")
);

const isDoc = computed(
  () => currentDocType.value === "doc" || currentDocName.value.endsWith(".doc")
);

const isTxt = computed(
  () =>
    ["txt", "md"].includes(currentDocType.value) ||
    currentDocName.value.endsWith(".txt") ||
    currentDocName.value.endsWith(".md")
);

// --- 文件大小格式化 ---

const formatSize = (bytes: number) => {
  if (bytes === 0 || !bytes) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
};

const handleDownload = () => {
  if (documentStore.previewContent) {
    window.open(documentStore.previewContent, "_blank");
  }
};

// --- 文件预览逻辑 ---

const resetContent = () => {
  textContent.value = "";
  htmlContent.value = "";
  error.value = "";
  if (docxContainer.value) {
    docxContainer.value.innerHTML = "";
  }
};

// --- 处理 Docx 文件预览 ---
const renderDocx = async (url: string) => {
  await nextTick();
  if (!docxContainer.value) return;

  try {
    const res = await api.get(url).then((res) => res.data) as Response;
    if (!res.ok) throw new Error(`加载文档内容失败: ${res.statusText}`);

    const blob = await res.blob();
    const container = docxContainer.value as HTMLElement;
    await renderAsync(blob, container, container, {
      className: "docx-viewer",
      inWrapper: false,
      ignoreWidth: false,
    });
  } catch (e) {
    throw e;
  }
};

// --- 提取文档内容 ---

const fetchExtractedContent = async (id: string) => {
  const res = await documentApi.getPreviewContent(id);
  if (res.code === 200 && res.data) {
    htmlContent.value = res.data;
  } else {
    throw new Error(res.message || "加载文档预览内容失败");
  }
};

// --- 回退加载内容 ---

const fetchFallbackContent = async (url: string) => {
  const res = await api.get(url).then((res) => res.data) as Response;
  if (!res.ok) throw new Error(`加载文档内容失败: ${res.statusText}`);
  textContent.value = await res.text();
};

const loadPreview = async () => {
  const doc = documentStore.currentDocument;
  if (!doc) return;

  loading.value = true;
  resetContent();

  try {
    if (isDocx.value) {
      await renderDocx(documentStore.previewContent!);
    } else if (isDoc.value || isTxt.value) {
      await fetchExtractedContent(doc.id);
    } else if (!isPdf.value && !isImage.value) {
      await fetchFallbackContent(documentStore.previewContent!);
    }
  } catch (e: any) {
    console.error("Preview failed:", e);
    error.value = "无法预览此文件内容，请尝试下载后查看。";
    textContent.value = error.value;
  } finally {
    loading.value = false;
  }
};

watch(
  () => documentStore.currentDocument?.id,
  (newId) => {
    if (newId) {
      loadPreview();
    }
  },
  { immediate: true }
);
</script>

<style scoped>
/* Docx Viewer Styles */
:deep(.docx-wrapper) {
  background: transparent !important;
  padding: 0 !important;
}
:deep(.docx-viewer) {
  padding: 0 !important;
  box-shadow: none !important;
  margin-bottom: 0 !important;
}
</style>
