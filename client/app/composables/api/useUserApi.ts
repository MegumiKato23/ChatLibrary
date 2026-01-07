import { type LoginRequest, type RegisterRequest, type LoginResponse, type User, type UserUpdateDTO, type ChangePasswordDTO } from '~/types'

/**
 * 用户相关 API 接口
 */
export const useUserApi = () => {
  const api = useApi()

  /**
   * 用户登录
   * @param credentials 登录凭证
   */
  const login = (credentials: LoginRequest) => {
    return api.post<LoginResponse>('/user/login', credentials)
  }

  /**
   * 用户注册
   * @param data 注册信息
   */
  const register = (data: RegisterRequest) => {
    return api.post<LoginResponse>('/user/register', data)
  }

  /**
   * 更新用户信息
   * @param data 用户信息
   */
  const update = (userId: string, data: UserUpdateDTO) => {
    return api.post<User>(`/user/update/${userId}`, data)
  }

  /**
   * 修改密码
   * @param data 密码信息
   */
  const changePassword = (userId: string, data: ChangePasswordDTO) => {
    return api.post<User>(`/user/change-password/${userId}`, data)
  }

  /**
   * 删除用户
   * @param userId 用户ID
   */
  const deleteUser = (userId: string) => {
    return api.del<void>(`/user/delete/${userId}`)
  }

  return {
    login,
    register,
    update,
    changePassword,
    deleteUser
  }
}
