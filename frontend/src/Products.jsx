
import { useState } from "react";
import { Package, Plus, Search, X, Trash2 } from "lucide-react";

export default function Products() {
  const [products, setProducts] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    name: "",
    description: "",
    sku: "",
  });

  const filteredProducts = products.filter((product) =>
    `${product.name} ${product.description} ${product.sku}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  function addProduct(event) {
    event.preventDefault();

    setProducts((previous) => [
      ...previous,
      { ...form, id: Date.now() },
    ]);

    setForm({ name: "", description: "", sku: "" });
    setShowForm(false);
  }

  function deleteProduct(id) {
    setProducts((previous) =>
      previous.filter((product) => product.id !== id)
    );
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
                <th>SKU</th>
                <th>DESCRIPTION</th>
                <th>STATUS</th>
                <th>ACTION</th>
              </tr>
            </thead>

            <tbody>
              {filteredProducts.length === 0 ? (
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
                    <td>{product.sku || "—"}</td>
                    <td>{product.description || "—"}</td>
                    <td><span className="customer-status">Active</span></td>
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
                SKU
                <input
                  value={form.sku}
                  onChange={(event) =>
                    setForm({ ...form, sku: event.target.value })
                  }
                  placeholder="Enter product SKU"
                />
              </label>

              <label>
                Description
                <input
                  value={form.description}
                  onChange={(event) =>
                    setForm({ ...form, description: event.target.value })
                  }
                  placeholder="Enter product description"
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
                  Add product
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

