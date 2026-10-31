
import { useState } from "react";
import {
  Repeat,
  Plus,
  Search,
  X,
  Trash2,
  CalendarDays,
} from "lucide-react";

export default function Subscriptions() {
  const [subscriptions, setSubscriptions] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    customer: "",
    product: "",
    plan: "",
    startDate: "",
    nextBilling: "",
  });

  const filtered = subscriptions.filter((item) =>
    `${item.customer} ${item.product} ${item.plan}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  function addSubscription(event) {
    event.preventDefault();

    setSubscriptions((previous) => [
      ...previous,
      {
        ...form,
        id: Date.now(),
        status: "ACTIVE",
      },
    ]);

    setForm({
      customer: "",
      product: "",
      plan: "",
      startDate: "",
      nextBilling: "",
    });

    setShowForm(false);
  }

  function cancelSubscription(id) {
    setSubscriptions((previous) =>
      previous.map((item) =>
        item.id === id
          ? { ...item, status: "CANCELLED" }
          : item
      )
    );
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Management / Subscriptions</p>
          <h1>Subscriptions</h1>
          <p className="subtitle">
            Manage customer subscriptions and billing schedules.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={() => setShowForm(true)}
        >
          <Plus size={17} /> Create subscription
        </button>
      </div>

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Search subscriptions..."
          />
        </div>
        <span>{subscriptions.length} subscriptions</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>CUSTOMER</th>
                <th>PRODUCT</th>
                <th>PLAN</th>
                <th>START DATE</th>
                <th>NEXT BILLING</th>
                <th>STATUS</th>
                <th>ACTION</th>
              </tr>
            </thead>

            <tbody>
              {filtered.length === 0 ? (
                <tr>
                  <td colSpan="7" className="table-empty">
                    <Repeat size={28} />
                    <strong>No subscriptions found</strong>
                    <p>Create your first subscription.</p>
                  </td>
                </tr>
              ) : (
                filtered.map((item) => (
                  <tr key={item.id}>
                    <td><strong>{item.customer}</strong></td>
                    <td>{item.product}</td>
                    <td>{item.plan}</td>
                    <td>{item.startDate}</td>
                    <td>{item.nextBilling}</td>
                    <td>
                      <span
                        className={
                          item.status === "ACTIVE"
                            ? "customer-status"
                            : "subscription-cancelled"
                        }
                      >
                        {item.status}
                      </span>
                    </td>
                    <td>
                      {item.status === "ACTIVE" && (
                        <button
                          className="icon-button"
                          title="Cancel subscription"
                          aria-label="Cancel subscription"
                          onClick={() => cancelSubscription(item.id)}
                        >
                          <Trash2 size={17} />
                        </button>
                      )}
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
                <h2>Create subscription</h2>
                <p>Enter the subscription details.</p>
              </div>

              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close form"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addSubscription}>
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
                Product
                <input
                  required
                  value={form.product}
                  onChange={(e) =>
                    setForm({ ...form, product: e.target.value })
                  }
                  placeholder="Enter product name"
                />
              </label>

              <label>
                Plan
                <input
                  required
                  value={form.plan}
                  onChange={(e) =>
                    setForm({ ...form, plan: e.target.value })
                  }
                  placeholder="Enter subscription plan"
                />
              </label>

              <label>
                Start date
                <input
                  required
                  type="date"
                  value={form.startDate}
                  onChange={(e) =>
                    setForm({ ...form, startDate: e.target.value })
                  }
                />
              </label>

              <label>
                Next billing date
                <input
                  required
                  type="date"
                  min={form.startDate || undefined}
                  value={form.nextBilling}
                  onChange={(e) =>
                    setForm({ ...form, nextBilling: e.target.value })
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
                  Create subscription
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}
