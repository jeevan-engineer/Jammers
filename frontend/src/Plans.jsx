
import { useState } from "react";
import {
  Layers,
  Plus,
  Search,
  X,
  Trash2,
  IndianRupee,
} from "lucide-react";

export default function Plans() {
  const [plans, setPlans] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    name: "",
    product: "",
    price: "",
    currency: "INR",
    interval: "MONTHLY",
    trialDays: "0",
  });

  const filteredPlans = plans.filter((plan) =>
    `${plan.name} ${plan.product}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  function addPlan(event) {
    event.preventDefault();

    setPlans((previous) => [
      ...previous,
      { ...form, id: Date.now() },
    ]);

    setForm({
      name: "",
      product: "",
      price: "",
      currency: "INR",
      interval: "MONTHLY",
      trialDays: "0",
    });

    setShowForm(false);
  }

  function deletePlan(id) {
    setPlans((previous) =>
      previous.filter((plan) => plan.id !== id)
    );
  }

  function formatPrice(plan) {
    return new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: plan.currency,
    }).format(Number(plan.price));
  }

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Management / Plans</p>
          <h1>Subscription Plans</h1>
          <p className="subtitle">
            Configure pricing and billing intervals for your products.
          </p>
        </div>

        <button
          className="primary-button"
          onClick={() => setShowForm(true)}
        >
          <Plus size={17} /> Create plan
        </button>
      </div>

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Search plans..."
          />
        </div>
        <span>{plans.length} plans</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>PLAN NAME</th>
                <th>PRODUCT</th>
                <th>PRICE</th>
                <th>BILLING</th>
                <th>TRIAL</th>
                <th>ACTION</th>
              </tr>
            </thead>

            <tbody>
              {filteredPlans.length === 0 ? (
                <tr>
                  <td colSpan="6" className="table-empty">
                    <Layers size={28} />
                    <strong>No plans found</strong>
                    <p>Create your first subscription plan.</p>
                  </td>
                </tr>
              ) : (
                filteredPlans.map((plan) => (
                  <tr key={plan.id}>
                    <td><strong>{plan.name}</strong></td>
                    <td>{plan.product}</td>
                    <td>{formatPrice(plan)}</td>
                    <td>{plan.interval}</td>
                    <td>{plan.trialDays} days</td>
                    <td>
                      <button
                        className="icon-button"
                        onClick={() => deletePlan(plan.id)}
                        aria-label={`Delete ${plan.name}`}
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
                <h2>Create subscription plan</h2>
                <p>Configure your plan details and pricing.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close form"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addPlan}>
              <label>
                Plan name
                <input
                  required
                  value={form.name}
                  onChange={(e) =>
                    setForm({ ...form, name: e.target.value })
                  }
                  placeholder="e.g. Premium Monthly"
                />
              </label>

              <label>
                Product name
                <input
                  required
                  value={form.product}
                  onChange={(e) =>
                    setForm({ ...form, product: e.target.value })
                  }
                  placeholder="Enter associated product"
                />
              </label>

              <label>
                Price
                <input
                  required
                  type="number"
                  min="0"
                  step="0.01"
                  value={form.price}
                  onChange={(e) =>
                    setForm({ ...form, price: e.target.value })
                  }
                  placeholder="Enter plan price"
                />
              </label>

              <label>
                Currency
                <select
                  value={form.currency}
                  onChange={(e) =>
                    setForm({ ...form, currency: e.target.value })
                  }
                >
                  <option value="INR">INR - Indian Rupee</option>
                  <option value="USD">USD - US Dollar</option>
                  <option value="EUR">EUR - Euro</option>
                  <option value="GBP">GBP - British Pound</option>
                </select>
              </label>

              <label>
                Billing interval
                <select
                  value={form.interval}
                  onChange={(e) =>
                    setForm({ ...form, interval: e.target.value })
                  }
                >
                  <option value="DAILY">Daily</option>
                  <option value="WEEKLY">Weekly</option>
                  <option value="MONTHLY">Monthly</option>
                  <option value="QUARTERLY">Quarterly</option>
                  <option value="YEARLY">Yearly</option>
                </select>
              </label>

              <label>
                Free trial (days)
                <input
                  type="number"
                  min="0"
                  value={form.trialDays}
                  onChange={(e) =>
                    setForm({ ...form, trialDays: e.target.value })
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
                  Create plan
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

