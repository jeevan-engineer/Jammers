import { useState } from "react";
import {
  LayoutDashboard,
  Users,
  Repeat,
  FileText,
  CreditCard,
  UserCircle,
  HelpCircle,
  Bell,
  Search,
  Menu,
  Plus,
  ChevronDown,
  Activity,
  Clock,
  TrendingUp,
  LogOut,
  Layers,
} from "lucide-react";

import {
  staffProfile,
  staffStats,
  staffCustomers,
  staffSubscriptions,
  pendingTasks,
  recentActivities,
} from "./data/staffData";
import "./StaffApp.css";

/* ── Navigation definition ─────────────────────────────────────────────────── */

const staffNavigation = [
  { title: "OVERVIEW",    items: [{ label: "Dashboard",    icon: LayoutDashboard }] },
  { title: "MANAGEMENT",  items: [
    { label: "My Customers",   icon: Users       },
    { label: "Subscriptions",  icon: Repeat      },
    { label: "Invoices",       icon: FileText    },
    { label: "Payments",       icon: CreditCard  },
  ]},
  { title: "ACCOUNT",     items: [
    { label: "Profile",        icon: UserCircle  },
    { label: "Help & Support", icon: HelpCircle  },
  ]},
];

/* ── Shared components ─────────────────────────────────────────────────────── */

function StatusBadge({ status }) {
  const map = {
    ACTIVE:    "sbadge-active",
    TRIAL:     "sbadge-trial",
    OVERDUE:   "sbadge-overdue",
    CANCELLED: "sbadge-cancelled",
    HIGH:      "sbadge-overdue",
    MEDIUM:    "sbadge-trial",
    LOW:       "sbadge-low",
  };
  return <span className={`staff-badge ${map[status] || "sbadge-active"}`}>{status}</span>;
}

/* ── Sidebar ───────────────────────────────────────────────────────────────── */

function StaffSidebar({ activePage, setActivePage, open, setOpen, collapsed, setCollapsed, onLogout }) {
  return (
    <>
      {open && <div className="sidebar-overlay" onClick={() => setOpen(false)} />}

      <aside className={`sidebar ${open ? "sidebar-open" : ""} ${collapsed ? "sidebar-collapsed" : ""}`}>

        {/* Brand */}
        <div className="brand">
          <div className="brand-icon">B</div>
          {!collapsed && (
            <div>
              <h2>brightloop</h2>
              <span>STAFF PORTAL</span>
            </div>
          )}
          <button
            className="sidebar-toggle"
            onClick={() => setCollapsed(!collapsed)}
            title={collapsed ? "Expand sidebar" : "Collapse sidebar"}
          >
            <Menu size={19} />
          </button>
        </div>

        {/* Staff identity card */}
        {!collapsed && (
          <div className="staff-identity-card">
            <div className="staff-id-avatar">{staffProfile.avatar}</div>
            <div>
              <strong>{staffProfile.name}</strong>
              <small>{staffProfile.role}</small>
            </div>
          </div>
        )}

        {/* Nav */}
        <nav className="sidebar-nav">
          {staffNavigation.map((group) => (
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
            <div className="help-card">
              <Activity size={19} />
              <strong>Need assistance?</strong>
              <span>Contact your administrator for support.</span>
            </div>
          )}
          <button className="nav-item staff-logout-btn" onClick={onLogout} title="Sign out">
            <LogOut size={18} />
            {!collapsed && <span>Sign out</span>}
          </button>
        </div>
      </aside>
    </>
  );
}

/* ── Dashboard page ─────────────────────────────────────────────────────────── */

function StaffDashboardPage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Staff Portal / Dashboard</p>
          <h1>My Dashboard</h1>
          <p className="subtitle">Welcome back, {staffProfile.name}.</p>
        </div>
        <button className="primary-button"><Plus size={17} /> Add Customer</button>
      </div>

      {/* Stat cards */}
      <div className="stats-grid">
        {staffStats.map((stat) => (
          <div className="stat-card staff-stat-card" key={stat.label}>
            <div className="stat-top">
              <span>{stat.label}</span>
              <div className="stat-icon"><TrendingUp size={19} /></div>
            </div>
            <h2>{stat.value}</h2>
            <p className={stat.positive ? "staff-trend-up" : "staff-trend-down"}>{stat.trend}</p>
          </div>
        ))}
      </div>

      {/* Two-panel row */}
      <div className="dashboard-grid">

        {/* Pending tasks */}
        <section className="panel">
          <div className="panel-header">
            <div>
              <h3>Pending Tasks</h3>
              <p>Items requiring your attention</p>
            </div>
            <span className="staff-badge-count">{pendingTasks.length}</span>
          </div>
          <div className="staff-task-list">
            {pendingTasks.map((task) => (
              <div className="staff-task-item" key={task.id}>
                <div className={`staff-task-icon ${task.type === "INVOICE" ? "task-icon-invoice" : "task-icon-renewal"}`}>
                  {task.type === "INVOICE" ? <FileText size={14} /> : <Clock size={14} />}
                </div>
                <div className="staff-task-body">
                  <p>{task.description}</p>
                  <small>{task.date}</small>
                </div>
                <StatusBadge status={task.priority} />
              </div>
            ))}
          </div>
        </section>

        {/* Recent activities */}
        <section className="panel">
          <div className="panel-header">
            <div>
              <h3>Recent Activities</h3>
              <p>Your latest actions</p>
            </div>
          </div>
          <div className="staff-activity-list">
            {recentActivities.map((a) => (
              <div className="staff-activity-item" key={a.id}>
                <div className="staff-activity-dot" />
                <div>
                  <p><strong>{a.action}</strong></p>
                  <small>{a.detail}</small>
                  <span className="staff-activity-time">{a.time}</span>
                </div>
              </div>
            ))}
          </div>
        </section>
      </div>

      {/* Quick actions */}
      <section className="panel staff-quick-panel">
        <div className="panel-header">
          <div><h3>Quick Actions</h3><p>Common tasks</p></div>
        </div>
        <div className="staff-quick-grid">
          {[
            { icon: Users,      label: "Add Customer",        sub: "Register a new customer"   },
            { icon: Repeat,     label: "View Subscriptions",  sub: "Manage active plans"        },
            { icon: FileText,   label: "Create Invoice",      sub: "Generate a new invoice"     },
            { icon: CreditCard, label: "Record Payment",      sub: "Log a received payment"     },
          ].map(({ icon: Icon, label, sub }) => (
            <button className="staff-quick-card" key={label}>
              <Icon size={22} />
              <strong>{label}</strong>
              <small>{sub}</small>
            </button>
          ))}
        </div>
      </section>
    </>
  );
}

/* ── My Customers page ──────────────────────────────────────────────────────── */

function MyCustomersPage() {
  const [search, setSearch] = useState("");
  const filtered = staffCustomers.filter((c) =>
    `${c.name} ${c.email} ${c.plan}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Staff Portal / My Customers</p>
          <h1>My Customers</h1>
          <p className="subtitle">Customers assigned to your account.</p>
        </div>
        <button className="primary-button"><Plus size={17} /> Add Customer</button>
      </div>

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search customers..." />
        </div>
        <span>{filtered.length} customers</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>CUSTOMER</th><th>EMAIL</th><th>PLAN</th><th>STATUS</th><th>ASSIGNED</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan="5" className="table-empty">
                    <Users size={28} /><strong>No customers found</strong><p>No customers match your search.</p>
                  </td>
                </tr>
              ) : filtered.map((c) => (
                <tr key={c.id}>
                  <td><strong>{c.name}</strong></td>
                  <td>{c.email}</td>
                  <td>{c.plan}</td>
                  <td><StatusBadge status={c.subscription} /></td>
                  <td>{c.assignedDate}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

/* ── Subscriptions page ─────────────────────────────────────────────────────── */

function StaffSubscriptionsPage() {
  const [search, setSearch] = useState("");
  const filtered = staffSubscriptions.filter((s) =>
    `${s.customer} ${s.plan}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Staff Portal / Subscriptions</p>
          <h1>Subscriptions</h1>
          <p className="subtitle">Subscriptions across your assigned customers.</p>
        </div>
      </div>

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input value={search} onChange={(e) => setSearch(e.target.value)} placeholder="Search subscriptions..." />
        </div>
        <span>{filtered.length} subscriptions</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>CUSTOMER</th><th>PLAN</th><th>AMOUNT</th><th>RENEWAL DATE</th><th>STATUS</th>
              </tr>
            </thead>
            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan="5" className="table-empty">
                    <Repeat size={28} /><strong>No subscriptions</strong><p>No subscriptions match your search.</p>
                  </td>
                </tr>
              ) : filtered.map((s) => (
                <tr key={s.id}>
                  <td><strong>{s.customer}</strong></td>
                  <td>{s.plan}</td>
                  <td>{s.amount}</td>
                  <td>{s.renewalDate}</td>
                  <td><StatusBadge status={s.status} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </>
  );
}

/* ── Profile page ───────────────────────────────────────────────────────────── */

function StaffProfilePage() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Staff Portal / Profile</p>
          <h1>My Profile</h1>
          <p className="subtitle">Your account and personal details.</p>
        </div>
      </div>

      <div className="panel staff-profile-panel">
        <div className="staff-profile-header">
          <div className="staff-profile-avatar-lg">{staffProfile.avatar}</div>
          <div>
            <h3>{staffProfile.name}</h3>
            <p className="subtitle">{staffProfile.role} · {staffProfile.department}</p>
          </div>
        </div>

        <div className="staff-profile-fields">
          {[
            { label: "Full Name",   value: staffProfile.name,       readOnly: false },
            { label: "Email",       value: staffProfile.email,      readOnly: false },
            { label: "Department",  value: staffProfile.department, readOnly: true  },
            { label: "Joined",      value: staffProfile.joinedDate, readOnly: true  },
          ].map(({ label, value, readOnly }) => (
            <div className="staff-profile-field" key={label}>
              <label>{label}</label>
              <input defaultValue={value} readOnly={readOnly} />
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

/* ── Generic placeholder ────────────────────────────────────────────────────── */

function StaffPlaceholder({ title, icon: Icon, description }) {
  return (
    <div className="placeholder-page">
      <div className="placeholder-icon"><Icon size={27} /></div>
      <h2>{title}</h2>
      <p>{description}</p>
      <span>Backend integration pending</span>
    </div>
  );
}

/* ── Main StaffApp ──────────────────────────────────────────────────────────── */

export default function StaffApp({ onLogout }) {
  const [activePage,       setActivePage]       = useState("Dashboard");
  const [sidebarOpen,      setSidebarOpen]      = useState(false);
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  function renderPage() {
    switch (activePage) {
      case "Dashboard":    return <StaffDashboardPage />;
      case "My Customers": return <MyCustomersPage />;
      case "Subscriptions":return <StaffSubscriptionsPage />;
      case "Invoices":     return <StaffPlaceholder title="Invoices"       icon={FileText}  description="View and manage invoices for your customers." />;
      case "Payments":     return <StaffPlaceholder title="Payments"       icon={CreditCard}description="Track payments collected from your customers." />;
      case "Profile":      return <StaffProfilePage />;
      case "Help & Support":return <StaffPlaceholder title="Help & Support" icon={HelpCircle}description="Get help from your administrator or view documentation." />;
      default: return null;
    }
  }

  return (
    <div className="app-shell">
      <StaffSidebar
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
            <input placeholder="Search anything..." />
            <kbd>⌘ K</kbd>
          </div>
          <div className="topbar-right">
            <button className="icon-button"><Bell size={19} /></button>
            <div className="topbar-user">
              <div className="profile-avatar staff-topbar-avatar">{staffProfile.avatar}</div>
              <div>
                <strong>{staffProfile.name}</strong>
                <small>{staffProfile.role}</small>
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

