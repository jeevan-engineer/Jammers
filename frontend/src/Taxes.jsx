
import { useState, useEffect, useCallback } from "react";
import { Search, Plus, X, Trash2, Percent } from "lucide-react";
import api from "./api/axios";

function Taxes() {
  const [taxes, setTaxes] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);

  const [form, setForm] = useState({
    name: "",
    percentage: "",
    applicability: "SERVICE",
    active: true,
  });

  const fetchTaxes = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get("/taxes", { params: { activeOnly: false } });
      setTaxes(Array.isArray(response.data) ? response.data : (response.data.content ?? []));
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load taxes.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTaxes();
  }, [fetchTaxes]);

  const addTax = async (e) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post("/taxes", {
        name: form.name,
        percentage: Number(form.percentage),
        applicability: form.applicability,
        active: true,
      });
      setForm({ name: "", percentage: "", applicability: "SERVICE", active: true });
      setShowForm(false);
      await fetchTaxes();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save tax.");
    } finally {
      setSaving(false);
    }
  };

  const deleteTax = async (id) => {
    setError(null);
    try {
      await api.delete(`/taxes/${id}`);
      await fetchTaxes();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to delete tax.");
    }
  };

  const filteredTaxes = taxes.filter((tax) =>
    `${tax.name} ${tax.applicability ?? ""}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Configuration / Taxes</p>
          <h1>Taxes</h1>
          <p className="subtitle">Manage tax rates applied to your subscription plans.</p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={16} /> Add Tax
        </button>
      </div>

      {error && (
        <div className="table-empty" style={{ color: "var(--danger, #e53e3e)", marginBottom: "1rem" }}>
          {error}{" "}
          <button className="select-button" onClick={fetchTaxes}>
            Retry
          </button>
        </div>
      )}

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            placeholder="Search taxes..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <span>{taxes.length} taxes</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Tax Name</th>
                <th>Rate</th>
                <th>Applicability</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="5" className="table-empty">Loading…</td>
                </tr>
              ) : filteredTaxes.length === 0 ? (
                <tr>
                  <td colSpan="5" className="table-empty">
                    <Percent size={28} />
                    <strong>No taxes found</strong>
                    <p>Add your first tax rate to get started.</p>
                  </td>
                </tr>
              ) : (
                filteredTaxes.map((tax) => (
                  <tr key={tax.id}>
                    <td>{tax.name}</td>
                    <td>{tax.percentage}%</td>
                    <td>{tax.applicability || "—"}</td>
                    <td>
                      <span className="customer-status">
                        {tax.active ? "ACTIVE" : "INACTIVE"}
                      </span>
                    </td>
                    <td>
                      <button
                        className="icon-button"
                        onClick={() => deleteTax(tax.id)}
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
                <h2>Add Tax Rate</h2>
                <p>Enter the tax details.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addTax}>
              <label>
                Tax Name
                <input
                  required
                  value={form.name}
                  onChange={(e) =>
                    setForm({ ...form, name: e.target.value })
                  }
                  placeholder="Example: GST"
                />
              </label>

              <label>
                Tax Rate (%)
                <input
                  required
                  type="number"
                  min="0"
                  max="100"
                  step="0.01"
                  value={form.percentage}
                  onChange={(e) =>
                    setForm({ ...form, percentage: e.target.value })
                  }
                  placeholder="Example: 18"
                />
              </label>

              <label>
                Applicability
                <select
                  value={form.applicability}
                  onChange={(e) =>
                    setForm({ ...form, applicability: e.target.value })
                  }
                >
                  <option value="SERVICE">Service</option>
                  <option value="CONSUMABLE">Consumable</option>
                  <option value="STORABLE">Storable</option>
                </select>
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
                  {saving ? "Saving…" : "Save Tax"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

export default Taxes;
