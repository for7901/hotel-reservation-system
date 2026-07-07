export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export interface HealthData {
  app: string
  profile: string
  status: string
  timestamp: string
}
