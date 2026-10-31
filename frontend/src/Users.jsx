
import { useState } from "react";
import { Search, Plus, X, Trash2, UserCog } from "lucide-react";

function Users() {
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState("");
  const [showForm, setShowForm] = useState(false);

  const [form, setForm] = useState({
    name: "",
    email: "",
    role: "STAFF",
  });

  const addUser = (e) => {
    e.preventDefault();

    const newUser = {
      id: Date.now(),
      ...form,
      status: "ACTIVE",
    };

    setUsers((prev) => [...prev, newUser]);
    setForm({ name: "", email: "", role: "STAFF" });
    setShowForm(false);
  };

  const deleteUser = (id) => {
    setUsers((prev) => prev.filter((user) => user.id !== id));
  };

  const filteredUsers = users.filter((user) =>
    `${user.name} ${user.email} ${user.role}`
      .toLowerCase()
      .includes(search.toLowerCase())
  );

  return (
    <section>
      <div className="page-heading">
        <div>
          <p className="breadcrumb">Configuration / Users</p>
          <h1>Users</h1>
          <p className="subtitle">Manage administrators and staff access.</p>
        </div>

        <button className="primary-button" onClick={() => setShowForm(true)}>
          <Plus size={16} /> Add User
        </button>
      </div>

      <div className="customer-toolbar">
        <div className="customer-search">
          <Search size={17} />
          <input
            placeholder="Search users..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />
        </div>
        <span>{users.length} users</span>
      </div>

      <div className="panel customer-table-panel">
        <div className="table-scroll">
          <table>
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Role</th>
                <th>Status</th>
                <th>Action</th>
              </tr>
            </thead>

            <tbody>
              {filteredUsers.length === 0 ? (
                <tr>
                  <td colSpan="5" className="table-empty">
                    <UserCog size={28} />
                    <strong>No users found</strong>
                    <p>Add your first team member to get started.</p>
                  </td>
                </tr>
              ) : (
                filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td>{user.name}</td>
                    <td>{user.email}</td>
                    <td>{user.role}</td>
                    <td>
                      <span className="customer-status">{user.status}</span>
                    </td>
                    <td>
                      <button
                        className="icon-button"
                        onClick={() => deleteUser(user.id)}
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
                <h2>Add User</h2>
                <p>Enter the team member's details.</p>
              </div>
              <button
                className="icon-button"
                onClick={() => setShowForm(false)}
                aria-label="Close"
              >
                <X size={19} />
              </button>
            </div>

            <form onSubmit={addUser}>
              <label>
                Full Name
                <input
                  required
                  value={form.name}
                  onChange={(e) => setForm({ ...form, name: e.target.value })}
                  placeholder="Enter full name"
                />
              </label>

              <label>
                Email Address
                <input
                  required
                  type="email"
                  value={form.email}
                  onChange={(e) => setForm({ ...form, email: e.target.value })}
                  placeholder="Enter email address"
                />
              </label>

              <label>
                Role
                <select
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                >
                  <option value="ADMIN">Admin</option>
                  <option value="MANAGER">Manager</option>
                  <option value="STAFF">Staff</option>
                </select>
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
                  Save User
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </section>
  );
}

export default Users;
