import { useUserApi } from "~/composables/api/useUserApi";
import { type LoginRequest, type RegisterRequest } from "~/types";

export const useUserStore = defineStore("user", () => {
  // --- 状态定义 ---
  const user = ref<any>(null); // 当前登录用户信息
  const userApi = useUserApi();
  const isLoading = ref(false); // 全局加载状态
  const isLoginModalVisible = ref(false); // 登录弹窗显示控制

  // --- 初始化逻辑 ---
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

  // --- 计算属性 ---
  const isAuthenticated = computed(() => !!user.value); // 是否已登录

  // --- Actions ---
  
  // 显示登录弹窗
  const showLoginModal = () => {
    isLoginModalVisible.value = true;
  };

  // 隐藏登录弹窗
  const hideLoginModal = () => {
    isLoginModalVisible.value = false;
  };

  // 用户登录
  const login = async (credentials: LoginRequest) => {
    isLoading.value = true;
    try {
      const res = await userApi.login(credentials);
      if (res.code === 200) {
        user.value = res.data.user;
        // 持久化用户信息
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

  // 用户注册
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

  // 退出登录，清理所有状态
  const logout = async () => {
    user.value = null;
    if (import.meta.client) {
      localStorage.removeItem("user-info");
    }

    // 清理其他 Store 的状态
    const chatStore = useChatStore();
    const documentStore = useDocumentStore();
    chatStore.clearState();
    documentStore.clearState();

    const router = useRouter();
    router.push("/");
  };

  // 检查认证状态（用于路由守卫等）
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
