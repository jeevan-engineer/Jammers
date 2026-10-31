
import { useState } from "react";
import { Search, Plus, X } from "lucide-react";

function Payments() {
  const [payments, setPayments] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    customer: "",
    invoice: "",
    amount: "",
    paymentMethod: "UPI",
    paymentDate: "",
  });

  const addPayment = (e) => {
    e.preventDefault();

    const newPayment = {
      id: Date.now(),
      ...form,
      status: "SUCCESS",
    };

    setPayments((prev) => [...prev, newPayment]);
    setForm({
      customer: "",
      invoice: "",
      amount: "",
      paymentMethod: "UPI",
      paymentDate: "",
    });
    setShowForm(false);
  };

  const filteredPayments = payments.filter((payment) =>
    `${payment.customer} ${payment.invoice} ${payment.paymentMethod}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  const formatAmount = (amount) =>
    new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
    }).format(Number(amount));

  return (
    <div className="module-page">
      <div className="module-header">
        <div>
          <h2>Payments</h2>
          <p>Track and manage customer payments.</p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={16} /> Record Payment
        </button>
      </div>

      <div className="module-toolbar">
        <div className="search-box">
          <Search size={17} />
          <input
            placeholder="Search payments..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
      </div>

      <div className="table-container">
        <table>
          <thead>
            <tr>
              <th>Customer</th>
              <th>Invoice</th>
              <th>Payment Method</th>
              <th>Payment Date</th>
              <th>Amount</th>
              <th>Status</th>
            </tr>
          </thead>

          <tbody>
            {filteredPayments.length === 0 ? (
              <tr>
                <td colSpan="6" className="empty-table">
                  No payments found. Record your first payment.
                </td>
              </tr>
            ) : (
              filteredPayments.map((payment) => (
                <tr key={payment.id}>
                  <td>{payment.customer}</td>
                  <td>{payment.invoice}</td>
                  <td>{payment.paymentMethod}</td>
                  <td>{payment.paymentDate}</td>
                  <td>{formatAmount(payment.amount)}</td>
                  <td>
                    <span className="payment-success">
                      {payment.status}
                    </span>
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
                <h3>Record Payment</h3>
                <p>Enter payment details.</p>
              </div>
              <button
                className="close-button"
                onClick={() => setShowForm(false)}
              >
                <X size={20} />
              </button>
            </div>

            <form onSubmit={addPayment}>
              <label>Customer Name</label>
              <input
                required
                value={form.customer}
                onChange={(e) =>
                  setForm({ ...form, customer: e.target.value })
                }
                placeholder="Enter customer name"
              />

              <label>Invoice Number</label>
              <input
                required
                value={form.invoice}
                onChange={(e) =>
                  setForm({ ...form, invoice: e.target.value })
                }
                placeholder="Example: INV-0001"
              />

              <label>Amount (₹)</label>
              <input
                required
                type="number"
                min="0.01"
                step="0.01"
                value={form.amount}
                onChange={(e) =>
                  setForm({ ...form, amount: e.target.value })
                }
                placeholder="Enter amount"
              />

              <label>Payment Method</label>
              <select
                value={form.paymentMethod}
                onChange={(e) =>
                  setForm({ ...form, paymentMethod: e.target.value })
                }
              >
                <option value="UPI">UPI</option>
                <option value="CARD">Card</option>
                <option value="BANK_TRANSFER">Bank Transfer</option>
                <option value="CASH">Cash</option>
              </select>

              <label>Payment Date</label>
              <input
                required
                type="date"
                value={form.paymentDate}
                onChange={(e) =>
                  setForm({ ...form, paymentDate: e.target.value })
                }
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
                  Save Payment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

export default Payments;
