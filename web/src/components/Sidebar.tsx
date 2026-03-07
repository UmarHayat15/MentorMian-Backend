import { useState, useEffect } from 'react';
import { ConversationInfo, fetchConversations, deleteConversation } from '../api';

interface SidebarProps {
  activeView: 'chat' | 'documents';
  setActiveView: (view: 'chat' | 'documents') => void;
  activeConversationId: string | null;
  onSelectConversation: (id: string) => void;
  onNewChat: () => void;
  onOpenSettings: () => void;
  refreshKey: number;
}

export default function Sidebar({
  activeView,
  setActiveView,
  activeConversationId,
  onSelectConversation,
  onNewChat,
  onOpenSettings,
  refreshKey,
}: SidebarProps) {
  const [conversations, setConversations] = useState<ConversationInfo[]>([]);

  useEffect(() => {
    loadConversations();
  }, [refreshKey]);

  const loadConversations = async () => {
    try {
      const data = await fetchConversations();
      setConversations(data.reverse());
    } catch (err) {
      console.error('Failed to load conversations:', err);
    }
  };

  const handleDelete = async (e: React.MouseEvent, id: string) => {
    e.stopPropagation();
    try {
      await deleteConversation(id);
      setConversations(prev => prev.filter(c => c.id !== id));
      if (activeConversationId === id) {
        onNewChat();
      }
    } catch (err) {
      console.error('Failed to delete conversation:', err);
    }
  };

  return (
    <aside className="sidebar">
      {/* Header */}
      <div className="sidebar-header">
        <div className="sidebar-logo">
          <span className="logo-icon">🎓</span>
          MentorMian
        </div>
        <button className="new-chat-btn" onClick={onNewChat}>
          ✨ New Chat
        </button>
      </div>

      {/* Nav */}
      <div className="sidebar-nav">
        <button
          className={activeView === 'chat' ? 'active' : ''}
          onClick={() => setActiveView('chat')}
        >
          💬 Chats
        </button>
        <button
          className={activeView === 'documents' ? 'active' : ''}
          onClick={() => setActiveView('documents')}
        >
          📄 Docs
        </button>
      </div>

      {/* Conversation List */}
      {activeView === 'chat' && (
        <div className="conversation-list">
          {conversations.length === 0 ? (
            <div className="empty-state">
              <p className="empty-icon">💬</p>
              <p>No conversations yet</p>
            </div>
          ) : (
            conversations.map(conv => (
              <div
                key={conv.id}
                className={`conversation-item ${conv.id === activeConversationId ? 'active' : ''}`}
                onClick={() => onSelectConversation(conv.id)}
              >
                <span className="conv-icon">💬</span>
                <span className="conv-title">{conv.title || 'Untitled'}</span>
                <button
                  className="conv-delete"
                  onClick={(e) => handleDelete(e, conv.id)}
                  title="Delete"
                >
                  🗑
                </button>
              </div>
            ))
          )}
        </div>
      )}

      {activeView === 'documents' && (
        <div className="conversation-list">
          <div className="empty-state">
            <p className="empty-icon">📄</p>
            <p>Manage documents in the main area</p>
          </div>
        </div>
      )}

      {/* Footer */}
      <div className="sidebar-footer">
        <button className="settings-btn" onClick={onOpenSettings}>
          ⚙️ Settings
        </button>
      </div>
    </aside>
  );
}
