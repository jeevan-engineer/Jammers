
import { useState } from "react";
import { Search, Plus, X, Trash2 } from "lucide-react";

function Taxes() {
  const [taxes, setTaxes] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    name: "",
    rate: "",
    description: "",
  });

  const addTax = (e) => {
    e.preventDefault();

    const newTax = {
      id: Date.now(),
      ...form,
      status: "ACTIVE",
    };

    setTaxes((prev) => [...prev, newTax]);
    setForm({ name: "", rate: "", description: "" });
    setShowForm(false);
  };

  const deleteTax = (id) => {
    setTaxes((prev) => prev.filter((tax) => tax.id !== id));
  };

  const filteredTaxes = taxes.filter((tax) =>
    `${tax.name} ${tax.description}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  return (
    <div className="module-page">
      <div className="module-header">
        <div>
          <h2>Taxes</h2>
          <p>Manage tax rates applied to your subscription plans.</p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={16} /> Add Tax
        </button>
      </div>

      <div className="module-toolbar">
        <div className="search-box">
          <Search size={17} />
          <input
            placeholder="Search taxes..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Tax Name</th>
              <th>Rate</th>
              <th>Description</th>
              <th>Status</th>
              <th>Action</th>
            </tr>
          </thead>

          <tbody>
            {filteredTaxes.length === 0 ? (
              <tr>
                <td colSpan="5" className="empty-table">
                  No taxes found. Add your first tax rate.
                </td>
              </tr>
            ) : (
              filteredTaxes.map((tax) => (
                <tr key={tax.id}>
                  <td>{tax.name}</td>
                  <td>{tax.rate}%</td>
                  <td>{tax.description || "—"}</td>
                  <td>
                    <span className="customer-status">{tax.status}</span>
                  </td>
                  <td>
                    <button
                      className="delete-button"
                      onClick={() => deleteTax(tax.id)}
                      title="Delete tax"
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

      {showForm && (
        <div className="modal-overlay">
          <div className="customer-modal">
            <div className="modal-header">
              <div>
                <h3>Add Tax Rate</h3>
                <p>Enter the tax details.</p>
              </div>
              <button
                className="close-button"
                onClick={() => setShowForm(false)}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={addTax}>
              <label>Tax Name</label>
              <input
                required
                value={form.name}
                onChange={(e) =>
                  setForm({ ...form, name: e.target.value })
                }
                placeholder="Example: GST"
              />

              <label>Tax Rate (%)</label>
              <input
                required
                type="number"
                min="0"
                max="100"
                step="0.01"
                value={form.rate}
                onChange={(e) =>
                  setForm({ ...form, rate: e.target.value })
                }
                placeholder="Example: 18"
              />

              <label>Description</label>
              <input
                value={form.description}
                onChange={(e) =>
                  setForm({ ...form, description: e.target.value })
                }
                placeholder="Enter tax description"
              />

              <div className="modal-actions">
                <button
                  type="button"
                  className="cancel-button"
                  onClick={() => setShowForm(false)}
                >
                  Cancel
                </button>
                <button type="submit" className="primary-button">
                  Save Tax
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Taxes;

