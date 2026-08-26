import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import * as authApi from '../api/auth'
import { useAuth } from '../auth/AuthContext'
import { ThemeToggle } from '../theme/ThemeToggle'
import { Card } from '../components/ui/Card'
import type { Role } from '../types'
import type { AxiosError } from 'axios'

const ROLE_OPTIONS: { value: Role; label: string; hint: string }[] = [
  { value: 'SUPPLIER', label: 'Supplier', hint: 'View everything, place and cancel orders' },
  { value: 'PLANNER', label: 'Planner', hint: 'Also: submit contract drafts, approve escalated reroutes' },
  { value: 'ADMIN', label: 'Admin', hint: 'Same access as Planner in this system' },
]

export function RegisterPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState<Role>('SUPPLIER')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await authApi.register(username, password, role)
      // Registration doesn't return tokens — sign the new account in immediately
      // so a first-time user lands straight in the app instead of a second manual step.
      await login(username, password)
      navigate('/', { replace: true })
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string }>
      if (axiosError.response?.status === 409) {
        setError('That username is already taken.')
      } else if (axiosError.response?.status === 400) {
        setError('Password must be at least 8 characters.')
      } else {
        setError('Registration failed. Please try again.')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center bg-bg px-4">
      <div className="absolute right-6 top-6">
        <ThemeToggle />
      </div>
      <Card className="w-full max-w-sm">
        <h1 className="text-xl font-semibold text-text">Create your account</h1>
        <p className="mt-1 text-sm text-text-muted">No approval needed, pick a role and you're in.</p>

        <form onSubmit={handleSubmit} className="mt-6 flex flex-col gap-4">
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-text">Username</span>
            <input
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              required
              autoFocus
              autoComplete="username"
              className="rounded-lg border border-border bg-bg px-3 py-2 text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
            />
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-text">Password</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              minLength={8}
              autoComplete="new-password"
              className="rounded-lg border border-border bg-bg px-3 py-2 text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
            />
            <span className="text-xs text-text-muted">At least 8 characters.</span>
          </label>
          <label className="flex flex-col gap-1.5 text-sm">
            <span className="font-medium text-text">Role</span>
            <select
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              className="rounded-lg border border-border bg-bg px-3 py-2 text-text outline-none focus:border-accent focus:ring-1 focus:ring-accent"
            >
              {ROLE_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
            <span className="text-xs text-text-muted">
              {ROLE_OPTIONS.find((option) => option.value === role)?.hint}
            </span>
          </label>

          {error && <p className="text-sm text-danger">{error}</p>}

          <button
            type="submit"
            disabled={submitting}
            className="mt-2 rounded-lg bg-accent px-4 py-2 text-sm font-medium text-accent-fg shadow-sm shadow-accent/20 transition-all hover:opacity-90 active:scale-[0.98] disabled:opacity-60"
          >
            {submitting ? 'Creating account…' : 'Create account'}
          </button>
        </form>

        <p className="mt-4 text-center text-sm text-text-muted">
          Already have an account?{' '}
          <Link to="/login" className="font-medium text-accent hover:underline">
            Sign in
          </Link>
        </p>
      </Card>
    </div>
  )
}
