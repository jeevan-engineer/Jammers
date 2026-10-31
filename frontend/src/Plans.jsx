
import { useState, useEffect, useCallback } from "react";
import {
  Layers,
  Plus,
  Search,
  X,
  Trash2,
  IndianRupee,
} from "lucide-react";
import api from "./api/axios";

export default function Plans() {
  const [plans, setPlans] = useState([]);
  const [products, setProducts] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    name: "",
    productId: "",
    price: "",
    billingPeriod: "MONTHLY",
    minQuantity: "1",
  });

  const fetchPlans = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get("/plans");
      setPlans(response.data.content ?? []);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load plans.");
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchProducts = useCallback(async () => {
    try {
      const response = await api.get("/products");
      setProducts(response.data.content ?? []);
    } catch {
      // silently fail — products list is only used for the form select
    }
  }, []);

  useEffect(() => {
    fetchPlans();
    fetchProducts();
  }, [fetchPlans, fetchProducts]);

  const filteredPlans = plans.filter((plan) =>
    `${plan.name} ${plan.productName ?? ""}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  async function addPlan(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post("/plans", {
        productId: Number(form.productId),
        name: form.name,
        price: Number(form.price),
        billingPeriod: form.billingPeriod,
        minQuantity: Number(form.minQuantity),
        active: true,
      });
      setForm({ name: "", productId: "", price: "", billingPeriod: "MONTHLY", minQuantity: "1" });
      setShowForm(false);
      await fetchPlans();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save plan.");
    } finally {
      setSaving(false);
    }
  }

  async function deletePlan(id) {
    setError(null);
    try {
      await api.delete(`/plans/${id}`);
      await fetchPlans();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to delete plan.");
    }
  }

  function formatPrice(price) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
    }).format(Number(price));
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Management / Plans</p>
          <h1>Subscription Plans</h1>
          <p className="subtitle">
            Configure pricing and billing intervals for your products.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={() => setShowForm(true)}
        >
          <Plus size={17} /> Create plan
        </button>
      </div>

      {error && (
        <div className="table-empty" style={{ color: "var(--danger, #e53e3e)", marginBottom: "1rem" }}>
          {error}{" "}
          <button className="select-button" onClick={fetchPlans}>
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
            placeholder="Search plans..."
          />
        </div>
        <span>{plans.length} plans</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>PLAN NAME</th>
                <th>PRODUCT</th>
                <th>PRICE</th>
                <th>BILLING</th>
                <th>MIN QTY</th>
                <th>ACTION</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="6" className="table-empty">Loading…</td>
                </tr>
              ) : filteredPlans.length === 0 ? (
                <tr>
                  <td colSpan="6" className="table-empty">
                    <Layers size={28} />
                    <strong>No plans found</strong>
                    <p>Create your first subscription plan.</p>
                  </td>
                </tr>
              ) : (
                filteredPlans.map((plan) => (
                  <tr key={plan.id}>
                    <td><strong>{plan.name}</strong></td>
                    <td>{plan.productName ?? "—"}</td>
                    <td>{formatPrice(plan.price)}</td>
                    <td>{plan.billingPeriod}</td>
                    <td>{plan.minQuantity}</td>
                    <td>
                      <button
                        className="icon-button"
                        onClick={() => deletePlan(plan.id)}
                        aria-label={`Delete ${plan.name}`}
                      >
                        <Trash2 size={17} />
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
                <h2>Create subscription plan</h2>
                <p>Configure your plan details and pricing.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close form"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addPlan}>
              <label>
                Plan name
                <input
                  required
                  value={form.name}
                  onChange={(e) =>
                    setForm({ ...form, name: e.target.value })
                  }
                  placeholder="e.g. Premium Monthly"
                />
              </label>

              <label>
                Product
                <select
                  required
                  value={form.productId}
                  onChange={(e) =>
                    setForm({ ...form, productId: e.target.value })
                  }
                >
                  <option value="">Select a product</option>
                  {products.map((p) => (
                    <option key={p.id} value={p.id}>
                      {p.name}
                    </option>
                  ))}
                </select>
              </label>

              <label>
                Price
                <input
                  required
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.price}
                  onChange={(e) =>
                    setForm({ ...form, price: e.target.value })
                  }
                  placeholder="Enter plan price"
                />
              </label>

              <label>
                Billing interval
                <select
                  value={form.billingPeriod}
                  onChange={(e) =>
                    setForm({ ...form, billingPeriod: e.target.value })
                  }
                >
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </label>

              <label>
                Minimum quantity
                <input
                  type="number"
                  min="1"
                  value={form.minQuantity}
                  onChange={(e) =>
                    setForm({ ...form, minQuantity: e.target.value })
                  }
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
                  {saving ? "Saving…" : "Create plan"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
