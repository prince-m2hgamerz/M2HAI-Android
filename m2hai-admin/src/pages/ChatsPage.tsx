import { useEffect, useState } from 'react';
import { deleteChat, getChatMessages, getChats, updateChat, type Chat, type Message } from '../lib/api';
import {
  Archive,
  ArchiveRestore,
  ChevronLeft,
  ChevronRight,
  Eye,
  MessageSquare,
  Pencil,
  Pin,
  PinOff,
  RefreshCw,
  Save,
  Search,
  Trash2,
  X,
} from 'lucide-react';
import styles from './TablePage.module.css';

export default function ChatsPage() {
  const [chats, setChats] = useState<Chat[]>([]);
  const [count, setCount] = useState(0);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [viewChat, setViewChat] = useState<{ chat: Chat; messages: Message[] } | null>(null);
  const [editChat, setEditChat] = useState<Chat | null>(null);
  const [editTitle, setEditTitle] = useState('');
  const [viewLoading, setViewLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [banner, setBanner] = useState<string | null>(null);
  const PAGE_SIZE = 20;

  const load = () => {
    setLoading(true);
    getChats(page, PAGE_SIZE, search)
      .then(({ data, count: total }) => {
        setChats(data);
        setCount(total);
      })
      .finally(() => setLoading(false));
  };

  useEffect(() => { setPage(0); }, [search]);
  useEffect(() => { load(); }, [page, search]);

  const handleDelete = async (id: string) => {
    if (!confirm('Delete this chat and all its messages?')) return;
    await deleteChat(id);
    setBanner('Chat deleted');
    load();
  };

  const handleView = async (chat: Chat) => {
    setViewLoading(true);
    try {
      const messages = await getChatMessages(chat.id);
      setViewChat({ chat, messages });
    } finally {
      setViewLoading(false);
    }
  };

  const openEdit = (chat: Chat) => {
    setEditChat(chat);
    setEditTitle(chat.title);
  };

  const handleSaveTitle = async () => {
    if (!editChat || !editTitle.trim()) return;
    setSaving(true);
    try {
      await updateChat(editChat.id, { title: editTitle.trim() });
      setBanner('Chat title updated');
      setEditChat(null);
      load();
    } finally {
      setSaving(false);
    }
  };

  const togglePinned = async (chat: Chat) => {
    await updateChat(chat.id, { is_pinned: !chat.is_pinned });
    setBanner(chat.is_pinned ? 'Chat unpinned' : 'Chat pinned');
    load();
  };

  const toggleArchived = async (chat: Chat) => {
    await updateChat(chat.id, { is_archived: !chat.is_archived });
    setBanner(chat.is_archived ? 'Chat restored' : 'Chat archived');
    load();
  };

  const start = count === 0 ? 0 : page * PAGE_SIZE + 1;
  const end = Math.min((page + 1) * PAGE_SIZE, count);

  return (
    <div className={styles.root}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>Chats</h1>
          <p className={styles.subtitle}>{count.toLocaleString()} total conversations</p>
        </div>
        <button className={styles.btnRefresh} onClick={load}><RefreshCw size={14} />Refresh</button>
      </div>

      {banner && <div className={styles.setupNote}>{banner}</div>}

      <div className={styles.toolbar}>
        <div className={styles.searchWrap}>
          <Search size={15} className={styles.searchIcon} />
          <input
            className={styles.search}
            placeholder="Search by title..."
            value={search}
            onChange={event => setSearch(event.target.value)}
          />
        </div>
      </div>

      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>Title</th>
              <th>Model</th>
              <th>User ID</th>
              <th>Status</th>
              <th>Created</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={6} className={styles.loadingCell}><div className={styles.spinner} /></td></tr>
            ) : chats.length === 0 ? (
              <tr><td colSpan={6} className={styles.emptyCell}>No chats found</td></tr>
            ) : chats.map(chat => (
              <tr key={chat.id} className={styles.row}>
                <td className={styles.name}>{chat.title}</td>
                <td><span className={`${styles.tag} ${styles.tagPurple}`}>{chat.model_id}</span></td>
                <td className={styles.monoMuted}>{chat.user_id.slice(0, 8)}...</td>
                <td>
                  <div className={styles.statusTags}>
                    {chat.is_archived && <span className={`${styles.tag} ${styles.tagRed}`}>Archived</span>}
                    {chat.is_pinned && <span className={`${styles.tag} ${styles.tagBlue}`}>Pinned</span>}
                    {!chat.is_archived && !chat.is_pinned && <span className={`${styles.tag} ${styles.tagGreen}`}>Active</span>}
                  </div>
                </td>
                <td className={styles.muted}>{new Date(chat.created_at).toLocaleDateString()}</td>
                <td>
                  <div className={styles.actionRow}>
                    <button className={styles.btnIcon} onClick={() => handleView(chat)} disabled={viewLoading} title="View messages">
                      <Eye size={13} />
                    </button>
                    <button className={styles.btnIcon} onClick={() => openEdit(chat)} title="Edit title">
                      <Pencil size={13} />
                    </button>
                    <button className={styles.btnIcon} onClick={() => togglePinned(chat)} title={chat.is_pinned ? 'Unpin chat' : 'Pin chat'}>
                      {chat.is_pinned ? <PinOff size={13} /> : <Pin size={13} />}
                    </button>
                    <button className={styles.btnIcon} onClick={() => toggleArchived(chat)} title={chat.is_archived ? 'Restore chat' : 'Archive chat'}>
                      {chat.is_archived ? <ArchiveRestore size={13} /> : <Archive size={13} />}
                    </button>
                    <button className={styles.btnDanger} onClick={() => handleDelete(chat.id)} title="Delete chat">
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
          <button className={styles.pageBtn} disabled={page === 0} onClick={() => setPage(current => current - 1)}><ChevronLeft size={16} /></button>
          <span className={styles.pageCurrent}>{page + 1}</span>
          <button className={styles.pageBtn} disabled={(page + 1) * PAGE_SIZE >= count} onClick={() => setPage(current => current + 1)}><ChevronRight size={16} /></button>
        </div>
      </div>

      {editChat && (
        <div className={styles.modalOverlay} onClick={() => setEditChat(null)}>
          <div className={styles.modal} onClick={event => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2 className={styles.modalTitle}>Edit Chat</h2>
              <button onClick={() => setEditChat(null)} className={styles.btnDanger} style={{ width: 28, height: 28 }}><X size={13} /></button>
            </div>
            <div className={styles.detailStack}>
              <div className={styles.infoLine}>
                <span>Model</span>
                <strong>{editChat.model_id}</strong>
              </div>
              <div className={styles.infoLine}>
                <span>Chat ID</span>
                <strong>{editChat.id}</strong>
              </div>
            </div>
            <div className={styles.field}>
              <label className={styles.label}>Title</label>
              <input className={styles.input} value={editTitle} onChange={event => setEditTitle(event.target.value)} />
            </div>
            <div className={styles.modalActions}>
              <button className={styles.btnCancel} onClick={() => setEditChat(null)}>Cancel</button>
              <button className={styles.btnPrimary} onClick={handleSaveTitle} disabled={saving || !editTitle.trim()}>
                <Save size={14} />{saving ? 'Saving...' : 'Save Chat'}
              </button>
            </div>
          </div>
        </div>
      )}

      {viewChat && (
        <div className={styles.modalOverlay} onClick={() => setViewChat(null)}>
          <div className={`${styles.modal} ${styles.modalWide}`} onClick={event => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <div className={styles.modalTitleRow}>
                <MessageSquare size={18} />
                <h2 className={styles.modalTitle}>{viewChat.chat.title}</h2>
              </div>
              <button onClick={() => setViewChat(null)} className={styles.btnDanger} style={{ width: 28, height: 28 }}><X size={13} /></button>
            </div>
            <div className={styles.messageList}>
              {viewChat.messages.length === 0 ? (
                <p className={styles.emptyInline}>No messages</p>
              ) : viewChat.messages.map(message => (
                <div
                  key={message.id}
                  className={`${styles.messageBubble} ${message.role === 'user' ? styles.messageUser : styles.messageAssistant}`}
                >
                  <div className={styles.messageRole}>{message.role.toUpperCase()}</div>
                  <p>{message.content}</p>
                  <span>{new Date(message.created_at).toLocaleString()}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
