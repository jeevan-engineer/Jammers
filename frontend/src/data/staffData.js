/**
 * Staff Portal – Sample Data
 * ─────────────────────────────────────────────────────────────────────────────
 * Replace these exports with real API responses when the backend is connected.
 * Do NOT import this file inside reusable UI components — use it only in page
 * components (StaffApp.jsx) so the swap to API data stays localised.
 */

export const staffProfile = {
  name: "Rahul Sharma",
  email: "rahul.sharma@brightloop.com",
  role: "Staff Member",
  avatar: "RS",
  department: "Customer Success",
  joinedDate: "15 Mar 2025",
};

export const staffStats = [
  { label: "Assigned Customers", value: "24", trend: "+3 this month", positive: true },
  { label: "Active Subscriptions", value: "19", trend: "+2 this month", positive: true },
  { label: "Pending Invoices", value: "7", trend: "Requires attention", positive: false },
  { label: "Payments Collected", value: "₹1,84,500", trend: "+₹12,000 this month", positive: true },
];

export const staffCustomers = [
  { id: 1, name: "Priya Nair",    email: "priya@acmecorp.in",     subscription: "ACTIVE",    assignedDate: "12 Jan 2026", plan: "Premium Monthly" },
  { id: 2, name: "Arjun Mehta",  email: "arjun@techvision.io",   subscription: "ACTIVE",    assignedDate: "03 Feb 2026", plan: "Starter Yearly"  },
  { id: 3, name: "Kavya Reddy",  email: "kavya@nexgen.co.in",    subscription: "TRIAL",     assignedDate: "20 Mar 2026", plan: "Pro Monthly"      },
  { id: 4, name: "Deepak Joshi", email: "deepak@infosolve.in",   subscription: "OVERDUE",   assignedDate: "08 Nov 2025", plan: "Premium Yearly"   },
  { id: 5, name: "Sneha Iyer",   email: "sneha@cloudbase.io",    subscription: "ACTIVE",    assignedDate: "17 Apr 2026", plan: "Starter Monthly"  },
  { id: 6, name: "Vikram Singh", email: "vikram@datanodes.in",   subscription: "CANCELLED", assignedDate: "22 Sep 2025", plan: "Pro Yearly"       },
];

export const staffSubscriptions = [
  { id: 1, customer: "Priya Nair",    plan: "Premium Monthly", amount: "₹4,999",  renewalDate: "12 Dec 2026", status: "ACTIVE"    },
  { id: 2, customer: "Arjun Mehta",  plan: "Starter Yearly",  amount: "₹9,999",  renewalDate: "03 Feb 2027", status: "ACTIVE"    },
  { id: 3, customer: "Kavya Reddy",  plan: "Pro Monthly",     amount: "₹7,499",  renewalDate: "20 Apr 2026", status: "TRIAL"     },
  { id: 4, customer: "Deepak Joshi", plan: "Premium Yearly",  amount: "₹49,999", renewalDate: "08 Nov 2026", status: "OVERDUE"   },
  { id: 5, customer: "Sneha Iyer",   plan: "Starter Monthly", amount: "₹1,999",  renewalDate: "17 May 2026", status: "ACTIVE"    },
];

export const pendingTasks = [
  { id: 1, type: "INVOICE",  description: "Invoice INV-0021 overdue for Deepak Joshi",    priority: "HIGH",   date: "08 Nov 2026" },
  { id: 2, type: "RENEWAL",  description: "Kavya Reddy trial ending in 3 days",           priority: "MEDIUM", date: "23 Nov 2026" },
  { id: 3, type: "INVOICE",  description: "Invoice INV-0019 pending for Arjun Mehta",     priority: "LOW",    date: "30 Nov 2026" },
  { id: 4, type: "RENEWAL",  description: "Sneha Iyer subscription renews in 7 days",     priority: "LOW",    date: "17 Nov 2026" },
];

export const recentActivities = [
  { id: 1, action: "Added new customer",      detail: "Sneha Iyer (sneha@cloudbase.io)",          time: "2 hours ago" },
  { id: 2, action: "Created invoice",         detail: "INV-0024 for Priya Nair — ₹4,999",         time: "5 hours ago" },
  { id: 3, action: "Updated subscription",    detail: "Arjun Mehta → Starter Yearly",             time: "Yesterday"   },
  { id: 4, action: "Recorded payment",        detail: "₹7,499 received from Kavya Reddy",         time: "2 days ago"  },
  { id: 5, action: "Cancelled subscription",  detail: "Vikram Singh cancelled Pro Yearly",        time: "3 days ago"  },
];

