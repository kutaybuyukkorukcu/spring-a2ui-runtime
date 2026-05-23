import { useState, useRef, useCallback, useEffect } from 'react';
import { A2UIProvider, A2UIRenderer, initializeDefaultCatalog } from '@a2ui/react/v0_8';
import type { A2UIClientEventMessage } from '@a2ui/react/v0_8';
import { useSurfaceGeneration } from '../hooks/useSurfaceGeneration';

initializeDefaultCatalog();

function AppContent({ setActionHandler }: {
  setActionHandler: (fn: ((event: A2UIClientEventMessage) => Promise<void>) | null) => void;
}) {
  const { loading, error, mode, setMode, generate, clear, handleAction, activeSurfaceId } = useSurfaceGeneration();
  const [input, setInput] = useState('');

  useEffect(() => {
    setActionHandler(handleAction);
    return () => setActionHandler(null);
  }, [handleAction, setActionHandler]);

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
            <div className="mode-toggle">
              <label>
                <input
                  type="radio"
                  name="mode"
                  value="stream"
                  checked={mode === 'stream'}
                  onChange={() => setMode('stream')}
                />
                Stream
              </label>
              <label>
                <input
                  type="radio"
                  name="mode"
                  value="sync"
                  checked={mode === 'sync'}
                  onChange={() => setMode('sync')}
                />
                Sync
              </label>
            </div>
            <button onClick={clear} disabled={loading}>Clear</button>
          </div>

          {error && (
            <div className="a2ui-error">
              <h3>Error</h3>
              <p>{error}</p>
            </div>
          )}

          <div className="surface-container">
            <A2UIRenderer surfaceId={activeSurfaceId} fallback={
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

export function App() {
  const actionHandlerRef = useRef<((event: A2UIClientEventMessage) => Promise<void>) | null>(null);

  const onAction = useCallback((event: A2UIClientEventMessage) => {
    return actionHandlerRef.current?.(event) ?? Promise.resolve();
  }, []);

  return (
    <A2UIProvider onAction={onAction}>
      <AppContent setActionHandler={(fn) => { actionHandlerRef.current = fn; }} />
    </A2UIProvider>
  );
}