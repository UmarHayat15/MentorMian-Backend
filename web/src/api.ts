// ============================================================
// API Layer — All backend API calls + SSE streaming
// ============================================================

const API_BASE = '/api/v1';

function getLlmHeaders(): Record<string, string> {
  const headers: Record<string, string> = {};
  const provider = localStorage.getItem('llm_provider');
  const apiKey = localStorage.getItem('llm_api_key');
  const model = localStorage.getItem('llm_model');
  const baseUrl = localStorage.getItem('llm_base_url');

  if (provider) headers['X-LLM-Provider'] = provider;
  if (apiKey) headers['X-API-Key'] = apiKey;
  if (model) headers['X-Model'] = model;
  if (baseUrl) headers['X-Base-URL'] = baseUrl;

  return headers;
}

// --- Health ---

export async function fetchHealth() {
  const res = await fetch(`${API_BASE}/health`);
  return res.json();
}

// --- Documents ---

export interface DocumentInfo {
  id: string;
  title: string | null;
  fileName: string;
  totalPages: number | null;
  totalChunks: number;
  status: string;
  createdAt: string;
}

export async function fetchDocuments(): Promise<DocumentInfo[]> {
  const res = await fetch(`${API_BASE}/documents`, {
    headers: getLlmHeaders(),
  });
  const data = await res.json();
  return data.data?.documents ?? [];
}

export async function fetchDocument(id: string): Promise<DocumentInfo> {
  const res = await fetch(`${API_BASE}/documents/${id}`, {
    headers: getLlmHeaders(),
  });
  const data = await res.json();
  return data.data;
}

export async function uploadDocument(file: File, title: string): Promise<DocumentInfo> {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('title', title);

  const res = await fetch(`${API_BASE}/documents/upload`, {
    method: 'POST',
    headers: getLlmHeaders(),
    body: formData,
  });
  const data = await res.json();
  return data.data;
}

export async function deleteDocument(id: string): Promise<void> {
  await fetch(`${API_BASE}/documents/${id}`, {
    method: 'DELETE',
    headers: getLlmHeaders(),
  });
}

// --- Chat (SSE Streaming) ---

export interface ChatStreamCallbacks {
  onConversationId: (id: string) => void;
  onToken: (token: string) => void;
  onError: (error: string) => void;
  onDone: () => void;
}

export async function chatStream(
  message: string,
  conversationId: string | null,
  documentIds: string[] | null,
  callbacks: ChatStreamCallbacks
): Promise<void> {
  const body: Record<string, unknown> = { message };
  if (conversationId) body.conversationId = conversationId;
  if (documentIds && documentIds.length > 0) body.documentIds = documentIds;

  const res = await fetch(`${API_BASE}/chat`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getLlmHeaders(),
    },
    body: JSON.stringify(body),
  });

  const reader = res.body?.getReader();
  if (!reader) {
    callbacks.onError('No response body');
    return;
  }

  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() ?? '';

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed.startsWith('data: ')) continue;

      const data = trimmed.slice(6);
      if (data === '[DONE]') {
        callbacks.onDone();
        return;
      }

      try {
        const parsed = JSON.parse(data);
        if (parsed.conversation_id) {
          callbacks.onConversationId(parsed.conversation_id);
        }
        if (parsed.token !== undefined) {
          callbacks.onToken(parsed.token);
        }
        if (parsed.error) {
          callbacks.onError(parsed.error);
        }
      } catch {
        // skip malformed
      }
    }
  }

  callbacks.onDone();
}

// --- Conversations ---

export interface ConversationInfo {
  id: string;
  title: string | null;
  createdAt: string;
}

export interface MessageInfo {
  id: string;
  role: string;
  content: string;
  sources: string | null;
  createdAt: string;
}

export async function fetchConversations(): Promise<ConversationInfo[]> {
  const res = await fetch(`${API_BASE}/conversations`);
  const data = await res.json();
  return data.data?.conversations ?? [];
}

export async function fetchMessages(conversationId: string): Promise<MessageInfo[]> {
  const res = await fetch(`${API_BASE}/conversations/${conversationId}/messages`);
  const data = await res.json();
  return data.data?.messages ?? [];
}

export async function deleteConversation(id: string): Promise<void> {
  await fetch(`${API_BASE}/conversations/${id}`, { method: 'DELETE' });
}

// --- Models ---

export interface ModelInfo {
  id: string;
  name: string;
  provider: string;
}

export async function fetchModels(): Promise<ModelInfo[]> {
  const res = await fetch(`${API_BASE}/models/available`);
  const data = await res.json();
  return data.data?.models ?? [];
}
