
import { useState, useEffect, useCallback } from "react";
import { Search, Plus, Users, X } from "lucide-react";
import api from "./api/axios";

export default function Customers() {
  const [customers, setCustomers] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: "",
    email: "",
    phone: "",
    companyName: "",
    address: "",
  });

  const fetchCustomers = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get("/customers");
      setCustomers(response.data.content ?? []);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load customers.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchCustomers();
  }, [fetchCustomers]);

  const filteredCustomers = customers.filter((customer) =>
    `${customer.name} ${customer.email} ${customer.phone ?? ""} ${customer.companyName ?? ""}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  async function addCustomer(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post("/customers", {
        name: form.name,
        email: form.email,
        phone: form.phone,
        companyName: form.companyName,
        address: form.address,
      });
      setForm({ name: "", email: "", phone: "", companyName: "", address: "" });
      setShowForm(false);
      await fetchCustomers();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save customer.");
    } finally {
      setSaving(false);
    }
  }

  return (
    <section className="customers-page">
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Management / Customers</p>
          <h1>Customers</h1>
          <p className="subtitle">
            Manage your customer information and accounts.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={() => setShowForm(true)}
        >
          <Plus size={17} /> Add customer
        </button>
      </div>

      {error && (
        <div className="table-empty" style={{ color: "var(--danger, #e53e3e)", marginBottom: "1rem" }}>
          {error}{" "}
          <button className="select-button" onClick={fetchCustomers}>
            Retry
          </button>
        </div>
      )}

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search customers..."
          />
        </div>
        <span>{customers.length} customers</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>CUSTOMER</th>
                <th>EMAIL</th>
                <th>PHONE</th>
                <th>STATUS</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="4" className="table-empty">Loading…</td>
                </tr>
              ) : filteredCustomers.length === 0 ? (
                <tr>
                  <td colSpan="4" className="table-empty">
                    <Users size={28} />
                    <strong>No customers found</strong>
                    <p>Add a customer to get started.</p>
                  </td>
                </tr>
              ) : (
                filteredCustomers.map((customer) => (
                  <tr key={customer.id}>
                    <td>{customer.name}</td>
                    <td>{customer.email}</td>
                    <td>{customer.phone || "—"}</td>
                    <td>
                      <span className="customer-status">
                        {customer.active ? "Active" : "Inactive"}
                      </span>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showForm && (
        <div className="customer-modal-overlay">
          <div className="customer-modal">
            <div className="customer-modal-header">
              <div>
                <h2>Add customer</h2>
                <p>Enter the customer's contact information.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close form"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addCustomer}>
              <label>
                Customer name
                <input
                  required
                  value={form.name}
                  onChange={(event) =>
                    setForm({ ...form, name: event.target.value })
                  }
                  placeholder="Enter customer name"
                />
              </label>

              <label>
                Email address
                <input
                  required
                  type="email"
                  value={form.email}
                  onChange={(event) =>
                    setForm({ ...form, email: event.target.value })
                  }
                  placeholder="Enter email address"
                />
              </label>

              <label>
                Phone number
                <input
                  value={form.phone}
                  onChange={(event) =>
                    setForm({ ...form, phone: event.target.value })
                  }
                  placeholder="Enter phone number"
                />
              </label>

              <label>
                Company name
                <input
                  value={form.companyName}
                  onChange={(event) =>
                    setForm({ ...form, companyName: event.target.value })
                  }
                  placeholder="Enter company name"
                />
              </label>

              <label>
                Address
                <input
                  value={form.address}
                  onChange={(event) =>
                    setForm({ ...form, address: event.target.value })
                  }
                  placeholder="Enter address"
                />
              </label>

              <div className="customer-modal-actions">
                <button
                  type="button"
                  className="select-button"
                  onClick={() => setShowForm(false)}
                >
                  Cancel
                </button>
                <button type="submit" className="primary-button" disabled={saving}>
                  {saving ? "Saving…" : "Add customer"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
