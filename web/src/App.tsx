import { useState, useCallback } from 'react';
import { ConfigProvider } from './ConfigContext';
import Sidebar from './components/Sidebar';
import ChatView from './components/ChatView';
import DocumentsView from './components/DocumentsView';
import SettingsModal from './components/SettingsModal';
import './App.css';

function AppContent() {
  const [activeView, setActiveView] = useState<'chat' | 'documents'>('chat');
  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [showSettings, setShowSettings] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);

  const handleNewChat = useCallback(() => {
    setActiveConversationId(null);
    setActiveView('chat');
  }, []);

  const handleSelectConversation = useCallback((id: string) => {
    setActiveConversationId(id);
    setActiveView('chat');
  }, []);

  const handleConversationCreated = useCallback((id: string) => {
    setActiveConversationId(id);
    // Refresh conversation list in sidebar
    setRefreshKey(prev => prev + 1);
  }, []);

  return (
    <div className="app-layout">
      <Sidebar
        activeView={activeView}
        setActiveView={setActiveView}
        activeConversationId={activeConversationId}
        onSelectConversation={handleSelectConversation}
        onNewChat={handleNewChat}
        onOpenSettings={() => setShowSettings(true)}
        refreshKey={refreshKey}
      />

      <main className="main-content">
        {activeView === 'chat' ? (
          <ChatView
            conversationId={activeConversationId}
            onConversationCreated={handleConversationCreated}
          />
        ) : (
          <DocumentsView />
        )}
      </main>

      {showSettings && (
        <SettingsModal onClose={() => setShowSettings(false)} />
      )}
    </div>
  );
}

export default function App() {
  return (
    <ConfigProvider>
      <AppContent />
    </ConfigProvider>
  );
}
