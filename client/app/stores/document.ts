import { useDocumentApi } from "~/composables/api/useDocumentApi";
import { useStorage } from "@vueuse/core";
import { type Document } from "~/types";

export const useDocumentStore = defineStore("document", () => {
  // --- 状态定义 ---
  const documents = ref<Document[]>([]); // 文档列表
  const currentDocument = ref<Document | null>(null); // 当前选中文档
  const previewContent = ref<string | undefined>(undefined); // 预览内容 URL 或 数据
  const isUploading = ref(false); // 上传状态
  const uploadProgress = ref(0); // 上传进度

  const documentApi = useDocumentApi();
  const userStore = useUserStore();

  const guestDocuments = useStorage<Document[]>("guest-documents", []);

  // --- Actions ---

  // 获取文档列表
  const fetchDocuments = async () => {
    if (userStore.isAuthenticated && userStore.user?.id) {
      try {
        const res = await documentApi.list(userStore.user.id);
        if (res.code === 200) {
          let list = res.data;
          if (!Array.isArray(list)) {
            console.warn(
              "[DocumentStore] Documents data is not an array:",
              list
            );
            list = [];
          }
          documents.value = list;
        }
      } catch (error) {
        console.error("Fetch documents failed", error);
      }
    } else {
      console.log("[DocumentStore] Guest mode or not authenticated");
      documents.value = guestDocuments.value;
    }
  };

  // 上传文档
  const uploadDocument = async (file: File) => {
    if (userStore.isAuthenticated && userStore.user?.id) {
      isUploading.value = true;
      uploadProgress.value = 0;
      try {
        // 模拟进度条，因为 fetch API 原生不支持上传进度回调
        const interval = setInterval(() => {
          if (uploadProgress.value < 90) {
            uploadProgress.value += 10;
          }
        }, 200);

        // 使用标准上传接口
        const res = await documentApi.upload(userStore.user.id, file);

        clearInterval(interval);
        uploadProgress.value = 100;

        if (res.code === 200) {
          await fetchDocuments();
        }
      } catch (error) {
        console.error("Upload failed", error);
        throw error;
      } finally {
        isUploading.value = false;
        setTimeout(() => {
          uploadProgress.value = 0;
        }, 1000);
      }
    } else {
      userStore.showLoginModal();
      throw new Error("Authentication required");
    }
  };

  // 删除文档
  const deleteDocument = async (id: string) => {
    if (userStore.isAuthenticated) {
      try {
        const res = await documentApi.deleteDocument(id);
        if (res.code === 200) {
          await fetchDocuments();
          // 如果删除的是当前预览文档，清空预览
          if (currentDocument.value?.id === id) {
            currentDocument.value = null;
            previewContent.value = undefined;
          }
        }
      } catch (error) {
        console.error("Delete failed", error);
      }
    } else {
      // 访客模式删除
      const index = guestDocuments.value.findIndex((d) => d.id === id);
      if (index !== -1) {
        guestDocuments.value.splice(index, 1);
        documents.value = guestDocuments.value;
        if (currentDocument.value?.id === id) {
          currentDocument.value = null;
          previewContent.value = undefined;
        }
      }
    }
  };

  // 选中文档进行预览
  const selectDocument = async (doc: Document) => {
    currentDocument.value = doc;
    if (userStore.isAuthenticated) {
      try {
        // 获取预览链接（通常是二进制流或静态资源URL）
        previewContent.value = documentApi.getPreviewUrl(doc.id);
      } catch (e) {
        console.error(e);
      }
    }
  };

  // 清空状态
  const clearState = () => {
    documents.value = [];
    currentDocument.value = null;
    previewContent.value = undefined;
    isUploading.value = false;
    uploadProgress.value = 0;
  };

  return {
    documents,
    currentDocument,
    previewContent,
    isUploading,
    uploadProgress,
    fetchDocuments,
    uploadDocument,
    deleteDocument,
    selectDocument,
    clearState,
  };
});
