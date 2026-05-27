import { useEffect, useRef, useState } from 'react';
import { A2UIProvider, A2UIRenderer, initializeDefaultCatalog } from '@a2ui/react/v0_8';
import type { A2UIClientEventMessage } from '@a2ui/react/v0_8';
import { useSurfaceGeneration } from '../hooks/useSurfaceGeneration';

initializeDefaultCatalog();

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

  useEffect(() => {
    onActionRef.current = handleAction;
  }, [handleAction, onActionRef]);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || loading) return;
    generate(input.trim());
    setInput('');
  };

  return (
    <div className="app">
      <header>
        <h1>A2UI Runtime Demo</h1>
        <p>Connect to spring-a2ui-runtime backend and render A2UI v0.8 surfaces</p>
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

        <form onSubmit={handleSubmit} className="input-form">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            placeholder="Describe a surface to generate..."
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
