import { useEffect, useRef, useState } from 'react';
import { A2UIProvider, A2UIRenderer, initializeDefaultCatalog } from '@a2ui/react/v0_8';
import type { A2UIClientEventMessage } from '@a2ui/react/v0_8';
import { useSurfaceGeneration } from '../hooks/useSurfaceGeneration';

initializeDefaultCatalog();

const generationMode = import.meta.env.VITE_A2UI_GENERATION_MODE === 'dynamic' ? 'dynamic' : 'template';

const TEMPLATE_SAMPLE_PROMPTS = [
  'Show me a login form',
  'Create a weather card for San Francisco',
  'Display a hero section with a call to action',
];

const DYNAMIC_SAMPLE_PROMPTS = [
  'Build a dashboard summarizing Q1 sales by region',
  'Create a product comparison table for three laptops',
  'Design a settings panel for notification preferences',
];

export function App() {
  const onActionRef = useRef<(event: A2UIClientEventMessage) => void | Promise<void>>(async () => {});

  return (
    <A2UIProvider onAction={(event) => onActionRef.current(event)}>
      <DemoContent onActionRef={onActionRef} />
    </A2UIProvider>
  );
}

function DemoContent({
  onActionRef,
}: {
  onActionRef: React.MutableRefObject<(event: A2UIClientEventMessage) => void | Promise<void>>;
}) {
  const { loading, error, generate, clear, handleAction } = useSurfaceGeneration();
  const [input, setInput] = useState('');
  const samplePrompts = generationMode === 'dynamic' ? DYNAMIC_SAMPLE_PROMPTS : TEMPLATE_SAMPLE_PROMPTS;

  useEffect(() => {
    onActionRef.current = handleAction;
  }, [handleAction, onActionRef]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || loading) return;
    generate(input.trim());
    setInput('');
  };

  const handleSamplePrompt = (prompt: string) => {
    if (loading) return;
    generate(prompt);
  };

  return (
    <div className="app">
      <header>
        <h1>A2UI Runtime Demo</h1>
        <p>Connect to spring-a2ui-runtime backend and render A2UI v0.8 surfaces</p>
        <p className="generation-mode-hint">
          Generation mode: <strong>{generationMode}</strong>
          {generationMode === 'dynamic' && (
            <span> — start the showcase with <code>--spring.profiles.active=dynamic</code></span>
          )}
        </p>
      </header>

      <main>
        <div className="controls">
          <button onClick={clear} disabled={loading}>Clear</button>
        </div>

        {error && (
          <div className="a2ui-error">
            <h3>Error</h3>
            <p>{error}</p>
          </div>
        )}

        <div className="surface-container">
          <A2UIRenderer surfaceId="main" fallback={
            <div className="a2ui-empty">
              <p>Send a message to generate an A2UI surface.</p>
            </div>
          } />
        </div>

        {loading && <div className="a2ui-loading">Generating...</div>}

        <div className="sample-prompts">
          <span className="sample-prompts-label">Try:</span>
          {samplePrompts.map((prompt) => (
            <button
              key={prompt}
              type="button"
              className="sample-prompt-button"
              disabled={loading}
              onClick={() => handleSamplePrompt(prompt)}
            >
              {prompt}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="input-form">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder={
              generationMode === 'dynamic'
                ? 'Describe any UI to generate from scratch...'
                : 'Describe a surface to generate...'
            }
            disabled={loading}
            className="input-field"
          />
          <button type="submit" disabled={loading || !input.trim()}>
            {loading ? 'Generating...' : 'Generate'}
          </button>
        </form>
      </main>
    </div>
  );
}
