import { useState } from "react";
import {
  LayoutDashboard,
  Repeat,
  Layers,
  FileText,
  CreditCard,
  UserCircle,
  HelpCircle,
  Bell,
  Search,
  Menu,
  CheckCircle2,
  ArrowUpRight,
  Download,
  LogOut,
  ChevronDown,
  Star,
  Zap,
} from "lucide-react";

import {
  customerProfile,
  currentSubscription,
  availablePlans,
  customerInvoices,
  paymentHistory,
} from "./data/customerData";
import "./CustomerApp.css";

/* ── Navigation ─────────────────────────────────────────────────────────────── */

const customerNavigation = [
  { title: "PORTAL",       items: [{ label: "Overview",        icon: LayoutDashboard }] },
  { title: "SUBSCRIPTION", items: [
    { label: "My Subscription", icon: Repeat  },
    { label: "Explore Plans",   icon: Layers  },
  ]},
  { title: "BILLING",      items: [
    { label: "Invoices",        icon: FileText   },
    { label: "Payment History", icon: CreditCard },
  ]},
  { title: "ACCOUNT",      items: [
    { label: "My Profile",      icon: UserCircle },
    { label: "Help & Support",  icon: HelpCircle },
  ]},
];

/* ── Status badge ───────────────────────────────────────────────────────────── */

function CustBadge({ status }) {
  const map = {
    ACTIVE:  "cbadge-active",
    PAID:    "cbadge-active",
    SUCCESS: "cbadge-active",
    UNPAID:  "cbadge-unpaid",
    CANCELLED:"cbadge-cancelled",
    TRIAL:   "cbadge-trial",
  };
  return <span className={`cust-badge ${map[status] || "cbadge-active"}`}>{status}</span>;
}

/* ── Customer Sidebar ───────────────────────────────────────────────────────── */

function CustomerSidebar({ activePage, setActivePage, open, setOpen, collapsed, setCollapsed, onLogout }) {
  return (
    <>
      {open && <div className="sidebar-overlay" onClick={() => setOpen(false)} />}

      <aside className={`sidebar cust-sidebar ${open ? "sidebar-open" : ""} ${collapsed ? "sidebar-collapsed" : ""}`}>

        {/* Brand */}
        <div className="brand">
          <div className="brand-icon cust-brand-icon">B</div>
          {!collapsed && (
            <div>
              <h2>brightloop</h2>
              <span>MY PORTAL</span>
            </div>
          )}
          <button
            className="sidebar-toggle"
            onClick={() => setCollapsed(!collapsed)}
            title={collapsed ? "Expand" : "Collapse"}
          >
            <Menu size={19} />
          </button>
        </div>

        {/* Customer identity */}
        {!collapsed && (
          <div className="cust-identity-card">
            <div className="cust-id-avatar">{customerProfile.avatar}</div>
            <div>
              <strong>{customerProfile.name}</strong>
              <small>{customerProfile.company}</small>
            </div>
            <ChevronDown size={13} style={{ marginLeft: "auto", color: "#91a0b7" }} />
          </div>
        )}

        {/* Nav */}
        <nav className="sidebar-nav">
          {customerNavigation.map((group) => (
            <div className="nav-group" key={group.title}>
              {!collapsed && <p className="nav-heading">{group.title}</p>}
              {group.items.map(({ label, icon: Icon }) => (
                <button
                  key={label}
                  title={collapsed ? label : ""}
                  className={`nav-item ${activePage === label ? "active" : ""}`}
                  onClick={() => { setActivePage(label); setOpen(false); }}
                >
                  <Icon size={18} />
                  {!collapsed && <span>{label}</span>}
                </button>
              ))}
            </div>
          ))}
        </nav>

        {/* Bottom */}
        <div className="sidebar-bottom">
          {!collapsed && (
            <div className="cust-plan-pill">
              <Zap size={13} />
              <span>{currentSubscription.plan}</span>
              <span className="cust-plan-pill-badge">ACTIVE</span>
            </div>
          )}
          <button className="nav-item cust-logout-btn" onClick={onLogout} title="Sign out">
            <LogOut size={18} />
            {!collapsed && <span>Sign out</span>}
          </button>
        </div>
      </aside>
    </>
  );
}

/* ── Overview page ──────────────────────────────────────────────────────────── */

function OverviewPage({ setActivePage }) {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">My Portal / Overview</p>
          <h1>Welcome back, {customerProfile.name.split(" ")[0]} 👋</h1>
          <p className="subtitle">{customerProfile.company}</p>
        </div>
      </div>

      {/* Subscription hero banner */}
      <div className="cust-banner">
        <div className="cust-banner-left">
          <div className="cust-banner-icon"><Star size={20} /></div>
          <div>
            <strong>{currentSubscription.plan}</strong>
            <p>Next renewal: {currentSubscription.nextRenewal} · {currentSubscription.price}/month</p>
          </div>
        </div>
        <div className="cust-banner-right">
          <CustBadge status={currentSubscription.status} />
          <button className="primary-button" onClick={() => setActivePage("Explore Plans")}>
            <ArrowUpRight size={15} /> Upgrade Plan
          </button>
        </div>
      </div>

      {/* Overview stat cards */}
      <div className="cust-overview-grid">
        {[
          { label: "Current Plan",   value: currentSubscription.plan,         sub: `Billed ${currentSubscription.billingCycle}` },
          { label: "Plan Status",    value: currentSubscription.status,        sub: "No issues detected",  badge: true },
          { label: "Next Renewal",   value: currentSubscription.nextRenewal,   sub: `${currentSubscription.price} will be charged` },
          { label: "Member Since",   value: customerProfile.joinedDate,        sub: "Brightloop customer" },
        ].map(({ label, value, sub, badge }) => (
          <div className="cust-stat-card" key={label}>
            <p>{label}</p>
            {badge ? <h3><CustBadge status={value} /></h3> : <h3>{value}</h3>}
            <small>{sub}</small>
          </div>
        ))}
      </div>

      <div className="dashboard-grid">
        {/* Recent invoices */}
        <section className="panel">
          <div className="panel-header">
            <div>
              <h3>Recent Invoices</h3>
              <p>Your latest billing statements</p>
            </div>
            <button className="text-button" onClick={() => setActivePage("Invoices")}>View all</button>
          </div>
          <div className="table-scroll" style={{ marginTop: 14 }}>
            <table>
              <thead>
                <tr><th>INVOICE</th><th>DATE</th><th>AMOUNT</th><th>STATUS</th></tr>
              </thead>
              <tbody>
                {customerInvoices.slice(0, 3).map((inv) => (
                  <tr key={inv.id}>
                    <td><strong>{inv.number}</strong></td>
                    <td>{inv.date}</td>
                    <td>{inv.amount}</td>
                    <td><CustBadge status={inv.status} /></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>

        {/* Plan features */}
        <section className="panel cust-features-panel">
          <div className="panel-header">
            <div>
              <h3>Plan Features</h3>
              <p>{currentSubscription.plan}</p>
            </div>
          </div>
          <div className="cust-feature-list">
            {currentSubscription.features.map((f) => (
              <div className="cust-feature-item" key={f}>
                <CheckCircle2 size={15} className="cust-check" />
                <span>{f}</span>
              </div>
            ))}
          </div>
          <button className="cust-compare-btn" onClick={() => setActivePage("Explore Plans")}>
            <Layers size={14} /> Compare all plans
          </button>
        </section>
      </div>
    </>
  );
}

/* ── My Subscription page ───────────────────────────────────────────────────── */

function MySubscriptionPage() {
  const [showCancel, setShowCancel] = useState(false);

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">My Portal / My Subscription</p>
          <h1>My Subscription</h1>
          <p className="subtitle">Manage your plan and billing details.</p>
        </div>
      </div>

      <div className="cust-sub-grid">
        {/* Subscription detail card */}
        <section className="panel cust-sub-detail">
          <div className="cust-sub-head">
            <div className="cust-sub-plan-icon"><Star size={22} /></div>
            <div>
              <h3>{currentSubscription.plan}</h3>
              <CustBadge status={currentSubscription.status} />
            </div>
            <div className="cust-sub-price">
              <strong>{currentSubscription.price}</strong>
              <small>/ {currentSubscription.billingCycle}</small>
            </div>
          </div>

          <div className="cust-sub-info-grid">
            {[
              { label: "Start Date",    value: currentSubscription.startDate    },
              { label: "Next Renewal",  value: currentSubscription.nextRenewal  },
              { label: "Billing Cycle", value: currentSubscription.billingCycle },
              { label: "Status",        value: currentSubscription.status       },
            ].map(({ label, value }) => (
              <div className="cust-info-item" key={label}>
                <label>{label}</label>
                <span>{value}</span>
              </div>
            ))}
          </div>

          <div className="cust-sub-features">
            <p className="cust-features-heading">Included Features</p>
            {currentSubscription.features.map((f) => (
              <div className="cust-feature-item" key={f}>
                <CheckCircle2 size={14} className="cust-check" />
                <span>{f}</span>
              </div>
            ))}
          </div>
        </section>

        {/* Action panel */}
        <div className="cust-action-col">
          <section className="panel cust-action-panel">
            <h4>Subscription Actions</h4>
            <p className="subtitle" style={{ marginBottom: 20 }}>Manage your plan</p>
            <div className="cust-action-list">
              <button className="primary-button cust-full-btn"><ArrowUpRight size={15} /> Upgrade Plan</button>
              <button className="select-button cust-full-btn"><Layers size={15} /> Change Plan</button>
              <button className="cust-danger-outline" onClick={() => setShowCancel(true)}>
                Cancel Subscription
              </button>
            </div>
          </section>

          <section className="panel cust-action-panel" style={{ marginTop: 16 }}>
            <h4>Payment Method</h4>
            <p className="subtitle" style={{ marginBottom: 16 }}>Your billing information</p>
            <div className="cust-billing-row">
              <div className="cust-billing-method">
                <CreditCard size={18} />
                <div>
                  <strong>UPI</strong>
                  <small>priya@acme-upi</small>
                </div>
              </div>
              <button className="text-button">Update</button>
            </div>
          </section>
        </div>
      </div>

      {/* Cancel confirmation modal */}
      {showCancel && (
        <div className="customer-modal-overlay">
          <div className="customer-modal">
            <div className="customer-modal-header">
              <div>
                <h2>Cancel Subscription</h2>
                <p>This will cancel your {currentSubscription.plan} plan.</p>
              </div>
              <button className="icon-button" onClick={() => setShowCancel(false)} aria-label="Close">✕</button>
            </div>
            <p style={{ fontSize: 13, color: "#64748b", marginBottom: 22, lineHeight: 1.6 }}>
              Your subscription will remain active until <strong>{currentSubscription.nextRenewal}</strong>. After that, you will lose access to all premium features.
            </p>
            <div className="customer-modal-actions">
              <button className="select-button" onClick={() => setShowCancel(false)}>Keep Subscription</button>
              <button className="cust-danger-btn">Confirm Cancellation</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

/* ── Explore Plans page ─────────────────────────────────────────────────────── */

function ExplorePlansPage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">My Portal / Explore Plans</p>
          <h1>Available Plans</h1>
          <p className="subtitle">Choose a plan that fits your business needs.</p>
        </div>
      </div>

      <div className="cust-plans-grid">
        {availablePlans.map((plan) => (
          <div className={`cust-plan-card ${plan.highlight ? "cust-plan-featured" : ""}`} key={plan.id}>
            {plan.highlight && <div className="cust-plan-tag">Current Plan</div>}
            <div className="cust-plan-header">
              <h3>{plan.name}</h3>
              <div className="cust-plan-price">
                <strong>{plan.price}</strong>
                <small>{plan.billingCycle}</small>
              </div>
              <p className="cust-plan-yearly">{plan.yearlyPrice}</p>
            </div>
            <div className="cust-feature-list cust-plan-features">
              {plan.features.map((f) => (
                <div className="cust-feature-item" key={f}>
                  <CheckCircle2 size={13} className="cust-check" />
                  <span>{f}</span>
                </div>
              ))}
            </div>
            <button className={plan.isCurrent ? "cust-current-btn" : "primary-button cust-full-btn"}>
              {plan.isCurrent ? "✓ Current Plan" : "Select Plan"}
            </button>
          </div>
        ))}
      </div>
    </>
  );
}

/* ── Invoices page ──────────────────────────────────────────────────────────── */

function CustomerInvoicesPage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Billing / Invoices</p>
          <h1>Invoices</h1>
          <p className="subtitle">Your billing statements from Brightloop.</p>
        </div>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr><th>INVOICE</th><th>ISSUE DATE</th><th>DUE DATE</th><th>AMOUNT</th><th>STATUS</th><th>ACTION</th></tr>
            </thead>
            <tbody>
              {customerInvoices.map((inv) => (
                <tr key={inv.id}>
                  <td><strong>{inv.number}</strong></td>
                  <td>{inv.date}</td>
                  <td>{inv.dueDate}</td>
                  <td>{inv.amount}</td>
                  <td><CustBadge status={inv.status} /></td>
                  <td>
                    <button className="icon-button" title="Download invoice">
                      <Download size={16} />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

/* ── Payment History page ───────────────────────────────────────────────────── */

function PaymentHistoryPage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Billing / Payment History</p>
          <h1>Payment History</h1>
          <p className="subtitle">All your past transactions with Brightloop.</p>
        </div>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr><th>DATE</th><th>DESCRIPTION</th><th>METHOD</th><th>AMOUNT</th><th>STATUS</th></tr>
            </thead>
            <tbody>
              {paymentHistory.map((p) => (
                <tr key={p.id}>
                  <td>{p.date}</td>
                  <td>{p.description}</td>
                  <td>{p.method}</td>
                  <td><strong>{p.amount}</strong></td>
                  <td><CustBadge status={p.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

/* ── My Profile page ────────────────────────────────────────────────────────── */

function CustomerProfilePage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Account / My Profile</p>
          <h1>My Profile</h1>
          <p className="subtitle">Manage your account information.</p>
        </div>
      </div>

      <div className="panel cust-profile-panel">
        <div className="cust-profile-header">
          <div className="cust-profile-avatar-lg">{customerProfile.avatar}</div>
          <div>
            <h3>{customerProfile.name}</h3>
            <p className="subtitle">{customerProfile.company}</p>
          </div>
        </div>

        <div className="cust-profile-fields">
          {[
            { label: "Full Name",     value: customerProfile.name    },
            { label: "Email Address", value: customerProfile.email   },
            { label: "Company",       value: customerProfile.company },
            { label: "Phone",         value: customerProfile.phone   },
            { label: "Address",       value: customerProfile.address },
          ].map(({ label, value }) => (
            <div className="cust-profile-field" key={label}>
              <label>{label}</label>
              <input defaultValue={value} />
            </div>
          ))}
        </div>

        <div style={{ marginTop: 24 }}>
          <button className="primary-button">Save changes</button>
        </div>
      </div>
    </>
  );
}

/* ── Help page ──────────────────────────────────────────────────────────────── */

function CustomerHelpPage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Account / Help & Support</p>
          <h1>Help & Support</h1>
          <p className="subtitle">Get assistance with your Brightloop subscription.</p>
        </div>
      </div>
      <div className="placeholder-page">
        <div className="placeholder-icon"><HelpCircle size={27} /></div>
        <h2>Need assistance?</h2>
        <p>Contact our support team for help with your subscription or account.</p>
        <span>Support integration pending</span>
      </div>
    </>
  );
}

/* ── Main CustomerApp ───────────────────────────────────────────────────────── */

export default function CustomerApp({ onLogout }) {
  const [activePage,       setActivePage]       = useState("Overview");
  const [sidebarOpen,      setSidebarOpen]      = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  function renderPage() {
    switch (activePage) {
      case "Overview":        return <OverviewPage setActivePage={setActivePage} />;
      case "My Subscription": return <MySubscriptionPage />;
      case "Explore Plans":   return <ExplorePlansPage />;
      case "Invoices":        return <CustomerInvoicesPage />;
      case "Payment History": return <PaymentHistoryPage />;
      case "My Profile":      return <CustomerProfilePage />;
      case "Help & Support":  return <CustomerHelpPage />;
      default: return null;
    }
  }

  return (
    <div className="app-shell">
      <CustomerSidebar
        activePage={activePage}
        setActivePage={setActivePage}
        open={sidebarOpen}
        setOpen={setSidebarOpen}
        collapsed={sidebarCollapsed}
        setCollapsed={setSidebarCollapsed}
        onLogout={onLogout}
      />

      <main className={`main-content ${sidebarCollapsed ? "main-content-expanded" : ""}`}>
        <header className="topbar">
          <button className="mobile-menu" onClick={() => setSidebarOpen(true)}>
            <Menu size={21} />
          </button>
          <div className="topbar-search">
            <Search size={17} />
            <input placeholder="Search..." />
            <kbd>⌘ K</kbd>
          </div>
          <div className="topbar-right">
            <button className="icon-button"><Bell size={19} /></button>
            <div className="topbar-user">
              <div className="profile-avatar cust-topbar-avatar">{customerProfile.avatar}</div>
              <div>
                <strong>{customerProfile.name}</strong>
                <small>Customer</small>
              </div>
            </div>
          </div>
        </header>

        <div className="page-container">
          {renderPage()}
          <footer>© 2026 Brightloop Software. All rights reserved.</footer>
        </div>
      </main>
    </div>
  );
}

