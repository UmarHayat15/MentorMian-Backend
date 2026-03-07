import { createContext, useContext, useState, useEffect, ReactNode } from 'react';

interface LlmConfig {
  provider: string;
  apiKey: string;
  model: string;
  baseUrl: string;
}

interface ConfigContextType {
  config: LlmConfig;
  updateConfig: (config: Partial<LlmConfig>) => void;
  isConfigured: boolean;
}

const defaultConfig: LlmConfig = {
  provider: 'gemini',
  apiKey: '',
  model: 'gemini-2.5-flash',
  baseUrl: '',
};

const ConfigContext = createContext<ConfigContextType | null>(null);

export function ConfigProvider({ children }: { children: ReactNode }) {
  const [config, setConfig] = useState<LlmConfig>(() => {
    return {
      provider: localStorage.getItem('llm_provider') || defaultConfig.provider,
      apiKey: localStorage.getItem('llm_api_key') || defaultConfig.apiKey,
      model: localStorage.getItem('llm_model') || defaultConfig.model,
      baseUrl: localStorage.getItem('llm_base_url') || defaultConfig.baseUrl,
    };
  });

  useEffect(() => {
    localStorage.setItem('llm_provider', config.provider);
    localStorage.setItem('llm_api_key', config.apiKey);
    localStorage.setItem('llm_model', config.model);
    localStorage.setItem('llm_base_url', config.baseUrl);
  }, [config]);

  const updateConfig = (partial: Partial<LlmConfig>) => {
    setConfig(prev => ({ ...prev, ...partial }));
  };

  const isConfigured = !!config.apiKey || config.provider === 'ollama';

  return (
    <ConfigContext.Provider value={{ config, updateConfig, isConfigured }}>
      {children}
    </ConfigContext.Provider>
  );
}

export function useConfig() {
  const ctx = useContext(ConfigContext);
  if (!ctx) throw new Error('useConfig must be used within ConfigProvider');
  return ctx;
}
