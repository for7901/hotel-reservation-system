export interface LoginRequest {
  phone: string
  password: string
}

export interface RegisterRequest {
  phone: string
  password: string
  nickname?: string
}

export interface LoginResponse {
  token: string
  userId: number
  phone: string
  nickname: string
  role: string
}

export interface UserProfile {
  id: number
  phone: string
  nickname: string
  avatar: string | null
  role: string
  status: number
  createdAt: string
}
