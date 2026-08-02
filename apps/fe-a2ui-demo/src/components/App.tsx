import { useCallback, useEffect, useRef, useState } from 'react';
import {
  MessageProcessor,
  Catalog,
  type SurfaceModel,
  type A2uiClientAction,
  type A2uiMessage,
} from '@a2ui/web_core/v0_9';
import {
  A2uiSurface,
  basicCatalog,
  type ReactComponentImplementation,
} from '@a2ui/react/v0_9';
import { streamSurface, sendAction } from '../services/api';
// Package exports omit CSS; load via Vite alias in vite.config.ts
import '@a2ui/react-v0_9-css';

/** Canonical basic catalog id (vendored catalog.json). @a2ui/react's basicCatalog.id differs — remap below. */
const SERVER_CATALOG_ID = 'https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json';

const generationMode = import.meta.env.VITE_A2UI_GENERATION_MODE === 'dynamic' ? 'dynamic' : 'template';

const TEMPLATE_SAMPLE_PROMPTS = [
  'Show me a login form',
  'Create a weather card for San Francisco',
  'Display a hero section with a call to action',
];

const DYNAMIC_SAMPLE_PROMPTS = [
  'Build a simple greeting card that says Hello',
  'Create a product card for a laptop with a Buy button',
  'Design a settings panel for notification preferences',
];

function createServerAlignedCatalog(): Catalog<ReactComponentImplementation> {
  return new Catalog(
    SERVER_CATALOG_ID,
    Array.from(basicCatalog.components.values()),
    Array.from(basicCatalog.functions.values()),
  );
}

export function App() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [input, setInput] = useState('');
  const [surfaces, setSurfaces] = useState<SurfaceModel<ReactComponentImplementation>[]>([]);
  const processorRef = useRef<MessageProcessor<ReactComponentImplementation> | null>(null);

  if (processorRef.current === null) {
    processorRef.current = new MessageProcessor(
      // Register under the server catalogId; do not use React's non-canonical basicCatalog.id.
      [createServerAlignedCatalog()],
      async (action: A2uiClientAction) => {
        try {
          const result = await sendAction({ action });
          const processor = processorRef.current;
          if (result.messages && result.messages.length > 0 && processor) {
            processor.processMessages(result.messages as A2uiMessage[]);
            setSurfaces(Array.from(processor.model.surfacesMap.values()));
          }
        } catch (err) {
          console.error('Action failed:', err);
        }
      },
    );
  }

  const processor = processorRef.current;

  useEffect(() => {
    const sync = () => setSurfaces(Array.from(processor.model.surfacesMap.values()));
    const createdSub = processor.onSurfaceCreated(sync);
    const deletedSub = processor.onSurfaceDeleted(sync);
    return () => {
      createdSub.unsubscribe();
      deletedSub.unsubscribe();
    };
  }, [processor]);

  const clear = useCallback(() => {
    for (const surfaceId of Array.from(processor.model.surfacesMap.keys())) {
      processor.processMessages([
        { version: 'v0.9.1', deleteSurface: { surfaceId } },
      ]);
    }
    setSurfaces([]);
    setError(null);
  }, [processor]);

  const generate = useCallback(async (content: string) => {
    setLoading(true);
    setError(null);
    clear();

    try {
      await streamSurface(
        content,
        (message) => {
          try {
            processor.processMessages([message as A2uiMessage]);
            setSurfaces(Array.from(processor.model.surfacesMap.values()));
          } catch (processingError) {
            const detail = processingError instanceof Error
              ? processingError.message
              : String(processingError);
            setError(`Invalid A2UI message from server: ${detail}`);
          }
        },
        (err) => setError(err),
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [processor, clear]);

  const samplePrompts = generationMode === 'dynamic' ? DYNAMIC_SAMPLE_PROMPTS : TEMPLATE_SAMPLE_PROMPTS;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || loading) return;
    void generate(input.trim());
    setInput('');
  };

  return (
    <div className="app">
      <header>
        <h1>A2UI Runtime Demo</h1>
        <p>Connect to spring-a2ui-runtime backend and render A2UI v0.9.1 surfaces</p>
        <p className="generation-mode-hint">
          Generation mode hint: <strong>{generationMode}</strong>
          {' '}(actual mode is set by the showcase Spring profile)
        </p>
      </header>

      <main>
        <div className="controls">
          <button type="button" onClick={clear} disabled={loading}>Clear</button>
        </div>

        {error && (
          <div className="a2ui-error">
            <h3>Error</h3>
            <p>{error}</p>
          </div>
        )}

        <div className="surface-container">
          {surfaces.length === 0 ? (
            <div className="a2ui-empty">
              <p>Send a message to generate an A2UI surface.</p>
            </div>
          ) : (
            surfaces.map((surface) => (
              <A2uiSurface key={surface.id} surface={surface} />
            ))
          )}
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
              onClick={() => void generate(prompt)}
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
            {loading ? 'Generating...' : 'Send'}
          </button>
        </form>
      </main>
    </div>
  );
}
