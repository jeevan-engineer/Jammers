
import { useState } from "react";
import { Search, Plus, X, Trash2 } from "lucide-react";

function Discounts() {
  const [discounts, setDiscounts] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    name: "",
    code: "",
    type: "PERCENTAGE",
    value: "",
    validUntil: "",
  });

  const addDiscount = (e) => {
    e.preventDefault();

    const newDiscount = {
      id: Date.now(),
      ...form,
      status: "ACTIVE",
    };

    setDiscounts((prev) => [...prev, newDiscount]);
    setForm({
      name: "",
      code: "",
      type: "PERCENTAGE",
      value: "",
      validUntil: "",
    });
    setShowForm(false);
  };

  const deleteDiscount = (id) => {
    setDiscounts((prev) => prev.filter((item) => item.id !== id));
  };

  const filteredDiscounts = discounts.filter((item) =>
    `${item.name} ${item.code}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="module-page">
      <div className="module-header">
        <div>
          <h2>Discounts</h2>
          <p>Create and manage promotional discount codes.</p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={16} /> Add Discount
        </button>
      </div>

      <div className="module-toolbar">
        <div className="search-box">
          <Search size={17} />
          <input
            placeholder="Search discounts..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="table-container">
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
            {filteredDiscounts.length === 0 ? (
              <tr>
                <td colSpan="7" className="empty-table">
                  No discounts found. Create your first discount.
                </td>
              </tr>
            ) : (
              filteredDiscounts.map((item) => (
                <tr key={item.id}>
                  <td>{item.name}</td>
                  <td><strong>{item.code}</strong></td>
                  <td>{item.type === "PERCENTAGE" ? "Percentage" : "Fixed amount"}</td>
                  <td>
                    {item.type === "PERCENTAGE" ? `${item.value}%` : `₹${item.value}`}
                  </td>
                  <td>{item.validUntil || "No expiry"}</td>
                  <td>
                    <span className="customer-status">{item.status}</span>
                  </td>
                  <td>
                    <button
                      className="delete-button"
                      onClick={() => deleteDiscount(item.id)}
                      title="Delete discount"
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
                <h3>Create Discount</h3>
                <p>Enter the promotional offer details.</p>
              </div>
              <button
                className="close-button"
                onClick={() => setShowForm(false)}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={addDiscount}>
              <label>Discount Name</label>
              <input
                required
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="Example: Festival Offer"
              />

              <label>Coupon Code</label>
              <input
                required
                value={form.code}
                onChange={(e) =>
                  setForm({ ...form, code: e.target.value.toUpperCase() })
                }
                placeholder="Example: FESTIVE20"
              />

              <label>Discount Type</label>
              <select
                value={form.type}
                onChange={(e) => setForm({ ...form, type: e.target.value })}
              >
                <option value="PERCENTAGE">Percentage (%)</option>
                <option value="FIXED">Fixed Amount (₹)</option>
              </select>

              <label>Discount Value</label>
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
                max={form.type === "PERCENTAGE" ? "100" : undefined}
                value={form.value}
                onChange={(e) => setForm({ ...form, value: e.target.value })}
                placeholder={form.type === "PERCENTAGE" ? "Example: 20" : "Example: 200"}
              />

              <label>Valid Until</label>
              <input
                type="date"
                value={form.validUntil}
                onChange={(e) => setForm({ ...form, validUntil: e.target.value })}
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
                  Save Discount
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Discounts;
