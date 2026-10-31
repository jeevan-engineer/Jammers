
import { useState } from "react";
import { Search, Plus, X, Trash2, Percent } from "lucide-react";

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
                <th>Description</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {filteredTaxes.length === 0 ? (
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
                    <td>{tax.rate}%</td>
                    <td>{tax.description || "—"}</td>
                    <td>
                      <span className="customer-status">{tax.status}</span>
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
                  value={form.rate}
                  onChange={(e) =>
                    setForm({ ...form, rate: e.target.value })
                  }
                  placeholder="Example: 18"
                />
              </label>

              <label>
                Description
                <input
                  value={form.description}
                  onChange={(e) =>
                    setForm({ ...form, description: e.target.value })
                  }
                  placeholder="Enter tax description"
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
                <button type="submit" className="primary-button">
                  Save Tax
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
