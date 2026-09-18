import { createContext, useContext, useEffect, useMemo, useState } from 'react'
import api, { TOKEN_KEY } from '../api/client.js'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [loading, setLoading] = useState(true)

  // On a reload the token is still in storage but the user object is not,
  // so ask the API who this token belongs to before rendering the app.
  useEffect(() => {
    const token = localStorage.getItem(TOKEN_KEY)
    if (!token) {
      setLoading(false)
      return
    }
    api
      .get('/api/auth/me')
      .then((response) => setUser(response.data))
      .catch(() => localStorage.removeItem(TOKEN_KEY))
      .finally(() => setLoading(false))
  }, [])

  const value = useMemo(
    () => ({
      user,
      loading,
      isAdmin: user?.role === 'ADMIN',
      async login(email, password) {
        const { data } = await api.post('/api/auth/login', { email, password })
        localStorage.setItem(TOKEN_KEY, data.token)
        setUser(data.user)
        return data.user
      },
      async register(name, email, password) {
        const { data } = await api.post('/api/auth/register', { name, email, password })
        localStorage.setItem(TOKEN_KEY, data.token)
        setUser(data.user)
        return data.user
      },
      logout() {
        localStorage.removeItem(TOKEN_KEY)
        setUser(null)
      },
    }),
    [user, loading],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) {
    throw new Error('useAuth must be used inside AuthProvider')
  }
  return context
}
