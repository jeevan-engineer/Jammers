import Products from "./Products.jsx";
import Invoices from "./Invoices.jsx";
import Customers from "./Customers.jsx";
import Taxes from "./Taxes.jsx";
import Payments from "./Payments.jsx";
import Discounts from "./Discounts.jsx";
import Auth from "./Auth.jsx";
import StaffApp from "./StaffApp.jsx";
import CustomerApp from "./CustomerApp.jsx";

import Users from "./Users.jsx";
import Plans from "./Plans.jsx";
import Subscriptions from "./Subscriptions.jsx";
import { useState } from "react";
import {
  LayoutDashboard,
  Package,
  Layers,
  Users as UsersIcon,
  Repeat,
  FileText,
  CreditCard,
  Percent,
  Tag,
  UserCog,
  Bell,
  Search,
  Menu,
  Wallet,
  CircleAlert,
  CalendarDays,
  Plus,
  ChevronDown,
  Activity,
  LogOut,
} from "lucide-react";

import "./App.css";

const navigation = [
  {
    title: "OVERVIEW", items: [
      { label: "Dashboard", icon: LayoutDashboard },
    ]
  },
  {
    title: "MANAGEMENT", items: [
      { label: "Products", icon: Package },
      { label: "Plans", icon: Layers },
      { label: "Customers", icon: UsersIcon },
      { label: "Subscriptions", icon: Repeat },
      { label: "Invoices", icon: FileText },
      { label: "Payments", icon: CreditCard },
    ]
  },
  {
    title: "CONFIGURATION", items: [
      { label: "Taxes", icon: Percent },
      { label: "Discounts", icon: Tag },
      { label: "Users", icon: UserCog },
    ]
  },
];

const stats = [
  { label: "Active Subscriptions", value: "—", icon: Repeat, note: "Awaiting API data" },
  { label: "Revenue Collected", value: "—", icon: Wallet, note: "Awaiting API data" },
  { label: "Outstanding Amount", value: "—", icon: CircleAlert, note: "Awaiting API data" },
  { label: "Overdue Invoices", value: "—", icon: FileText, note: "Awaiting API data" },
];



function Sidebar({
  activePage,
  setActivePage,
  open,
  setOpen,
  collapsed,
  setCollapsed,
  onLogout,
}) {
  return (
    <>
      {open && (
        <div
          className="sidebar-overlay"
          onClick={() => setOpen(false)}
        />
      )}

      <aside
        className={`sidebar ${open ? "sidebar-open" : ""} ${
          collapsed ? "sidebar-collapsed" : ""
        }`}
      >
        <div className="brand">
          <div className="brand-icon">B</div>

          {!collapsed && (
            <div>
              <h2>brightloop</h2>
              <span>SOFTWARE</span>
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

        {!collapsed && (
          <div className="workspace">
            <div className="workspace-avatar">B</div>
            <div>
              <strong>Brightloop Workspace</strong>
              <small>Business account</small>
            </div>
            <ChevronDown size={15} />
          </div>
        )}

        <nav className="sidebar-nav">
          {navigation.map((group) => (
            <div className="nav-group" key={group.title}>
              {!collapsed && (
                <p className="nav-heading">{group.title}</p>
              )}

              {group.items.map(({ label, icon: Icon }) => (
                <button
                  key={label}
                  title={collapsed ? label : ""}
                  className={`nav-item ${
                    activePage === label ? "active" : ""
                  }`}
                  onClick={() => {
                    setActivePage(label);
                    setOpen(false);
                  }}
                >
                  <Icon size={19} />
                  {!collapsed && <span>{label}</span>}
                </button>
              ))}
            </div>
          ))}
        </nav>

        <div className="sidebar-bottom">
          {!collapsed && (
            <div className="help-card">
              <Activity size={19} />
              <strong>Need assistance?</strong>
              <span>Contact your administrator for support.</span>
            </div>
          )}

          <div className="profile">
            <div className="profile-avatar">JN</div>

            {!collapsed && (
              <div>
                <strong>Administrator</strong>
                <small>Workspace admin</small>
              </div>
            )}

            {!collapsed && <ChevronDown size={15} />}
          </div>

          <button
            className="nav-item admin-logout-btn"
            onClick={onLogout}
            title="Sign out"
          >
            <LogOut size={18} />
            {!collapsed && <span>Sign out</span>}
          </button>
        </div>
      </aside>
    </>
  );
}

function StatCard({ item }) {
  const Icon = item.icon;
  return (
    <div className="stat-card">
      <div className="stat-top">
        <span>{item.label}</span>
        <div className="stat-icon"><Icon size={19} /></div>
      </div>
      <h2>{item.value}</h2>
      <p>{item.note}</p>
    </div>
  );
}

function Dashboard() {
  return (
    <>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Overview / Dashboard</p>
          <h1>Dashboard</h1>
          <p className="subtitle">Monitor your subscription business in one place.</p>
        </div>
        <button className="primary-button">
          <Plus size={17} /> Create subscription
        </button>
      </div>

      <div className="stats-grid">
        {stats.map((item) => <StatCard key={item.label} item={item} />)}
      </div>

      <div className="dashboard-grid">
        <section className="panel revenue-panel">
          <div className="panel-header">
            <div>
              <h3>Revenue overview</h3>
              <p>Revenue performance over time</p>
            </div>
            <button className="select-button">
              <CalendarDays size={15} /> This year <ChevronDown size={14} />
            </button>
          </div>

          <div className="chart-empty">
            <Activity size={30} />
            <strong>Revenue data will appear here</strong>
            <span>Connect the reports API to display revenue analytics.</span>
          </div>
        </section>

        <section className="panel">
          <div className="panel-header">
            <div>
              <h3>Monthly recurring revenue</h3>
              <p>Current recurring revenue</p>
            </div>
          </div>
          <div className="mrr-value">—</div>
          <div className="empty-message">
            <Wallet size={23} />
            <span>Waiting for subscription analytics API.</span>
          </div>
        </section>
      </div>

      <section className="panel payments-panel">
        <div className="panel-header">
          <div>
            <h3>Recent payments</h3>
            <p>Latest transactions from your customers</p>
          </div>
          <button className="text-button">View all</button>
        </div>

        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>TRANSACTION</th>
                <th>CUSTOMER</th>
                <th>DATE</th>
                <th>AMOUNT</th>
                <th>STATUS</th>
              </tr>
            </thead>
            <tbody>
              <tr>
                <td colSpan="5" className="table-empty">
                  No payment records available yet.
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </>
  );
}

function PlaceholderPage({ title }) {
  return (
    <div className="placeholder-page">
      <div className="placeholder-icon"><Layers size={27} /></div>
      <h2>{title}</h2>
      <p>This module is being prepared for backend integration.</p>
      <span>API connection pending</span>
    </div>
  );
}


function App() {
  const [activePage,       setActivePage]       = useState("Dashboard");
  const [sidebarOpen,      setSidebarOpen]      = useState(false);
  const [userRole,         setUserRole]         = useState(null); // null | "admin" | "staff" | "customer"
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);

  // ── Auth gate ────────────────────────────────────────────────────────────
  // onAuthenticated(role) is called by Auth — either from the demo buttons
  // or (later) from a real backend response.  Replace the demo wiring with
  // an API call that returns the user's actual role.
  if (!userRole) {
    return <Auth onAuthenticated={(role) => setUserRole(role)} />;
  }

  // ── Role-based routing ───────────────────────────────────────────────────
  if (userRole === "staff") {
    return <StaffApp onLogout={() => setUserRole(null)} />;
  }

  if (userRole === "customer") {
    return <CustomerApp onLogout={() => setUserRole(null)} />;
  }

  // ── Administrator dashboard (existing) ───────────────────────────────────
  return (
    <div className="app-shell">
      <Sidebar
        activePage={activePage}
        setActivePage={setActivePage}
        open={sidebarOpen}
        setOpen={setSidebarOpen}
        collapsed={sidebarCollapsed}
        setCollapsed={setSidebarCollapsed}
        onLogout={() => setUserRole(null)}
      />

      <main
        className={`main-content ${
          sidebarCollapsed ? "main-content-expanded" : ""
        }`}
      >
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
            <button className="icon-button">
              <Bell size={19} />
            </button>

            <div className="topbar-user">
              <div className="profile-avatar">JN</div>
              <div>
                <strong>Administrator</strong>
                <small>Admin</small>
              </div>
            </div>

            <button
              className="icon-button admin-topbar-logout"
              onClick={() => setUserRole(null)}
              title="Sign out"
            >
              <LogOut size={19} />
            </button>
          </div>
        </header>

        <div className="page-container">
          {activePage === "Dashboard" ? (
            <Dashboard />
          ) : activePage === "Customers" ? (
            <Customers />
          ) : activePage === "Products" ? (
            <Products />
          ) : activePage === "Plans" ? (
            <Plans />
          ) : activePage === "Subscriptions" ? (
            <Subscriptions />
          ) : activePage === "Invoices" ? (
            <Invoices />
          ) : activePage === "Payments" ? (
            <Payments />
          ) : activePage === "Taxes" ? (
            <Taxes />
          ) : activePage === "Discounts" ? (
            <Discounts />
          ) : activePage === "Users" ? (
            <Users />
          ) : (
            <PlaceholderPage title={activePage} />
          )}

          <footer>© 2026 Brightloop Software. All rights reserved.</footer>
        </div>
      </main>
    </div>
  );
}

export default App;
