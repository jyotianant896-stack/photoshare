import { Navigate, Route, Routes } from 'react-router-dom'
import ProtectedRoute from './components/ProtectedRoute.jsx'
import Login from './pages/Login.jsx'
import Register from './pages/Register.jsx'
import EventsPage from './pages/EventsPage.jsx'
import EventPage from './pages/EventPage.jsx'
import PublicGallery from './pages/PublicGallery.jsx'
import NotFound from './pages/NotFound.jsx'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/events" replace />} />
      <Route path="/login" element={<Login />} />
      <Route path="/register" element={<Register />} />

      <Route element={<ProtectedRoute />}>
        <Route path="/events" element={<EventsPage />} />
        <Route path="/events/:eventId" element={<EventPage />} />
      </Route>

      {/* The customer route: no account, no token from storage. */}
      <Route path="/g/:slug" element={<PublicGallery />} />

      <Route path="*" element={<NotFound />} />
    </Routes>
  )
}
