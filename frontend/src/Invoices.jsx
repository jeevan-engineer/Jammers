
import { useState } from "react";
import {
  FileText,
  Plus,
  Search,
  X,
  Eye,
} from "lucide-react";

export default function Invoices() {
  const [invoices, setInvoices] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    customer: "",
    amount: "",
    issueDate: "",
    dueDate: "",
  });

  const filtered = invoices.filter((invoice) =>
    `${invoice.invoiceNumber} ${invoice.customer}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  function addInvoice(event) {
    event.preventDefault();

    const newInvoice = {
      ...form,
      id: Date.now(),
      invoiceNumber: `INV-${String(invoices.length + 1).padStart(4, "0")}`,
      status: "UNPAID",
    };

    setInvoices((previous) => [...previous, newInvoice]);
    setForm({ customer: "", amount: "", issueDate: "", dueDate: "" });
    setShowForm(false);
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Management / Invoices</p>
          <h1>Invoices</h1>
          <p className="subtitle">
            Create and manage customer invoices.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={() => setShowForm(true)}
        >
          <Plus size={17} /> Create invoice
        </button>
      </div>

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search invoices..."
          />
        </div>
        <span>{invoices.length} invoices</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>INVOICE</th>
                <th>CUSTOMER</th>
                <th>ISSUE DATE</th>
                <th>DUE DATE</th>
                <th>AMOUNT</th>
                <th>STATUS</th>
              </tr>
            </thead>

            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan="6" className="table-empty">
                    <FileText size={28} />
                    <strong>No invoices found</strong>
                    <p>Create your first invoice.</p>
                  </td>
                </tr>
              ) : (
                filtered.map((invoice) => (
                  <tr key={invoice.id}>
                    <td><strong>{invoice.invoiceNumber}</strong></td>
                    <td>{invoice.customer}</td>
                    <td>{invoice.issueDate}</td>
                    <td>{invoice.dueDate}</td>
                    <td>
                      {new Intl.NumberFormat("en-IN", {
                        style: "currency",
                        currency: "INR",
                      }).format(Number(invoice.amount))}
                    </td>
                    <td>
                      <span className="invoice-unpaid">
                        {invoice.status}
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
                <h2>Create invoice</h2>
                <p>Enter the invoice information.</p>
              </div>

              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close form"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addInvoice}>
              <label>
                Customer name
                <input
                  required
                  value={form.customer}
                  onChange={(e) =>
                    setForm({ ...form, customer: e.target.value })
                  }
                  placeholder="Enter customer name"
                />
              </label>

              <label>
                Amount (INR)
                <input
                  required
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={form.amount}
                  onChange={(e) =>
                    setForm({ ...form, amount: e.target.value })
                  }
                  placeholder="Enter invoice amount"
                />
              </label>

              <label>
                Issue date
                <input
                  required
                  type="date"
                  value={form.issueDate}
                  onChange={(e) =>
                    setForm({ ...form, issueDate: e.target.value })
                  }
                />
              </label>

              <label>
                Due date
                <input
                  required
                  type="date"
                  min={form.issueDate || undefined}
                  value={form.dueDate}
                  onChange={(e) =>
                    setForm({ ...form, dueDate: e.target.value })
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

                <button type="submit" className="primary-button">
                  Create invoice
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
