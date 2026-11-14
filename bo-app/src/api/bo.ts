import { http } from './http'
import type {
  BoChangeRequestDetail,
  BoChangeRequestSummary,
  BoUserProfile,
  ChangeRequestStatus,
  PageResponse,
  TokenResponse,
} from './types'

export async function login(email: string, password: string) {
  return (await http.post<TokenResponse>('/auth/bo/login', { email, password })).data
}

export async function getProfile() {
  return (await http.get<BoUserProfile>('/bo/me')).data
}

/** Newest first; all statuses when status is null. */
export async function listRequests(status: ChangeRequestStatus | null, page: number, size = 20) {
  const params = { page, size, ...(status ? { status } : {}) }
  return (await http.get<PageResponse<BoChangeRequestSummary>>('/bo/change-requests', { params }))
    .data
}

export async function getRequest(id: number) {
  return (await http.get<BoChangeRequestDetail>(`/bo/change-requests/${id}`)).data
}

export async function approve(id: number, comment: string) {
  return (
    await http.post<BoChangeRequestDetail>(`/bo/change-requests/${id}/approve`, {
      comment: comment.trim() || null,
    })
  ).data
}

export async function reject(id: number, reason: string) {
  return (await http.post<BoChangeRequestDetail>(`/bo/change-requests/${id}/reject`, { reason }))
    .data
}
