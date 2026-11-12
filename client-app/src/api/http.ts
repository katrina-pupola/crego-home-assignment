import axios from 'axios'

// Relative base URL: in dev, Vite proxies /api to the backend (see vite.config.ts)
export const http = axios.create({ baseURL: '/api' })

let getToken: () => string | null = () => null
let onUnauthorized: () => void = () => {}

/** Wired up in main.ts, so this module does not depend on the store or router. */
export function configureHttp(options: {
  getToken: () => string | null
  onUnauthorized: () => void
}) {
  getToken = options.getToken
  onUnauthorized = options.onUnauthorized
}

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    // A 401 on a request that carried a token means the session expired
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      error.config?.headers?.Authorization
    ) {
      onUnauthorized()
    }
    return Promise.reject(error)
  },
)
