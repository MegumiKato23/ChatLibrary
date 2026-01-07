import { type Result } from "~/types";

/**
 * HTTP 请求封装组合式函数
 * 提供统一的 API 请求处理、错误拦截和类型支持
 */
export const useApi = () => {
  const config = useRuntimeConfig();

  // API 基础路径
  const baseURL = config.public.apiBase as string;

  /**
   * 基础请求方法
   * @param url 请求地址
   * @param options 请求配置 (支持 retry 和 retryDelay)
   */
  const fetcher = async <T>(
    url: string,
    options: any = {}
  ): Promise<Result<T>> => {
    const { retry = 0, retryDelay = 1000, ...fetchOptions } = options;
    let lastError: any;

    for (let i = 0; i <= retry; i++) {
      try {
        const headers: any = {
          ...fetchOptions.headers,
        };

        if (process.client && fetchOptions.body instanceof FormData) {
          delete headers["Content-Type"];
        }

        const response = await $fetch<Result<T>>(url, {
          baseURL,
          ...fetchOptions,
          headers,
          credentials: "include", // 确保携带 Cookie
          onResponseError({ response }) {
            // 处理 HTTP 错误状态码
            if (response.status === 401) {
              // 401 Unauthorized - 清除本地状态并跳转登录页
              if (process.client) {
                const userStore = useUserStore();
                userStore.logout();
              }
            }
          },
        });

        return response;
      } catch (error: any) {
        lastError = error;
        console.error(
          `API Request Error [${url}] (Attempt ${i + 1}/${retry + 1}):`,
          error
        );

        // 如果是最后一次尝试，或者遇到特定不需要重试的错误（如4xx客户端错误），则抛出
        if (
          i === retry ||
          (error.response &&
            error.response.status >= 400 &&
            error.response.status < 500 &&
            error.response.status !== 408 &&
            error.response.status !== 429)
        ) {
          throw error;
        }

        // 等待后重试
        await new Promise((resolve) => setTimeout(resolve, retryDelay));
      }
    }

    throw lastError;
  };

  /**
   * GET 请求
   * @param url 请求地址
   * @param params 查询参数
   */
  const get = async <T>(url: string, params?: any): Promise<Result<T>> => {
    return fetcher<T>(url, { method: "GET", params });
  };

  /**
   * POST 请求
   * @param url 请求地址
   * @param body 请求体
   * @param options 其他配置
   */
  const post = async <T>(
    url: string,
    body?: any,
    options?: any
  ): Promise<Result<T>> => {
    return fetcher<T>(url, { method: "POST", body, ...options });
  };

  /**
   * PUT 请求
   * @param url 请求地址
   * @param body 请求体
   */
  const put = async <T>(url: string, body?: any): Promise<Result<T>> => {
    return fetcher<T>(url, { method: "PUT", body });
  };

  /**
   * DELETE 请求
   * @param url 请求地址
   */
  const del = async <T>(url: string): Promise<Result<T>> => {
    return fetcher<T>(url, { method: "DELETE" });
  };

  return { get, post, put, del };
};
