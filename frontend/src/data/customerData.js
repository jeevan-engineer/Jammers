/**
 * Customer Portal – Sample Data
 * ─────────────────────────────────────────────────────────────────────────────
 * Replace these exports with real API responses when the backend is connected.
 * Do NOT import this file inside reusable UI components.
 */

export const customerProfile = {
  name: "Priya Nair",
  email: "priya@acmecorp.in",
  company: "Acme Corporation",
  phone: "+91 98765 43210",
  avatar: "PN",
  address: "Bangalore, Karnataka, India",
  joinedDate: "12 Jan 2026",
};

export const currentSubscription = {
  plan: "Premium Monthly",
  status: "ACTIVE",
  startDate: "12 Jan 2026",
  nextRenewal: "12 Dec 2026",
  price: "₹4,999",
  billingCycle: "Monthly",
  features: [
    "Unlimited customers",
    "Advanced analytics",
    "Priority support",
    "Custom integrations",
    "Team collaboration",
  ],
};

export const availablePlans = [
  {
    id: 1,
    name: "Starter",
    price: "₹1,999",
    billingCycle: "per month",
    yearlyPrice: "Save 17% — ₹19,999 / year",
    features: ["Up to 50 customers", "Basic analytics", "Email support", "Standard integrations"],
    isCurrent: false,
    highlight: false,
  },
  {
    id: 2,
    name: "Premium",
    price: "₹4,999",
    billingCycle: "per month",
    yearlyPrice: "Save 17% — ₹49,999 / year",
    features: ["Unlimited customers", "Advanced analytics", "Priority support", "Custom integrations", "Team collaboration"],
    isCurrent: true,
    highlight: true,
  },
  {
    id: 3,
    name: "Pro",
    price: "₹9,999",
    billingCycle: "per month",
    yearlyPrice: "Save 17% — ₹99,999 / year",
    features: ["Everything in Premium", "Dedicated account manager", "SLA guarantee", "White-label option", "API access"],
    isCurrent: false,
    highlight: false,
  },
];

export const customerInvoices = [
  { id: 1, number: "INV-0024", date: "12 Nov 2026", dueDate: "12 Nov 2026", amount: "₹4,999", status: "UNPAID" },
  { id: 2, number: "INV-0021", date: "12 Oct 2026", dueDate: "12 Oct 2026", amount: "₹4,999", status: "PAID"   },
  { id: 3, number: "INV-0018", date: "12 Sep 2026", dueDate: "12 Sep 2026", amount: "₹4,999", status: "PAID"   },
  { id: 4, number: "INV-0015", date: "12 Aug 2026", dueDate: "12 Aug 2026", amount: "₹4,999", status: "PAID"   },
  { id: 5, number: "INV-0012", date: "12 Jul 2026", dueDate: "12 Jul 2026", amount: "₹4,999", status: "PAID"   },
];

export const paymentHistory = [
  { id: 1, date: "12 Oct 2026", description: "Premium Monthly — October",   amount: "₹4,999", method: "UPI",  status: "SUCCESS" },
  { id: 2, date: "12 Sep 2026", description: "Premium Monthly — September", amount: "₹4,999", method: "UPI",  status: "SUCCESS" },
  { id: 3, date: "12 Aug 2026", description: "Premium Monthly — August",    amount: "₹4,999", method: "Card", status: "SUCCESS" },
  { id: 4, date: "12 Jul 2026", description: "Premium Monthly — July",      amount: "₹4,999", method: "Card", status: "SUCCESS" },
  { id: 5, date: "12 Jun 2026", description: "Premium Monthly — June",      amount: "₹4,999", method: "UPI",  status: "SUCCESS" },
];

