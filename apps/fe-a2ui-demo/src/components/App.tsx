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
import {
  streamSurface,
  sendAction,
  fetchDemoInfo,
  extractActionResult,
  messagesWithoutDelete,
  type StreamUtilizationEvent,
  type StreamContext,
  type DemoInfo,
} from '../services/api';
import '@a2ui/react-v0_9-css';

const SERVER_CATALOG_ID = 'https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json';

type StoryStep = 'idle' | 'propose' | 'review' | 'decided';

function createServerAlignedCatalog(): Catalog<ReactComponentImplementation> {
  return new Catalog(
    SERVER_CATALOG_ID,
    Array.from(basicCatalog.components.values()),
    Array.from(basicCatalog.functions.values()),
  );
}

function isRawPayload(text: string | null): boolean {
  if (!text) return true;
  const trimmed = text.trim();
  return trimmed.startsWith('{') || trimmed.startsWith('[');
}

export function App() {
  const [demoInfo, setDemoInfo] = useState<DemoInfo | null>(null);
  const [demoInfoError, setDemoInfoError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [storyStep, setStoryStep] = useState<StoryStep>('idle');
  const [runStatus, setRunStatus] = useState<string | null>(null);
  const [toolProgress, setToolProgress] = useState<string | null>(null);
  const [surfaces, setSurfaces] = useState<SurfaceModel<ReactComponentImplementation>[]>([]);
  const processorRef = useRef<MessageProcessor<ReactComponentImplementation> | null>(null);

  const applyMessages = useCallback((processor: MessageProcessor<ReactComponentImplementation>, messages: A2uiMessage[]) => {
    for (const surfaceId of Array.from(processor.model.surfacesMap.keys())) {
      processor.processMessages([{ version: 'v0.9', deleteSurface: { surfaceId } }]);
    }
    if (messages.length > 0) {
      processor.processMessages(messages);
    }
    setSurfaces(Array.from(processor.model.surfacesMap.values()));
  }, []);

  if (processorRef.current === null) {
    processorRef.current = new MessageProcessor(
      [createServerAlignedCatalog()],
      async (action: A2uiClientAction) => {
        try {
          const result = await sendAction({ action });
          const processor = processorRef.current;
          if (!processor) return;

          const inbound = messagesWithoutDelete(result.messages ?? []) as unknown as A2uiMessage[];
          if (inbound.length > 0) {
            applyMessages(processor, inbound);
          }

          const actionResult = extractActionResult(result.messages);
          if (actionResult?.nextStep === 'approval') {
            setStoryStep('review');
            setRunStatus('Host persisted the draft and returned the approval surface.');
          } else if (actionResult?.status === 'approved' || actionResult?.status === 'rejected') {
            setStoryStep('decided');
            setRunStatus(
              actionResult.status === 'approved'
                ? `Change ${actionResult.changeId ?? ''} approved in the host ledger.`
                : `Change ${actionResult.changeId ?? ''} rejected — write was not applied.`,
            );
          }
        } catch (err) {
          console.error('Action failed:', err);
          setError(err instanceof Error ? err.message : 'Action failed');
        }
      },
    );
  }

  const processor = processorRef.current;

  useEffect(() => {
    void fetchDemoInfo()
      .then(setDemoInfo)
      .catch((err: unknown) => {
        setDemoInfoError(err instanceof Error ? err.message : 'Failed to load demo info');
      });
  }, []);

  useEffect(() => {
    const sync = () => setSurfaces(Array.from(processor.model.surfacesMap.values()));
    const createdSub = processor.onSurfaceCreated(sync);
    const deletedSub = processor.onSurfaceDeleted(sync);
    return () => {
      createdSub.unsubscribe();
      deletedSub.unsubscribe();
    };
  }, [processor]);

  const clearSurfaces = useCallback(() => {
    applyMessages(processor, []);
    setError(null);
    setRunStatus(null);
    setToolProgress(null);
    setStoryStep('idle');
  }, [processor, applyMessages]);

  const handleUtilizationEvent = useCallback((event: StreamUtilizationEvent) => {
    switch (event.type) {
      case 'runStarted':
        setRunStatus('Composing tonight’s change surface…');
        break;
      case 'runFinished':
        setRunStatus('Surface ready — continue the change in the card below.');
        setToolProgress(null);
        break;
      case 'runError':
        setRunStatus('Run failed');
        break;
      case 'toolProgress': {
        const payload = event.data as { toolName?: string; phase?: string };
        if (payload.toolName) {
          const phaseLabel = payload.phase === 'end' ? 'finished' : 'running';
          setToolProgress(`${payload.toolName} ${phaseLabel}`);
        }
        break;
      }
      default:
        break;
    }
  }, []);

  const generate = useCallback(async (content: string, context?: StreamContext) => {
    setLoading(true);
    setError(null);
    applyMessages(processor, []);
    setStoryStep('propose');

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
        handleUtilizationEvent,
        undefined,
        context,
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [processor, applyMessages, handleUtilizationEvent]);

  const startStory = () => {
    if (!demoInfo || loading) return;
    void generate(demoInfo.primaryPrompt, {
      intent: 'change_intake',
      instructions:
        'Tonight’s change: payments-api config deploy of payment-config v2.4. Prefill service, change type, and summary.',
    });
  };

  const generationMode = demoInfo?.generationMode ?? 'template';

  return (
    <div className="app">
      <header>
        <div className="header-row">
          <h1>{demoInfo?.productName ?? 'Ops Change Console'}</h1>
          <span className={`mode-pill mode-pill--${generationMode}`}>
            {generationMode === 'dynamic' ? 'Dynamic' : 'Template'}
          </span>
        </div>
        <h2 className="story-title">{demoInfo?.storyTitle ?? "Tonight's change window"}</h2>
        <p>
          {demoInfo?.storyBlurb
            ?? 'Propose a production change, then gate the write in your Spring host.'}
        </p>
        {demoInfoError && (
          <p className="demo-info-warning">
            Demo metadata unavailable ({demoInfoError}). Start the showcase host on port 5001.
          </p>
        )}
      </header>

      <ol className="story-steps">
        <li className={storyStep !== 'idle' ? 'is-active' : ''}>Propose</li>
        <li className={storyStep === 'review' || storyStep === 'decided' ? 'is-active' : ''}>Review</li>
        <li className={storyStep === 'decided' ? 'is-active' : ''}>Decide</li>
      </ol>

      <main>
        <div className="controls">
          <button type="button" onClick={startStory} disabled={loading || !demoInfo}>
            {loading ? 'Opening…' : (demoInfo?.primaryCta ?? "Open tonight's change")}
          </button>
          <button type="button" className="secondary-button" onClick={clearSurfaces} disabled={loading}>
            Reset
          </button>
        </div>

        {error && (
          <div className="a2ui-error">
            <h3>Error</h3>
            <p>{error}</p>
          </div>
        )}

        {(runStatus || toolProgress) && (
          <div className="a2ui-run-status">
            {runStatus && !isRawPayload(runStatus) && <p>{runStatus}</p>}
            {toolProgress && <p className="a2ui-tool-progress">{toolProgress}</p>}
          </div>
        )}

        <div className="surface-container">
          {surfaces.length === 0 ? (
            <div className="a2ui-empty">
              <p>Open tonight’s change to start the intake → approval loop.</p>
            </div>
          ) : (
            surfaces.map((surface) => (
              <A2uiSurface key={surface.id} surface={surface} />
            ))
          )}
        </div>
      </main>
    </div>
  );
}
