import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const TOKEN_KEY = 'photoshare.token'

const api = axios.create({ baseURL })

// Attach the staff token to every call. Gallery tokens are passed explicitly
// instead, because they belong to a customer session, not to a signed in user.
api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const status = error.response?.status
    const onPublicPage = window.location.pathname.startsWith('/g/')
    if (status === 401 && !onPublicPage) {
      localStorage.removeItem(TOKEN_KEY)
      if (!window.location.pathname.startsWith('/login')) {
        window.location.assign('/login')
      }
    }
    return Promise.reject(error)
  },
)

/** Turns any axios failure into a sentence worth showing a person. */
export function messageFrom(error, fallback = 'Something went wrong. Try again.') {
  const data = error?.response?.data
  if (data?.fieldErrors) {
    const first = Object.values(data.fieldErrors)[0]
    if (first) return first
  }
  return data?.message || error?.message || fallback
}

export default api
