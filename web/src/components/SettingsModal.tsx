import { useState } from 'react';
import { useConfig } from '../ConfigContext';

interface SettingsModalProps {
  onClose: () => void;
}

export default function SettingsModal({ onClose }: SettingsModalProps) {
  const { config, updateConfig } = useConfig();
  const [form, setForm] = useState({ ...config });

  const handleSave = () => {
    updateConfig(form);
    onClose();
  };

  const showBaseUrl = form.provider === 'ollama' || form.provider === 'custom';

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>⚙️ Settings</h2>
          <button className="modal-close" onClick={onClose}>✕</button>
        </div>

        <div className="form-group">
          <label>LLM Provider</label>
          <select
            value={form.provider}
            onChange={(e) => setForm({ ...form, provider: e.target.value })}
          >
            <option value="gemini">Google Gemini</option>
            <option value="openai">OpenAI</option>
            <option value="ollama">Ollama (Local)</option>
            <option value="custom">Custom (OpenAI-Compatible)</option>
          </select>
        </div>

        {form.provider !== 'ollama' && (
          <div className="form-group">
            <label>API Key</label>
            <input
              type="password"
              value={form.apiKey}
              onChange={(e) => setForm({ ...form, apiKey: e.target.value })}
              placeholder={
                form.provider === 'gemini'
                  ? 'AIza...'
                  : form.provider === 'openai'
                  ? 'sk-...'
                  : 'Your API key'
              }
            />
            <p className="hint">
              {form.provider === 'gemini'
                ? 'Get your key at ai.google.dev'
                : form.provider === 'openai'
                ? 'Get your key at platform.openai.com'
                : 'API key for your custom endpoint'}
            </p>
          </div>
        )}

        <div className="form-group">
          <label>Model</label>
          <input
            type="text"
            value={form.model}
            onChange={(e) => setForm({ ...form, model: e.target.value })}
            placeholder={
              form.provider === 'gemini'
                ? 'gemini-2.5-flash'
                : form.provider === 'openai'
                ? 'gpt-4o'
                : form.provider === 'ollama'
                ? 'llama3'
                : 'model-name'
            }
          />
          <p className="hint">
            {form.provider === 'gemini' && 'e.g. gemini-2.5-flash, gemini-2.5-pro'}
            {form.provider === 'openai' && 'e.g. gpt-4o, gpt-4o-mini'}
            {form.provider === 'ollama' && 'e.g. llama3, mistral, qwen2'}
            {form.provider === 'custom' && 'Model name for your custom endpoint'}
          </p>
        </div>

        {showBaseUrl && (
          <div className="form-group">
            <label>Base URL</label>
            <input
              type="text"
              value={form.baseUrl}
              onChange={(e) => setForm({ ...form, baseUrl: e.target.value })}
              placeholder={
                form.provider === 'ollama'
                  ? 'http://localhost:11434'
                  : 'https://your-api-endpoint.com/v1'
              }
            />
          </div>
        )}

        <button className="save-btn" onClick={handleSave}>
          Save Settings
        </button>
      </div>
    </div>
  );
}
