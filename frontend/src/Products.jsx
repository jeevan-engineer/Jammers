
import { useState, useEffect, useCallback } from "react";
import { Package, Plus, Search, X, Trash2 } from "lucide-react";
import api from "./api/axios";

export default function Products() {
  const [products, setProducts] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [saving, setSaving] = useState(false);
  const [form, setForm] = useState({
    name: "",
    type: "SERVICE",
    salesPrice: "",
    costPrice: "",
    notes: "",
    active: true,
  });

  const fetchProducts = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const response = await api.get("/products");
      setProducts(response.data.content ?? []);
    } catch (err) {
      setError(err.response?.data?.message || "Failed to load products.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchProducts();
  }, [fetchProducts]);

  const filteredProducts = products.filter((product) =>
    `${product.name} ${product.type} ${product.notes ?? ""}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  async function addProduct(event) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    try {
      await api.post("/products", {
        name: form.name,
        type: form.type,
        salesPrice: Number(form.salesPrice),
        costPrice: Number(form.costPrice),
        notes: form.notes,
        active: true,
      });
      setForm({ name: "", type: "SERVICE", salesPrice: "", costPrice: "", notes: "", active: true });
      setShowForm(false);
      await fetchProducts();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to save product.");
    } finally {
      setSaving(false);
    }
  }

  async function deleteProduct(id) {
    setError(null);
    try {
      await api.delete(`/products/${id}`);
      await fetchProducts();
    } catch (err) {
      setError(err.response?.data?.message || "Failed to delete product.");
    }
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Management / Products</p>
          <h1>Products</h1>
          <p className="subtitle">
            Manage the products offered to your customers.
          </p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={17} /> Add product
        </button>
      </div>

      {error && (
        <div className="table-empty" style={{ color: "var(--danger, #e53e3e)", marginBottom: "1rem" }}>
          {error}{" "}
          <button className="select-button" onClick={fetchProducts}>
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
            placeholder="Search products..."
          />
        </div>
        <span>{products.length} products</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>PRODUCT</th>
                <th>TYPE</th>
                <th>PRICE</th>
                <th>STATUS</th>
                <th>ACTION</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="5" className="table-empty">Loading…</td>
                </tr>
              ) : filteredProducts.length === 0 ? (
                <tr>
                  <td colSpan="5" className="table-empty">
                    <Package size={28} />
                    <strong>No products found</strong>
                    <p>Add your first product to get started.</p>
                  </td>
                </tr>
              ) : (
                filteredProducts.map((product) => (
                  <tr key={product.id}>
                    <td><strong>{product.name}</strong></td>
                    <td>{product.type}</td>
                    <td>
                      {product.salesPrice != null
                        ? new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(product.salesPrice)
                        : "—"}
                    </td>
                    <td>
                      <span className="customer-status">
                        {product.active ? "Active" : "Inactive"}
                      </span>
                    </td>
                    <td>
                      <button
                        className="icon-button"
                        onClick={() => deleteProduct(product.id)}
                        aria-label={`Delete ${product.name}`}
                        title="Delete product"
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
                <h2>Add product</h2>
                <p>Enter the product information.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close form"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addProduct}>
              <label>
                Product name
                <input
                  required
                  value={form.name}
                  onChange={(event) =>
                    setForm({ ...form, name: event.target.value })
                  }
                  placeholder="Enter product name"
                />
              </label>

              <label>
                Type
                <select
                  value={form.type}
                  onChange={(event) =>
                    setForm({ ...form, type: event.target.value })
                  }
                >
                  <option value="SERVICE">Service</option>
                  <option value="CONSUMABLE">Consumable</option>
                  <option value="STORABLE">Storable</option>
                </select>
              </label>

              <label>
                Sales price
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.salesPrice}
                  onChange={(event) =>
                    setForm({ ...form, salesPrice: event.target.value })
                  }
                  placeholder="Enter sales price"
                />
              </label>

              <label>
                Cost price
                <input
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.costPrice}
                  onChange={(event) =>
                    setForm({ ...form, costPrice: event.target.value })
                  }
                  placeholder="Enter cost price"
                />
              </label>

              <label>
                Notes
                <textarea
                  value={form.notes}
                  onChange={(event) =>
                    setForm({ ...form, notes: event.target.value })
                  }
                  placeholder="Enter product notes"
                  rows={3}
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
                  {saving ? "Saving…" : "Add product"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
