import { useEffect, useState } from 'react';
import { deleteUser, getUsers, updateUser, type User } from '../lib/api';
import {
  ChevronLeft,
  ChevronRight,
  Pencil,
  RefreshCw,
  Save,
  Search,
  Trash2,
  User as UserIcon,
  UserCheck,
  UserX,
  X,
} from 'lucide-react';
import styles from './TablePage.module.css';

export default function UsersPage() {
  const [users, setUsers] = useState<User[]>([]);
  const [count, setCount] = useState(0);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [editing, setEditing] = useState<User | null>(null);
  const [form, setForm] = useState({ full_name: '', admin_notes: '', is_disabled: false });
  const [saving, setSaving] = useState(false);
  const [banner, setBanner] = useState<string | null>(null);
  const PAGE_SIZE = 20;

  const load = () => {
    setLoading(true);
    getUsers(page, PAGE_SIZE, search)
      .then(({ data, count: total }) => {
        setUsers(data);
        setCount(total);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { setPage(0); }, [search]);
  useEffect(() => { load(); }, [page, search]);

  const openEdit = (user: User) => {
    setEditing(user);
    setForm({
      full_name: user.full_name ?? '',
      admin_notes: user.admin_notes ?? '',
      is_disabled: user.is_disabled,
    });
  };

  const handleSave = async () => {
    if (!editing) return;
    setSaving(true);
    try {
      await updateUser(editing.id, {
        full_name: form.full_name.trim() || null,
        admin_notes: form.admin_notes.trim() || null,
        is_disabled: form.is_disabled,
      });
      setBanner(`Updated ${editing.email}`);
      setEditing(null);
      load();
    } finally {
      setSaving(false);
    }
  };

  const handleToggleDisabled = async (user: User) => {
    const nextDisabled = !user.is_disabled;
    await updateUser(user.id, { is_disabled: nextDisabled });
    setBanner(nextDisabled ? `Disabled ${user.email}` : `Enabled ${user.email}`);
    load();
  };

  const handleDelete = async (id: string, email: string) => {
    if (!confirm(`Delete user ${email}? This removes the profile record and can hide their app data from admin views.`)) return;
    await deleteUser(id);
    setBanner(`Deleted ${email}`);
    load();
  };

  const start = count === 0 ? 0 : page * PAGE_SIZE + 1;
  const end = Math.min((page + 1) * PAGE_SIZE, count);

  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Users</h1>
          <p className={styles.subtitle}>{count.toLocaleString()} total users</p>
        </div>
        <button className={styles.btnRefresh} onClick={load}><RefreshCw size={14} />Refresh</button>
      </div>

      {banner && <div className={styles.setupNote}>{banner}</div>}

      <div className={styles.toolbar}>
        <div className={styles.searchWrap}>
          <Search size={15} className={styles.searchIcon} />
          <input
            className={styles.search}
            placeholder="Search by email or name..."
            value={search}
            onChange={event => setSearch(event.target.value)}
          />
        </div>
      </div>

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>User</th>
              <th>Email</th>
              <th>Status</th>
              <th>Admin Notes</th>
              <th>Updated</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className={styles.loadingCell}><div className={styles.spinner} /></td></tr>
            ) : users.length === 0 ? (
              <tr><td colSpan={6} className={styles.emptyCell}>No users found</td></tr>
            ) : users.map(user => (
              <tr key={user.id} className={styles.row}>
                <td>
                  <div className={styles.userCell}>
                    {user.avatar_url
                      ? <img src={user.avatar_url} className={styles.avatar} alt={user.full_name ?? 'User'} />
                      : <div className={styles.avatarFallback}><UserIcon size={14} /></div>
                    }
                    <div>
                      <div className={styles.name}>{user.full_name || <span className={styles.muted}>Unnamed user</span>}</div>
                      <div className={styles.monoMuted}>{user.id.slice(0, 8)}...</div>
                    </div>
                  </div>
                </td>
                <td className={styles.email}>{user.email}</td>
                <td>
                  <span className={`${styles.tag} ${user.is_disabled ? styles.tagRed : styles.tagGreen}`}>
                    {user.is_disabled ? 'Disabled' : 'Enabled'}
                  </span>
                </td>
                <td className={styles.noteCell}>{user.admin_notes || <span className={styles.muted}>None</span>}</td>
                <td className={styles.muted}>{new Date(user.updated_at).toLocaleDateString()}</td>
                <td>
                  <div className={styles.actionRow}>
                    <button className={styles.btnIcon} onClick={() => openEdit(user)} title="Edit user">
                      <Pencil size={13} />
                    </button>
                    <button
                      className={styles.btnIcon}
                      onClick={() => handleToggleDisabled(user)}
                      title={user.is_disabled ? 'Enable user' : 'Disable user'}
                    >
                      {user.is_disabled ? <UserCheck size={13} /> : <UserX size={13} />}
                    </button>
                    <button className={styles.btnDanger} onClick={() => handleDelete(user.id, user.email)} title="Delete user">
                      <Trash2 size={13} />
                    </button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className={styles.pagination}>
        <span className={styles.pageInfo}>Showing {start}-{end} of {count}</span>
        <div className={styles.pageButtons}>
          <button className={styles.pageBtn} disabled={page === 0} onClick={() => setPage(current => current - 1)}>
            <ChevronLeft size={16} />
          </button>
          <span className={styles.pageCurrent}>{page + 1}</span>
          <button className={styles.pageBtn} disabled={(page + 1) * PAGE_SIZE >= count} onClick={() => setPage(current => current + 1)}>
            <ChevronRight size={16} />
          </button>
        </div>
      </div>

      {editing && (
        <div className={styles.modalOverlay} onClick={() => setEditing(null)}>
          <div className={styles.modal} onClick={event => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle}>Manage User</h2>
              <button onClick={() => setEditing(null)} className={styles.btnDanger} style={{ width: 28, height: 28 }}>
                <X size={13} />
              </button>
            </div>

            <div className={styles.detailStack}>
              <div className={styles.infoLine}>
                <span>Email</span>
                <strong>{editing.email}</strong>
              </div>
              <div className={styles.infoLine}>
                <span>User ID</span>
                <strong>{editing.id}</strong>
              </div>
            </div>

            <div className={styles.field}>
              <label className={styles.label}>Full Name</label>
              <input
                className={styles.input}
                value={form.full_name}
                onChange={event => setForm(current => ({ ...current, full_name: event.target.value }))}
              />
            </div>

            <div className={styles.field}>
              <label className={styles.label}>Admin Notes</label>
              <textarea
                className={styles.textarea}
                value={form.admin_notes}
                onChange={event => setForm(current => ({ ...current, admin_notes: event.target.value }))}
                rows={5}
                placeholder="Private note for support or moderation..."
              />
            </div>

            <div className={styles.field}>
              <label className={styles.checkboxLabel}>
                <input
                  type="checkbox"
                  checked={form.is_disabled}
                  onChange={event => setForm(current => ({ ...current, is_disabled: event.target.checked }))}
                />
                Disable this user in Android app
              </label>
            </div>

            <div className={styles.modalActions}>
              <button className={styles.btnCancel} onClick={() => setEditing(null)}>Cancel</button>
              <button className={styles.btnPrimary} onClick={handleSave} disabled={saving}>
                <Save size={14} />{saving ? 'Saving...' : 'Save User'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
