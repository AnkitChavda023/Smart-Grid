import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { Navigate, Route, BrowserRouter, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { ThemeProvider } from './theme/ThemeContext'
import { NotificationsProvider } from './ws/NotificationsContext'
import { AppShell } from './components/layout/AppShell'
import { ProtectedRoute } from './components/layout/ProtectedRoute'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { OverviewPage } from './pages/OverviewPage'
import { OrdersPage } from './pages/OrdersPage'
import { VendorsPage } from './pages/VendorsPage'
import { DisruptionsPage } from './pages/DisruptionsPage'
import { ReroutesPage } from './pages/ReroutesPage'
import { AnalyticsPage } from './pages/AnalyticsPage'
import { AgentTracesPage } from './pages/AgentTracesPage'
import { ShipmentsPage } from './pages/ShipmentsPage'
import { SlaPage } from './pages/SlaPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  return (
    <ThemeProvider>
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <NotificationsProvider>
            <BrowserRouter>
              <Routes>
                <Route path="/login" element={<LoginPage />} />
                <Route path="/register" element={<RegisterPage />} />

                {/* Base protected routes for all authenticated roles */}
                <Route element={<ProtectedRoute />}>
                  <Route element={<AppShell />}>
                    <Route path="/" element={<OverviewPage />} />
                    <Route path="/orders" element={<OrdersPage />} />
                    <Route path="/shipments" element={<ShipmentsPage />} />
                    <Route path="/sla" element={<SlaPage />} />

                    {/* Admin & Planner only routes */}
                    <Route element={<ProtectedRoute allowedRoles={['ADMIN', 'PLANNER']} />}>
                      <Route path="/vendors" element={<VendorsPage />} />
                      <Route path="/disruptions" element={<DisruptionsPage />} />
                      <Route path="/reroutes" element={<ReroutesPage />} />
                      <Route path="/analytics" element={<AnalyticsPage />} />
                      <Route path="/agent-traces" element={<AgentTracesPage />} />
                    </Route>
                  </Route>
                </Route>

                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </BrowserRouter>
          </NotificationsProvider>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}
