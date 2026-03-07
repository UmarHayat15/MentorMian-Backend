import { useState, useRef, useEffect, useCallback } from 'react';
import ReactMarkdown from 'react-markdown';
import { chatStream, fetchMessages } from '../api';
import { useConfig } from '../ConfigContext';

interface ChatViewProps {
  conversationId: string | null;
  onConversationCreated: (id: string) => void;
}

interface Message {
  role: string;
  content: string;
}

export default function ChatView({ conversationId, onConversationCreated }: ChatViewProps) {
  const { isConfigured } = useConfig();
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  // Track the conversation ID locally so we don't lose it during streaming.
  const localConvIdRef = useRef<string | null>(conversationId);
  const isStreamingRef = useRef(false);

  // When parent passes a NEW conversationId (sidebar click or new chat),
  // reload messages — but skip if streaming.
  useEffect(() => {
    if (isStreamingRef.current) {
      localConvIdRef.current = conversationId;
      return;
    }

    localConvIdRef.current = conversationId;

    if (conversationId) {
      loadMessages(conversationId);
    } else {
      setMessages([]);
    }
  }, [conversationId]);

  // Auto-scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const loadMessages = async (id: string) => {
    try {
      const data = await fetchMessages(id);
      setMessages(
        data
          .filter((m) => m.role !== 'system')
          .map((m) => ({ role: m.role, content: m.content }))
      );
    } catch (err) {
      console.error('Failed to load messages:', err);
    }
  };

  const handleSend = useCallback(async () => {
    const msg = input.trim();
    if (!msg || isStreaming) return;

    setInput('');
    if (textareaRef.current) {
      textareaRef.current.style.height = 'auto';
    }

    setMessages(prev => [...prev, { role: 'user', content: msg }]);
    setIsStreaming(true);
    isStreamingRef.current = true;
    setMessages(prev => [...prev, { role: 'assistant', content: '' }]);

    const convIdForThisRequest = localConvIdRef.current;

    try {
      await chatStream(msg, convIdForThisRequest, null, {
        onConversationId: (id) => {
          localConvIdRef.current = id;
          onConversationCreated(id);
        },
        onToken: (token) => {
          setMessages(prev => {
            const updated = [...prev];
            const last = updated[updated.length - 1];
            if (last && last.role === 'assistant') {
              updated[updated.length - 1] = {
                ...last,
                content: last.content + token,
              };
            }
            return updated;
          });
        },
        onError: (error) => {
          setMessages(prev => {
            const updated = [...prev];
            const last = updated[updated.length - 1];
            if (last && last.role === 'assistant') {
              updated[updated.length - 1] = {
                ...last,
                content: `⚠️ Error: ${error}`,
              };
            }
            return updated;
          });
        },
        onDone: () => {
          setIsStreaming(false);
          isStreamingRef.current = false;
        },
      });
    } catch (err) {
      console.error('Chat stream error:', err);
      setIsStreaming(false);
      isStreamingRef.current = false;
      setMessages(prev => {
        const updated = [...prev];
        const last = updated[updated.length - 1];
        if (last && last.role === 'assistant') {
          updated[updated.length - 1] = {
            ...last,
            content: '⚠️ Failed to connect to the server.',
          };
        }
        return updated;
      });
    }
  }, [input, isStreaming, onConversationCreated]);

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const handleInputChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
    setInput(e.target.value);
    const ta = e.target;
    ta.style.height = 'auto';
    ta.style.height = Math.min(ta.scrollHeight, 120) + 'px';
  };

  return (
    <div className="chat-container">
      <div className="chat-messages">
        {messages.length === 0 ? (
          <div className="chat-welcome">
            <div className="welcome-icon">🎓</div>
            <h2>MentorMian AI Tutor</h2>
            <p>
              Ask me anything — Physics, Math, History, or any subject.
              I respond in English, Urdu, or Roman Urdu!
            </p>
            {isConfigured ? (
              <div className="welcome-suggestions">
                {[
                  "Newton ke laws samjhao 🍎",
                  "Photosynthesis explain kro",
                  "Solve: 2x + 5 = 15",
                  "WW2 ki history batao",
                  "Python mein loop kya hai?",
                ].map((suggestion) => (
                  <button
                    key={suggestion}
                    className="suggestion-chip"
                    onClick={() => {
                      setInput(suggestion);
                      textareaRef.current?.focus();
                    }}
                  >
                    {suggestion}
                  </button>
                ))}
              </div>
            ) : (
              <p style={{ color: 'var(--warning)', marginTop: '8px' }}>
                ⚠️ Please configure your API key in Settings first.
              </p>
            )}
          </div>
        ) : (
          messages.map((msg, i) => (
            <div key={i} className={`message ${msg.role}`}>
              <div className="message-avatar">
                {msg.role === 'assistant' ? '🤖' : '👤'}
              </div>
              <div className="message-content markdown-body">
                {msg.role === 'assistant' ? (
                  <>
                    <ReactMarkdown>{msg.content}</ReactMarkdown>
                    {isStreaming && i === messages.length - 1 && (
                      <span className="typing-cursor" />
                    )}
                  </>
                ) : (
                  msg.content
                )}
              </div>
            </div>
          ))
        )}
        <div ref={messagesEndRef} />
      </div>

      <div className="chat-input-container">
        <div className="chat-input-wrapper">
          <textarea
            ref={textareaRef}
            value={input}
            onChange={handleInputChange}
            onKeyDown={handleKeyDown}
            placeholder={
              isConfigured
                ? 'Ask anything — Newton\'s laws, math problems, history...'
                : 'Configure your API key in Settings first'
            }
            rows={1}
            disabled={!isConfigured}
          />
          <button
            className="send-btn"
            onClick={handleSend}
            disabled={!input.trim() || isStreaming || !isConfigured}
          >
            ▶
          </button>
        </div>
        <p className="chat-disclaimer">
          AI responses may be inaccurate. Always verify important information.
        </p>
      </div>
    </div>
  );
}
