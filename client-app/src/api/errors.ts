import axios from 'axios'
import type { ProblemDetail } from './types'

export interface ApiError {
  status: number | null
  /** Machine-readable reason from the backend, e.g. "EMAIL_NOT_VERIFIED" */
  code: string | null
  message: string
  fieldErrors: Record<string, string>
}

/** Turns any thrown error into a message plus per-field errors from the backend's ProblemDetail. */
export function toApiError(error: unknown): ApiError {
  if (!axios.isAxiosError(error)) {
    return {
      status: null,
      code: null,
      message: 'Something went wrong. Please try again.',
      fieldErrors: {},
    }
  }
  if (!error.response) {
    return {
      status: null,
      code: null,
      message: 'Cannot reach the server. Please try again later.',
      fieldErrors: {},
    }
  }

  const status = error.response.status
  const problem = error.response.data as Partial<ProblemDetail> | undefined
  const code = problem?.code ?? null
  const fieldErrors = problem?.errors ?? {}
  if (Object.keys(fieldErrors).length > 0) {
    return { status, code, message: 'Please correct the highlighted fields.', fieldErrors }
  }
  return { status, code, message: problem?.detail ?? fallbackMessage(status), fieldErrors }
}

function fallbackMessage(status: number): string {
  if (status === 401) return 'Your session has expired. Please log in again.'
  if (status === 403) return 'You do not have access to this page.'
  return 'Something went wrong. Please try again.'
}
