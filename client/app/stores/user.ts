import { useUserApi } from "~/composables/api/useUserApi";
import { type LoginRequest, type RegisterRequest } from "~/types";

export const useUserStore = defineStore("user", () => {
  const user = ref<any>(null);
  const userApi = useUserApi();
  const isLoading = ref(false);
  const isLoginModalVisible = ref(false);

  // 初始化时从 localStorage 恢复用户信息
  if (import.meta.client) {
    const storedUser = localStorage.getItem("user-info");
    if (storedUser) {
      try {
        user.value = JSON.parse(storedUser);
      } catch (e) {
        localStorage.removeItem("user-info");
      }
    }
  }

  const isAuthenticated = computed(() => !!user.value);

  const showLoginModal = () => {
    isLoginModalVisible.value = true;
  };

  const hideLoginModal = () => {
    isLoginModalVisible.value = false;
  };

  const login = async (credentials: LoginRequest) => {
    isLoading.value = true;
    try {
      const res = await userApi.login(credentials);
      if (res.code === 200) {
        user.value = res.data.user;
        if (import.meta.client) {
          localStorage.setItem("user-info", JSON.stringify(user.value));
        }
        return true;
      }
      throw new Error(res.message || "登录失败");
    } catch (error: any) {
      throw new Error("登录失败", error);
    } finally {
      isLoading.value = false;
    }
  };

  const register = async (userData: RegisterRequest) => {
    isLoading.value = true;
    try {
      const res = await userApi.register(userData);
      if (res.code === 200) {
        user.value = res.data.user;
        if (import.meta.client) {
          localStorage.setItem("user-info", JSON.stringify(user.value));
        }
        return true;
      }
      throw new Error(res.message || "注册失败");
    } catch (error: any) {
      throw new Error("注册失败", error);
    } finally {
      isLoading.value = false;
    }
  };

  const logout = async () => {
    user.value = null;
    if (import.meta.client) {
      localStorage.removeItem("user-info");
    }

    const chatStore = useChatStore();
    const documentStore = useDocumentStore();
    chatStore.clearState();
    documentStore.clearState();

    const router = useRouter();
    router.push("/");
  };

  const checkAuth = async () => {
    if (import.meta.client) {
      const storedUser = localStorage.getItem("user-info");
      if (storedUser) {
        try {
          user.value = JSON.parse(storedUser);
        } catch (e: any) {
          throw new Error("解析用户信息失败", e);
        }
      }
    }
  };

  return {
    user,
    isLoading,
    isAuthenticated,
    isLoginModalVisible,
    showLoginModal,
    hideLoginModal,
    login,
    register,
    logout,
    checkAuth,
  };
});
