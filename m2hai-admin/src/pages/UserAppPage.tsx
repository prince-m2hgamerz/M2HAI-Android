import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import type { Session } from '@supabase/supabase-js';
import { StreamingMarkdown, WindowChrome } from '@21st-sdk/react';
import {
  Archive,
  Bot,
  Check,
  ChevronDown,
  Copy,
  Download,
  Edit3,
  FolderOpen,
  LogOut,
  Menu,
  MessageSquare,
  Mic,
  PanelLeftClose,
  Pin,
  Plus,
  Search,
  Send,
  Settings2,
  Share2,
  Sparkles,
  ThumbsDown,
  ThumbsUp,
  Trash2,
  Upload,
  UserRound,
  WandSparkles,
  X,
} from 'lucide-react';
import type { AIModel, AppSettings, Chat, Message } from '../lib/api';
import {
  createUserChat,
  deleteUserChat,
  extractGeneratedImageUrl,
  getUserAppSettings,
  getUserChats,
  getUserMessages,
  getUserModels,
  getUserProfile,
  saveUserMessage,
  shouldUseImageModel,
  signOutUser,
  streamWebChat,
  submitUserFeedback,
  updateUserChat,
  updateUserProfile,
  type UserProfile,
} from '../lib/userApi';
import styles from './UserAppPage.module.css';

interface UserAppPageProps {
  session: Session;
}

const EXAMPLE_PROMPTS = [
  'Explain this idea in simple words',
  'Generate an image of a futuristic workspace',
  'Write a professional email',
  'Summarize my project plan',
];

const PROMPT_LIBRARY = [
  { title: 'Study helper', prompt: 'Explain this topic step by step with examples.' },
  { title: 'Image prompt', prompt: 'Generate an image of a clean modern Android AI app dashboard.' },
  { title: 'Code review', prompt: 'Review this code for bugs, edge cases, and improvements.' },
  { title: 'Business copy', prompt: 'Write a professional product description for my app.' },
];

export default function UserAppPage({ session }: UserAppPageProps) {
  const user = session.user;
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [settings, setSettings] = useState<AppSettings | null>(null);
  const [models, setModels] = useState<AIModel[]>([]);
  const [chats, setChats] = useState<Chat[]>([]);
  const [activeChatId, setActiveChatId] = useState<string>('new');
  const [messages, setMessages] = useState<Message[]>([]);
  const [selectedModel, setSelectedModel] = useState('');
  const [prompt, setPrompt] = useState('');
  const [streamingContent, setStreamingContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const [error, setError] = useState('');
  const [query, setQuery] = useState('');
  const [sidebarOpen, setSidebarOpen] = useState(false);
  const [showProfile, setShowProfile] = useState(false);
  const [profileName, setProfileName] = useState('');
  const [showArchived, setShowArchived] = useState(false);
  const [showPrompts, setShowPrompts] = useState(false);
  const [showModelPicker, setShowModelPicker] = useState(false);
  const [modelSearch, setModelSearch] = useState('');
  const [renamingId, setRenamingId] = useState<string | null>(null);
  const [renameTitle, setRenameTitle] = useState('');
  const [attachedImage, setAttachedImage] = useState<string | null>(null);
  const [toolsOpen, setToolsOpen] = useState(false);
  const [activeTool, setActiveTool] = useState<'image' | 'search' | 'write' | 'think' | null>(null);
  const abortRef = useRef<AbortController | null>(null);
  const bottomRef = useRef<HTMLDivElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const textareaRef = useRef<HTMLTextAreaElement | null>(null);

  const defaultModel = useMemo(() => {
    return models.find(model => model.id === settings?.default_model_id)?.id
      ?? models[0]?.id
      ?? '';
  }, [models, settings?.default_model_id]);

  const imageModel = useMemo(() => {
    return models.find(model => model.id === settings?.image_model_id)?.id
      ?? models.find(model => /flux|image|sdxl|diffusion/i.test(`${model.id} ${model.name}`))?.id
      ?? defaultModel;
  }, [defaultModel, models, settings?.image_model_id]);

  const filteredChats = useMemo(() => {
    const text = query.trim().toLowerCase();
    return chats
      .filter(chat => showArchived || !chat.is_archived)
      .sort((a, b) => Number(b.is_pinned) - Number(a.is_pinned) || new Date(b.updated_at).getTime() - new Date(a.updated_at).getTime())
      .filter(chat => !text || chat.title.toLowerCase().includes(text) || chat.model_id.toLowerCase().includes(text));
  }, [chats, query, showArchived]);

  const activeChat = chats.find(chat => chat.id === activeChatId);
  const assistantMessages = messages.filter(message => message.role === 'assistant');
  const selectedModelInfo = models.find(model => model.id === selectedModel)
    ?? models.find(model => model.id === defaultModel)
    ?? null;
  const activeModelLabel = selectedModelInfo?.name ?? selectedModel;
  const filteredModels = useMemo(() => {
    const text = modelSearch.trim().toLowerCase();
    if (!text) return models;
    return models.filter(model => `${model.name} ${model.provider} ${model.id} ${model.description ?? ''}`.toLowerCase().includes(text));
  }, [modelSearch, models]);

  const loadBase = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const [nextProfile, nextSettings, nextModels, nextChats] = await Promise.all([
        getUserProfile(user),
        getUserAppSettings(),
        getUserModels(),
        getUserChats(user.id),
      ]);
      setProfile(nextProfile);
      setProfileName(nextProfile.full_name ?? '');
      setSettings(nextSettings);
      setModels(nextModels);
      setChats(nextChats);
      setSelectedModel(current => current || nextModels.find(model => model.id === nextSettings.default_model_id)?.id || nextModels[0]?.id || '');
      if (nextProfile.is_disabled) {
        setError('This account has been disabled by the administrator.');
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : String(err));
    } finally {
      setLoading(false);
    }
  }, [user]);

  useEffect(() => { loadBase(); }, [loadBase]);

  useEffect(() => {
    if (activeChatId === 'new') {
      setMessages([]);
      return;
    }
    getUserMessages(activeChatId)
      .then(setMessages)
      .catch(err => setError(err instanceof Error ? err.message : String(err)));
  }, [activeChatId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth', block: 'end' });
  }, [messages.length, streamingContent]);

  useEffect(() => {
    const textarea = textareaRef.current;
    if (!textarea) return;
    textarea.style.height = 'auto';
    textarea.style.height = `${Math.min(textarea.scrollHeight, 180)}px`;
  }, [prompt]);

  const send = async (overridePrompt?: string) => {
    const content = (overridePrompt ?? prompt).trim();
    if (!content || sending) return;
    if (profile?.is_disabled) {
      setError('This account has been disabled by the administrator.');
      return;
    }
    if (settings && !settings.app_enabled) {
      setError(settings.maintenance_message || 'M2HAI is temporarily unavailable.');
      return;
    }

    setError('');
    setPrompt('');
    setAttachedImage(null);
    setToolsOpen(false);
    setSending(true);
    setStreamingContent('');

    const forceImage = activeTool === 'image' || shouldUseImageModel(content);
    const targetModel = forceImage && imageModel ? imageModel : selectedModel || defaultModel;
    if (!targetModel) {
      setSending(false);
      setError('No active AI model is available.');
      return;
    }

    const controller = new AbortController();
    abortRef.current = controller;

    try {
      let chat = activeChat;
      if (!chat) {
        chat = await createUserChat(user.id, targetModel, titleFromPrompt(content));
        setChats(current => [chat!, ...current]);
        setActiveChatId(chat.id);
      } else if (chat.model_id !== targetModel) {
        await updateUserChat(chat.id, { model_id: targetModel });
      }

      setSelectedModel(targetModel);
      const savedUser = await saveUserMessage(chat.id, 'user', content);
      const nextMessages = [...messages, savedUser];
      setMessages(nextMessages);

      const history = nextMessages.map(message => ({
        role: message.role as 'user' | 'assistant' | 'system',
        content: message.content,
      }));

      const assistantContent = await streamWebChat({
        modelId: targetModel,
        messages: history,
        signal: controller.signal,
        onToken: setStreamingContent,
      });

      if (assistantContent.trim()) {
        const savedAssistant = await saveUserMessage(chat.id, 'assistant', assistantContent);
        setMessages(current => [...current, savedAssistant]);
        setChats(current => current.map(item => item.id === chat!.id ? { ...item, updated_at: new Date().toISOString(), model_id: targetModel } : item));
      }
      setStreamingContent('');
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        setError('Response stopped.');
      } else {
        setError(err instanceof Error ? err.message : String(err));
      }
    } finally {
      abortRef.current = null;
      setSending(false);
    }
  };

  const regenerate = async () => {
    if (sending) return;
    if (!activeChat) return;
    if (profile?.is_disabled) {
      setError('This account has been disabled by the administrator.');
      return;
    }
    if (settings && !settings.app_enabled) {
      setError(settings.maintenance_message || 'M2HAI is temporarily unavailable.');
      return;
    }

    const lastUserIndex = messages.map(message => message.role).lastIndexOf('user');
    if (lastUserIndex < 0) return;

    const targetModel = selectedModel || activeChat.model_id || defaultModel;
    if (!targetModel) {
      setError('No active AI model is available.');
      return;
    }

    setError('');
    setSending(true);
    setStreamingContent('');
    const controller = new AbortController();
    abortRef.current = controller;

    try {
      const history = messages.slice(0, lastUserIndex + 1).map(message => ({
        role: message.role as 'user' | 'assistant' | 'system',
        content: message.content,
      }));

      const assistantContent = await streamWebChat({
        modelId: targetModel,
        messages: history,
        signal: controller.signal,
        onToken: setStreamingContent,
      });

      if (assistantContent.trim()) {
        const savedAssistant = await saveUserMessage(activeChat.id, 'assistant', assistantContent);
        setMessages(current => [...current, savedAssistant]);
        setChats(current => current.map(item => item.id === activeChat.id ? { ...item, updated_at: new Date().toISOString(), model_id: targetModel } : item));
      }
      setStreamingContent('');
    } catch (err) {
      if (err instanceof DOMException && err.name === 'AbortError') {
        setError('Response stopped.');
      } else {
        setError(err instanceof Error ? err.message : String(err));
      }
    } finally {
      abortRef.current = null;
      setSending(false);
    }
  };

  const stop = () => {
    abortRef.current?.abort();
    abortRef.current = null;
    setSending(false);
    setStreamingContent('');
  };

  const handlePromptKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && !event.shiftKey) {
      event.preventDefault();
      void send();
    }
  };

  const attachImage = (file?: File | null) => {
    if (!file || !file.type.startsWith('image/')) return;
    const reader = new FileReader();
    reader.onloadend = () => setAttachedImage(String(reader.result));
    reader.readAsDataURL(file);
  };

  const newChat = () => {
    stop();
    setActiveChatId('new');
    setMessages([]);
    setSidebarOpen(false);
  };

  const openChat = (chatId: string) => {
    stop();
    setActiveChatId(chatId);
    setSidebarOpen(false);
  };

  const removeChat = async (chatId: string) => {
    if (!confirm('Delete this chat?')) return;
    await deleteUserChat(chatId);
    setChats(current => current.filter(chat => chat.id !== chatId));
    if (activeChatId === chatId) newChat();
  };

  const togglePin = async (chat: Chat) => {
    await updateUserChat(chat.id, { is_pinned: !chat.is_pinned });
    setChats(current => current.map(item => item.id === chat.id ? { ...item, is_pinned: !item.is_pinned } : item));
  };

  const archiveChat = async (chat: Chat) => {
    await updateUserChat(chat.id, { is_archived: !chat.is_archived });
    setChats(current => current.map(item => item.id === chat.id ? { ...item, is_archived: !item.is_archived } : item));
  };

  const startRename = (chat: Chat) => {
    setRenamingId(chat.id);
    setRenameTitle(chat.title);
  };

  const saveRename = async () => {
    const title = renameTitle.trim();
    if (!renamingId || !title) {
      setRenamingId(null);
      return;
    }
    await updateUserChat(renamingId, { title });
    setChats(current => current.map(chat => chat.id === renamingId ? { ...chat, title } : chat));
    setRenamingId(null);
  };

  const submitFeedback = async (message: Message, action: 'copy' | 'like' | 'unlike' | 'share') => {
    await submitUserFeedback(message, user.id, action, activeChat?.model_id ?? selectedModel ?? null);
  };

  const saveProfile = async () => {
    if (!profile) return;
    await updateUserProfile(profile.id, { full_name: profileName.trim() || null, avatar_url: profile.avatar_url });
    setProfile(current => current ? { ...current, full_name: profileName.trim() || null } : current);
    setShowProfile(false);
  };

  const signOut = async () => {
    await signOutUser();
    window.location.href = '/';
  };

  const chooseModel = (modelId: string) => {
    setSelectedModel(modelId);
    setShowModelPicker(false);
    setModelSearch('');
  };

  if (loading) {
    return <div className={styles.loading}><div className={styles.spinner} />Loading M2HAI web app...</div>;
  }

  return (
    <div className={styles.shell}>
      <aside className={`${styles.sidebar} ${sidebarOpen ? styles.sidebarOpen : ''}`}>
        <WindowChrome />
        <div className={styles.sidebarTop}>
          <div className={styles.brand}><Sparkles size={18} /><strong>M2HAI</strong></div>
          <button onClick={() => setSidebarOpen(false)} className={styles.mobileOnly}><X size={18} /></button>
        </div>

        <button className={styles.newChat} onClick={newChat}><Plus size={15} />New chat</button>

        <div className={styles.sidebarTools}>
          <button onClick={() => setShowPrompts(true)}><WandSparkles size={14} />Prompts</button>
          <button onClick={() => setShowArchived(current => !current)}><FolderOpen size={14} />{showArchived ? 'Hide archived' : 'Archived'}</button>
        </div>

        <label className={styles.search}>
          <Search size={14} />
          <input value={query} onChange={event => setQuery(event.target.value)} placeholder="Search chats" />
        </label>

        <div className={styles.chatList}>
          {filteredChats.length === 0 ? (
            <p className={styles.emptyList}>No chats yet</p>
          ) : filteredChats.map(chat => (
            <div key={chat.id} className={`${styles.chatRow} ${activeChatId === chat.id ? styles.chatRowActive : ''}`}>
              <button onClick={() => openChat(chat.id)}>
                <MessageSquare size={15} />
                <span>{chat.title}</span>
              </button>
              <div className={styles.chatActions}>
                <button onClick={() => togglePin(chat)} title={chat.is_pinned ? 'Unpin' : 'Pin'}><Pin size={12} /></button>
                <button onClick={() => startRename(chat)} title="Rename"><Edit3 size={12} /></button>
                <button onClick={() => archiveChat(chat)} title={chat.is_archived ? 'Restore' : 'Archive'}><Archive size={12} /></button>
                <button onClick={() => removeChat(chat.id)} title="Delete"><Trash2 size={12} /></button>
              </div>
            </div>
          ))}
        </div>

        <div className={styles.account}>
          <button onClick={() => setShowProfile(true)}>
            <UserRound size={16} />
            <span>{profile?.full_name || user.email}</span>
          </button>
          <button onClick={signOut} title="Sign out"><LogOut size={16} /></button>
        </div>
      </aside>

      {sidebarOpen && <button className={styles.backdrop} onClick={() => setSidebarOpen(false)} aria-label="Close menu" />}

      <main className={styles.main}>
        <header className={styles.topbar}>
          <button className={styles.menuButton} onClick={() => setSidebarOpen(true)}><Menu size={18} /></button>
          <div className={styles.topbarTitle}>
            <span>{activeChat?.title || 'New chat'}</span>
            <small>{settings?.global_announcement || activeModelLabel || `${assistantMessages.length} assistant replies`}</small>
          </div>
          <button className={styles.topAction} onClick={regenerate} disabled={sending || messages.length === 0} title="Regenerate response">
            <RefreshIcon />
          </button>
          <button className={styles.modelButton} onClick={() => setShowModelPicker(true)} title="Change model">
            <Bot size={14} />
            <span>{selectedModelInfo?.name || 'Select model'}</span>
            <small>{selectedModelInfo?.provider || 'Active'}</small>
            <ChevronDown size={14} />
          </button>
          <button className={styles.collapseButton} onClick={() => setSidebarOpen(true)}><PanelLeftClose size={18} /></button>
        </header>

        {error && <div className={styles.error}>{error}</div>}

        {messages.length === 0 && !streamingContent ? (
          <section className={styles.welcome}>
            <span className={styles.welcomeIcon}><WandSparkles size={22} /></span>
            <h1>What can M2HAI help with?</h1>
            <p>Chat with the same active models used by the Android app. Image prompts automatically use the configured image model.</p>
            <div className={styles.promptGrid}>
              {EXAMPLE_PROMPTS.map(item => (
                <button key={item} onClick={() => send(item)}>{item}</button>
              ))}
            </div>
            <div className={styles.toolGrid}>
              <button onClick={() => setShowPrompts(true)}><WandSparkles size={16} /> Prompt library</button>
              <button><Upload size={16} /> Upload files</button>
              <button><Download size={16} /> Export chat</button>
            </div>
          </section>
        ) : (
          <section className={styles.messages}>
            {messages.map(message => (
              <MessageBubble
                key={message.id}
                message={message}
                onFeedback={submitFeedback}
              />
            ))}
            {streamingContent && <StreamingBubble content={streamingContent} />}
            <div ref={bottomRef} />
          </section>
        )}

        <div className={styles.composerWrap}>
          <div className={styles.composer}>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              className={styles.hiddenFileInput}
              onChange={event => attachImage(event.target.files?.[0] ?? null)}
            />
            {attachedImage && (
              <div className={styles.imagePreviewRow}>
                <button type="button" className={styles.imagePreviewButton} onClick={() => window.open(attachedImage, '_blank')}>
                  <img src={attachedImage} alt="Attached preview" />
                </button>
                <button type="button" className={styles.removeImageButton} onClick={() => setAttachedImage(null)} aria-label="Remove attached image">
                  <X size={13} />
                </button>
              </div>
            )}
            <label className={styles.composerField}>
              <span className={styles.srOnly}>Message</span>
              <textarea
                ref={textareaRef}
                value={prompt}
                onChange={event => setPrompt(event.target.value)}
                onKeyDown={handlePromptKeyDown}
                placeholder="Message..."
                disabled={profile?.is_disabled || (settings ? !settings.app_enabled : false)}
                rows={1}
              />
            </label>
            <div className={styles.promptToolbar}>
              <button
                className={styles.promptIconButton}
                type="button"
                onClick={() => fileInputRef.current?.click()}
                title="Attach image"
              >
                <Plus size={20} />
              </button>

              <div className={styles.toolsMenuWrap}>
                <button
                  className={styles.toolsButton}
                  type="button"
                  onClick={() => setToolsOpen(current => !current)}
                  title="Explore tools"
                >
                  <Settings2 size={16} />
                  {!activeTool && <span>Tools</span>}
                </button>
                {toolsOpen && (
                  <div className={styles.toolsPopover}>
                    <button type="button" onClick={() => { setActiveTool('image'); setSelectedModel(imageModel); setToolsOpen(false); }}>
                      <WandSparkles size={15} />
                      <span>Create an image</span>
                    </button>
                    <button type="button" onClick={() => { setActiveTool('search'); setToolsOpen(false); }}>
                      <Search size={15} />
                      <span>Search the web</span>
                    </button>
                    <button type="button" onClick={() => { setActiveTool('write'); setToolsOpen(false); }}>
                      <Edit3 size={15} />
                      <span>Write or code</span>
                    </button>
                    <button type="button" onClick={() => { setActiveTool('think'); setToolsOpen(false); }}>
                      <Sparkles size={15} />
                      <span>Think longer</span>
                    </button>
                  </div>
                )}
              </div>

              {activeTool && (
                <button type="button" className={styles.activeToolChip} onClick={() => setActiveTool(null)}>
                  {activeTool === 'image' ? <WandSparkles size={14} /> : activeTool === 'search' ? <Search size={14} /> : activeTool === 'write' ? <Edit3 size={14} /> : <Sparkles size={14} />}
                  <span>{activeTool === 'image' ? 'Image' : activeTool === 'search' ? 'Search' : activeTool === 'write' ? 'Write' : 'Think'}</span>
                  <X size={13} />
                </button>
              )}

              <div className={styles.promptRightActions}>
                <button className={styles.promptIconButton} type="button" title="Record voice">
                  <Mic size={18} />
                </button>
                {sending ? (
                  <button className={styles.stopButton} onClick={stop} type="button" title="Stop generating">Stop</button>
                ) : (
                  <button
                    className={styles.sendButton}
                    onClick={() => send()}
                    type="button"
                    disabled={(!prompt.trim() && !attachedImage) || profile?.is_disabled || (settings ? !settings.app_enabled : false)}
                    title="Send message"
                  >
                    <Send size={16} />
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      </main>

      {showModelPicker && (
        <div className={styles.modelOverlay} onClick={() => setShowModelPicker(false)}>
          <section className={styles.modelPanel} onClick={event => event.stopPropagation()} aria-label="Choose AI model">
            <div className={styles.modelPanelHeader}>
              <div>
                <span>AI model</span>
                <h2>Choose how M2HAI replies.</h2>
              </div>
              <button onClick={() => setShowModelPicker(false)} aria-label="Close model picker"><X size={16} /></button>
            </div>
            <label className={styles.modelSearch}>
              <Search size={14} />
              <input
                value={modelSearch}
                onChange={event => setModelSearch(event.target.value)}
                placeholder="Search model or provider"
                autoFocus
              />
            </label>
            <div className={styles.modelList}>
              {filteredModels.length === 0 ? (
                <p className={styles.emptyList}>No models found</p>
              ) : filteredModels.map(model => (
                <button
                  key={model.id}
                  className={`${styles.modelOption} ${selectedModel === model.id ? styles.modelOptionActive : ''}`}
                  onClick={() => chooseModel(model.id)}
                >
                  <span className={styles.modelOptionIcon}><Bot size={15} /></span>
                  <span className={styles.modelOptionText}>
                    <strong>{model.name}</strong>
                    <small>{model.provider}{model.is_free ? ' - Free' : ''}</small>
                    {model.description && <em>{model.description}</em>}
                  </span>
                  {selectedModel === model.id && <Check size={16} />}
                </button>
              ))}
            </div>
          </section>
        </div>
      )}

      {showProfile && profile && (
        <div className={styles.modalOverlay} onClick={() => setShowProfile(false)}>
          <div className={styles.profileModal} onClick={event => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2>Profile</h2>
              <button onClick={() => setShowProfile(false)}><X size={16} /></button>
            </div>
            <label>
              <span>Email</span>
              <input value={profile.email} disabled />
            </label>
            <label>
              <span>Full name</span>
              <input value={profileName} onChange={event => setProfileName(event.target.value)} />
            </label>
            <button className={styles.saveProfile} onClick={saveProfile}>Save profile</button>
          </div>
        </div>
      )}

      {renamingId && (
        <div className={styles.modalOverlay} onClick={() => setRenamingId(null)}>
          <div className={styles.profileModal} onClick={event => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2>Rename chat</h2>
              <button onClick={() => setRenamingId(null)}><X size={16} /></button>
            </div>
            <label>
              <span>Chat title</span>
              <input value={renameTitle} onChange={event => setRenameTitle(event.target.value)} onKeyDown={event => { if (event.key === 'Enter') void saveRename(); }} />
            </label>
            <button className={styles.saveProfile} onClick={saveRename}>Save title</button>
          </div>
        </div>
      )}

      {showPrompts && (
        <div className={styles.modalOverlay} onClick={() => setShowPrompts(false)}>
          <div className={styles.profileModal} onClick={event => event.stopPropagation()}>
            <div className={styles.modalHeader}>
              <h2>Prompt library</h2>
              <button onClick={() => setShowPrompts(false)}><X size={16} /></button>
            </div>
            <div className={styles.promptLibrary}>
              {PROMPT_LIBRARY.map(item => (
                <button
                  key={item.title}
                  onClick={() => {
                    setPrompt(item.prompt);
                    setShowPrompts(false);
                  }}
                >
                  <strong>{item.title}</strong>
                  <span>{item.prompt}</span>
                </button>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function RefreshIcon() {
  return <WandSparkles size={15} />;
}

function MessageBubble({ message, onFeedback }: {
  message: Message;
  onFeedback: (message: Message, action: 'copy' | 'like' | 'unlike' | 'share') => Promise<void>;
}) {
  const [copied, setCopied] = useState(false);
  const [reaction, setReaction] = useState<'like' | 'unlike' | null>(null);
  const isUser = message.role === 'user';
  const imageUrl = !isUser ? extractGeneratedImageUrl(message.content) : null;
  const text = imageUrl ? message.content.replace(/!\[[^\]]*]\([^)]+\)/, '').replace(imageUrl, '').trim() : message.content;

  const copy = async () => {
    await navigator.clipboard.writeText(message.content);
    setCopied(true);
    window.setTimeout(() => setCopied(false), 1200);
    await onFeedback(message, 'copy');
  };

  const react = async (next: 'like' | 'unlike') => {
    setReaction(next);
    await onFeedback(message, next);
  };

  const share = async () => {
    if (navigator.share) {
      await navigator.share({ text: message.content });
    } else {
      await navigator.clipboard.writeText(message.content);
    }
    await onFeedback(message, 'share');
  };

  return (
    <article className={`${styles.message} ${isUser ? styles.userMessage : styles.aiMessage}`}>
      {!isUser && <div className={styles.aiAvatar}><Sparkles size={15} /></div>}
      <div className={styles.messageBody}>
        {imageUrl && <img src={imageUrl} className={styles.generatedImage} alt="Generated" />}
        {text && <StreamingMarkdown content={text} className={styles.markdown} />}
        {!isUser && (
          <div className={styles.responseActions}>
            <button onClick={copy} title="Copy">{copied ? <Check size={13} /> : <Copy size={13} />}</button>
            <button className={reaction === 'like' ? styles.actionActive : ''} onClick={() => react('like')} title="Like"><ThumbsUp size={13} /></button>
            <button className={reaction === 'unlike' ? styles.actionActive : ''} onClick={() => react('unlike')} title="Unlike"><ThumbsDown size={13} /></button>
            <button onClick={share} title="Share"><Share2 size={13} /></button>
          </div>
        )}
      </div>
    </article>
  );
}

function StreamingBubble({ content }: { content: string }) {
  const imageUrl = extractGeneratedImageUrl(content);
  const text = imageUrl ? content.replace(/!\[[^\]]*]\([^)]+\)/, '').replace(imageUrl, '').trim() : content;
  return (
    <article className={`${styles.message} ${styles.aiMessage}`}>
      <div className={styles.aiAvatar}><Sparkles size={15} /></div>
      <div className={styles.messageBody}>
        {imageUrl && <img src={imageUrl} className={styles.generatedImage} alt="Generated" />}
        {text && <StreamingMarkdown content={text} className={styles.markdown} />}
        <div className={styles.streamingLine} />
      </div>
    </article>
  );
}

function titleFromPrompt(prompt: string): string {
  const clean = prompt.replace(/\s+/g, ' ').trim();
  return clean.length > 42 ? `${clean.slice(0, 39)}...` : clean || 'New chat';
}
