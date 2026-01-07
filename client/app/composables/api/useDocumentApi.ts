import {
  type Document,
  type UploadProgress,
  type UploadResponse,
} from "~/types";

/**
 * 文档管理 API 接口
 * 对应后端 DocumentController
 */
export const useDocumentApi = () => {
  const api = useApi();
  const config = useRuntimeConfig();

  /**
   * 上传文件（支持进度监听）
   * POST /document/upload/{userId}
   * @param userId 用户ID
   * @param file 文件对象
   * @param onProgress 进度回调 (0-100)
   */
  const upload = (
    userId: string,
    file: File,
    onProgress?: (progress: UploadProgress) => void
  ): Promise<UploadResponse> => {
    const formData = new FormData();
    formData.append("file", file);

    // 创建 XMLHttpRequest 实例
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      const baseURL = config.public.apiBase as string;
      xhr.withCredentials = true; // 允许携带 Cookie

      xhr.upload.addEventListener("progress", (event) => {
        if (onProgress && event.lengthComputable) {
          const process: UploadProgress = {
            percentage: Math.round((event.loaded / event.total) * 100),
            loaded: event.loaded,
            total: event.total,
            speed: 0,
            estimatedTime: 0,
          };
          onProgress(process);
        }
      });

      xhr.addEventListener("load", () => {
        if (xhr.status >= 200 && xhr.status < 300) {
          try {
            const response = JSON.parse(xhr.responseText);
            resolve(response);
          } catch (e) {
            reject(new Error("Invalid JSON response"));
          }
        } else {
          reject(new Error(`Upload failed with status ${xhr.status}`));
        }
      });

      xhr.addEventListener("error", () => reject(new Error("Network error")));

      xhr.addEventListener("timeout", () =>
        reject(new Error("Upload timeout"))
      );

      xhr.open("POST", `${baseURL}/document/upload/${userId}`);
      xhr.timeout = 120000;
      xhr.send(formData);
    });
  };

  /**
   * 获取文档列表
   * GET /document/list?userId={userId}
   * @param userId 用户ID
   */
  const list = (userId?: string) => {
    return api.get<Document[]>("/document/list", { userId });
  };

  /**
   * 获取所有文档
   * GET /document/list/all
   */
  const listAll = () => {
    return api.get<Document[]>("/document/list/all");
  };

  /**
   * 删除文档
   * DELETE /document/{id}
   * @param id 文档ID
   */
  const deleteDocument = (id: string) => {
    return api.del<string>(`/document/${id}`);
  };

  /**
   * 获取预览地址
   * GET /document/preview/{id}
   * @param id 文档ID
   */
  const getPreviewUrl = (id: string) => {
    const baseURL = config.public.apiBase as string;
    const url = `${baseURL}/document/preview/${id}`;
    return url;
  };

  const getPreviewContent = (id: string) => {
    return api.get<string>(`/document/preview/content/${id}`);
  };

  return {
    upload,
    list,
    listAll,
    deleteDocument,
    getPreviewUrl,
    getPreviewContent,
  };
};
