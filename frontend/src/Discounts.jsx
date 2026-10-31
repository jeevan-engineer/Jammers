
import { useState, useEffect, useCallback } from "react";
import { Search, Plus, X, Trash2, Tag } from "lucide-react";
import api from "./api/axios";

function Discounts() {
  const [discounts, setDiscounts] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    name: "",
    code: "",
    discountType: "PERCENTAGE",
    discountValue: "",
    startDate: "",
    endDate: "",
  });

  const fetchDiscounts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get("/discounts", { params: { activeOnly: false } });
      setDiscounts(response.data.content ?? []);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load discounts.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchDiscounts();
  }, [fetchDiscounts]);

  const addDiscount = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post("/discounts", {
        name: form.name,
        code: form.code,
        discountType: form.discountType,
        discountValue: Number(form.discountValue),
        startDate: form.startDate,
        endDate: form.endDate,
        active: true,
      });
      setForm({
        name: "",
        code: "",
        discountType: "PERCENTAGE",
        discountValue: "",
        startDate: "",
        endDate: "",
      });
      setShowForm(false);
      await fetchDiscounts();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save discount.");
    } finally {
      setSaving(false);
    }
  };

  const deleteDiscount = async (id) => {
    setError(null);
    try {
      await api.delete(`/discounts/${id}`);
      await fetchDiscounts();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to delete discount.");
    }
  };

  const filteredDiscounts = discounts.filter((item) =>
    `${item.name} ${item.code}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Configuration / Discounts</p>
          <h1>Discounts</h1>
          <p className="subtitle">Create and manage promotional discount codes.</p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={16} /> Add Discount
        </button>
      </div>

      {error && (
        <div className="table-empty" style={{ color: "var(--danger, #e53e3e)", marginBottom: "1rem" }}>
          {error}{" "}
          <button className="select-button" onClick={fetchDiscounts}>
            Retry
          </button>
        </div>
      )}

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            placeholder="Search discounts..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <span>{discounts.length} discounts</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Discount Name</th>
                <th>Coupon Code</th>
                <th>Type</th>
                <th>Value</th>
                <th>Valid Until</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="table-empty">Loading…</td>
                </tr>
              ) : filteredDiscounts.length === 0 ? (
                <tr>
                  <td colSpan="7" className="table-empty">
                    <Tag size={28} />
                    <strong>No discounts found</strong>
                    <p>Create your first discount to get started.</p>
                  </td>
                </tr>
              ) : (
                filteredDiscounts.map((item) => (
                  <tr key={item.id}>
                    <td>{item.name}</td>
                    <td><strong>{item.code}</strong></td>
                    <td>{item.discountType === "PERCENTAGE" ? "Percentage" : "Fixed amount"}</td>
                    <td>
                      {item.discountType === "PERCENTAGE"
                        ? `${item.discountValue}%`
                        : `₹${item.discountValue}`}
                    </td>
                    <td>{item.endDate || "No expiry"}</td>
                    <td>
                      <span className="customer-status">
                        {item.active ? "ACTIVE" : "INACTIVE"}
                      </span>
                    </td>
                    <td>
                      <button
                        className="icon-button"
                        onClick={() => deleteDiscount(item.id)}
                        title="Delete"
                      >
                        <Trash2 size={16} />
                      </button>
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
                <h2>Create Discount</h2>
                <p>Enter the promotional offer details.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addDiscount}>
              <label>
                Discount Name
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Example: Festival Offer"
                />
              </label>

              <label>
                Coupon Code
                <input
                  required
                  value={form.code}
                  onChange={(e) =>
                    setForm({ ...form, code: e.target.value.toUpperCase() })
                  }
                  placeholder="Example: FESTIVE20"
                />
              </label>

              <label>
                Discount Type
                <select
                  value={form.discountType}
                  onChange={(e) => setForm({ ...form, discountType: e.target.value })}
                >
                  <option value="PERCENTAGE">Percentage (%)</option>
                  <option value="FIXED">Fixed Amount (₹)</option>
                </select>
              </label>

              <label>
                Discount Value
                <input
                  required
                  type="number"
                  min="0.01"
                  step="0.01"
                  max={form.discountType === "PERCENTAGE" ? "100" : undefined}
                  value={form.discountValue}
                  onChange={(e) => setForm({ ...form, discountValue: e.target.value })}
                  placeholder={form.discountType === "PERCENTAGE" ? "Example: 20" : "Example: 200"}
                />
              </label>

              <label>
                Start Date
                <input
                  required
                  type="date"
                  value={form.startDate}
                  onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                />
              </label>

              <label>
                End Date
                <input
                  required
                  type="date"
                  value={form.endDate}
                  onChange={(e) => setForm({ ...form, endDate: e.target.value })}
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
                  {saving ? "Saving…" : "Save Discount"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

export default Discounts;
