
import { useState } from "react";
import {
  Eye,
  EyeOff,
  Mail,
  Lock,
  User,
  ArrowRight,
  CheckCircle2,
} from "lucide-react";
import "./Auth.css";

function Auth({ onAuthenticated }) {
  const [isLogin, setIsLogin] = useState(true);
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [message, setMessage] = useState("");

  const [form, setForm] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
    remember: false,
  });

  const handleSubmit = (e) => {
    e.preventDefault();
    setMessage("");

    if (!isLogin && form.password !== form.confirmPassword) {
      setMessage("Passwords do not match.");
      return;
    }

    if (form.password.length < 8) {
      setMessage("Password must contain at least 8 characters.");
      return;
    }

    // TODO: replace with real API call — onAuthenticated(role) from backend response.
    onAuthenticated("admin");
  };

  const updateField = (field, value) => {
    setForm((prev) => ({ ...prev, [field]: value }));
    setMessage("");
  };

  return (
    <div className="auth-page">
      <div className="auth-brand-panel">
        <div className="auth-brand">
          <div className="auth-logo">B</div>
          <span>Brightloop</span>
        </div>

        <div className="auth-brand-content">
          <span className="auth-eyebrow">SUBSCRIPTION MANAGEMENT</span>
          <h1>Manage subscriptions.<br />Grow your business.</h1>
          <p>
            One platform to manage customers, plans, billing,
            payments and your entire subscription lifecycle.
          </p>

          <div className="auth-benefits">
            <div><CheckCircle2 size={18} /> Simplified subscription management</div>
            <div><CheckCircle2 size={18} /> Centralized billing and payments</div>
            <div><CheckCircle2 size={18} /> Real-time business insights</div>
          </div>
        </div>

        <p className="auth-brand-footer">
          © 2026 Brightloop. All rights reserved.
        </p>
      </div>

      <div className="auth-form-panel">
        <div className="auth-form-container">
          <div className="auth-mobile-brand">
            <div className="auth-logo">B</div>
            <span>Brightloop</span>
          </div>

          <div className="auth-heading">
            <span className="auth-eyebrow">
              {isLogin ? "WELCOME BACK" : "GET STARTED"}
            </span>
            <h2>{isLogin ? "Sign in to your account" : "Create your account"}</h2>
            <p>
              {isLogin
                ? "Enter your details to access your dashboard."
                : "Fill in your details to get started with Brightloop."}
            </p>
          </div>

          <form onSubmit={handleSubmit} className="auth-form">
            {!isLogin && (
              <div className="auth-field">
                <label>Full Name</label>
                <div className="auth-input-wrapper">
                  <User size={18} />
                  <input
                    required
                    type="text"
                    placeholder="Enter your full name"
                    value={form.name}
                    onChange={(e) => updateField("name", e.target.value)}
                  />
                </div>
              </div>
            )}

            <div className="auth-field">
              <label>Email Address</label>
              <div className="auth-input-wrapper">
                <Mail size={18} />
                <input
                  required
                  type="email"
                  placeholder="you@example.com"
                  value={form.email}
                  onChange={(e) => updateField("email", e.target.value)}
                />
              </div>
            </div>

            <div className="auth-field">
              <label>Password</label>
              <div className="auth-input-wrapper">
                <Lock size={18} />
                <input
                  required
                  type={showPassword ? "text" : "password"}
                  placeholder="Enter your password"
                  minLength={8}
                  value={form.password}
                  onChange={(e) => updateField("password", e.target.value)}
                />
                <button
                  type="button"
                  className="auth-eye-button"
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                </button>
              </div>
            </div>

            {!isLogin && (
              <div className="auth-field">
                <label>Confirm Password</label>
                <div className="auth-input-wrapper">
                  <Lock size={18} />
                  <input
                    required
                    type={showConfirmPassword ? "text" : "password"}
                    placeholder="Confirm your password"
                    minLength={8}
                    value={form.confirmPassword}
                    onChange={(e) =>
                      updateField("confirmPassword", e.target.value)
                    }
                  />
                  <button
                    type="button"
                    className="auth-eye-button"
                    onClick={() =>
                      setShowConfirmPassword(!showConfirmPassword)
                    }
                  >
                    {showConfirmPassword
                      ? <EyeOff size={18} />
                      : <Eye size={18} />}
                  </button>
                </div>
              </div>
            )}

            {isLogin && (
              <div className="auth-options">
                <label className="auth-remember">
                  <input
                    type="checkbox"
                    checked={form.remember}
                    onChange={(e) => updateField("remember", e.target.checked)}
                  />
                  Remember me
                </label>
                <button
                  type="button"
                  className="auth-text-button"
                  onClick={() =>
                    setMessage("Forgot password will be available after backend integration.")
                  }
                >
                  Forgot password?
                </button>
              </div>
            )}

            {message && (
              <div className="auth-message" role="status">
                {message}
              </div>
            )}

            <button type="submit" className="auth-submit">
              {isLogin ? "Sign In" : "Create Account"}
              <ArrowRight size={18} />
            </button>
          </form>

          <div className="auth-switch">
            {isLogin ? "Don't have an account?" : "Already have an account?"}
            <button
              type="button"
              onClick={() => {
                setIsLogin(!isLogin);
                setMessage("");
              }}
            >
              {isLogin ? "Create account" : "Sign in"}
            </button>
          </div>

          <p className="auth-security">
            Secure access to your Brightloop workspace
          </p>

          {/* ── Demo Access ────────────────────────────────── */}
          <div className="auth-demo-section">
            <div className="auth-demo-divider">
              <span>or try a demo</span>
            </div>
            <p className="auth-demo-note">
              No account needed — select a role to explore the dashboard.
            </p>
            <div className="auth-demo-buttons">
              <button
                type="button"
                className="auth-demo-btn auth-demo-admin"
                onClick={() => onAuthenticated("admin")}
              >
                <span className="auth-demo-icon">🔐</span>
                <span>
                  <strong>Administrator</strong>
                  <small>Full system access</small>
                </span>
              </button>
              <button
                type="button"
                className="auth-demo-btn auth-demo-staff"
                onClick={() => onAuthenticated("staff")}
              >
                <span className="auth-demo-icon">👤</span>
                <span>
                  <strong>Staff Member</strong>
                  <small>Customer management</small>
                </span>
              </button>
              <button
                type="button"
                className="auth-demo-btn auth-demo-customer"
                onClick={() => onAuthenticated("customer")}
              >
                <span className="auth-demo-icon">🏢</span>
                <span>
                  <strong>Customer</strong>
                  <small>Subscription portal</small>
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default Auth;
