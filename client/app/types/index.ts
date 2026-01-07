// 通用响应结构
export interface Result<T> {
  code: number
  message: string
  data: T
}

// 用户相关接口
export interface User {
  id: string
  username: string
  email?: string
  avatar?: string
  role?: string
  createTime?: string
  updateTime?: string
}

export interface LoginResponse {
  token: string
  user: User
}

export interface LoginRequest {
  username: string
  password?: string
}

export interface RegisterRequest {
  username: string
  password?: string
  email?: string
}

export interface UserUpdateDTO {
  username?: string
  email?: string
}

export interface ChangePasswordDTO {
  oldPassword?: string
  newPassword: string
  confirmPassword: string
}

// 文档相关接口
export interface Document {
  id: string
  userId: string
  documentName: string
  originalFilename: string
  filePath: string
  fileType: string
  fileSize: number
  contentType: string
  status: number
  totalChunks: number
  totalPages: number
  embeddingModel: string
  createTime: string
  uploadedAt: string
}

// 对话相关接口
export interface ChatSession {
  id: string
  userId: string
  title: string
  createTime: string
  updateTime: string
}

export interface ChatMessage {
  id: string
  conversationId: string
  role: 'user' | 'assistant' | 'system'
  messageType: 'USER' | 'ASSISTANT' | 'SYSTEM'
  content: string
  createTime: string
}

export interface ChatHistoryDTO {
  id: string
  title: string
  lastMessage?: string
  updateTime: string
}

export interface UploadResponse {
  code: number
  data: Document
  message?: string
  timestamp: number
}

export interface UploadProgress {
  percentage: number
  loaded: number
  total?: number
  speed?: number
  estimatedTime?: number
}