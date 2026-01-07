import { useDocumentApi } from "~/composables/api/useDocumentApi";
import { useStorage } from "@vueuse/core";
import { type Document } from "~/types";

export const useDocumentStore = defineStore("document", () => {
  const documents = ref<Document[]>([]);
  const currentDocument = ref<Document | null>(null);
  const previewContent = ref<string | undefined>(undefined);
  const isUploading = ref(false);
  const uploadProgress = ref(0);

  const documentApi = useDocumentApi();
  const userStore = useUserStore();

  const guestDocuments = useStorage<Document[]>("guest-documents", []);

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

  const deleteDocument = async (id: string) => {
    if (userStore.isAuthenticated) {
      try {
        const res = await documentApi.deleteDocument(id);
        if (res.code === 200) {
          await fetchDocuments();
          if (currentDocument.value?.id === id) {
            currentDocument.value = null;
            previewContent.value = undefined;
          }
        }
      } catch (error) {
        console.error("Delete failed", error);
      }
    } else {
      // Guest delete
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

  const selectDocument = async (doc: Document) => {
    currentDocument.value = doc;
    if (userStore.isAuthenticated) {
      try {
        previewContent.value = documentApi.getPreviewUrl(doc.id);
      } catch (e) {
        console.error(e);
      }
    }
  };

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
