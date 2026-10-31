
import { useState } from "react";
import { Search, Plus, X, CreditCard } from "lucide-react";

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
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Management / Payments</p>
          <h1>Payments</h1>
          <p className="subtitle">Track and manage customer payments.</p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={16} /> Record Payment
        </button>
      </div>

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            placeholder="Search payments..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <span>{payments.length} payments</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
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
                  <td colSpan="6" className="table-empty">
                    <CreditCard size={28} />
                    <strong>No payments found</strong>
                    <p>Record your first payment to get started.</p>
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
      </div>

      {showForm && (
        <div className="customer-modal-overlay">
          <div className="customer-modal">
            <div className="customer-modal-header">
              <div>
                <h2>Record Payment</h2>
                <p>Enter payment details.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addPayment}>
              <label>
                Customer Name
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
                Invoice Number
                <input
                  required
                  value={form.invoice}
                  onChange={(e) =>
                    setForm({ ...form, invoice: e.target.value })
                  }
                  placeholder="Example: INV-0001"
                />
              </label>

              <label>
                Amount (₹)
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
              </label>

              <label>
                Payment Method
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
              </label>

              <label>
                Payment Date
                <input
                  required
                  type="date"
                  value={form.paymentDate}
                  onChange={(e) =>
                    setForm({ ...form, paymentDate: e.target.value })
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
                  Save Payment
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

export default Payments;
