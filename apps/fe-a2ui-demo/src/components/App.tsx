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
  openAssembledRecord,
  extractActionResult,
  messagesWithoutDelete,
  applyDataModelSeeds,
  resolveActionContext,
  type StreamUtilizationEvent,
  type DemoInfo,
  type DemoRecord,
  type LedgerEntry,
} from '../services/api';
import '@a2ui/react-v0_9-css';

const SERVER_CATALOG_ID = 'https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json';

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

function ledgerFromAction(result: ReturnType<typeof extractActionResult>): LedgerEntry | null {
  if (!result?.changeId || !result.status) return null;
  const status = result.status === 'approved' || result.status === 'rejected'
    ? result.status.toUpperCase()
    : result.status;
  return {
    id: result.changeId,
    status,
    service: result.service,
    changeType: result.changeType,
  };
}

function mergeLedger(current: LedgerEntry[], incoming: LedgerEntry): LedgerEntry[] {
  const rest = current.filter((row) => row.id !== incoming.id);
  return [incoming, ...rest];
}

export function App() {
  const [demoInfo, setDemoInfo] = useState<DemoInfo | null>(null);
  const [demoInfoError, setDemoInfoError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [islandCaption, setIslandCaption] = useState<string | null>(null);
  const [runStatus, setRunStatus] = useState<string | null>(null);
  const [toolProgress, setToolProgress] = useState<string | null>(null);
  const [ledger, setLedger] = useState<LedgerEntry[]>([]);
  const [surfaces, setSurfaces] = useState<SurfaceModel<ReactComponentImplementation>[]>([]);
  const processorRef = useRef<MessageProcessor<ReactComponentImplementation> | null>(null);

  const refreshLedger = useCallback(async () => {
    try {
      const info = await fetchDemoInfo();
      setLedger(info.ledger ?? []);
    } catch {
      // Host page already has the last action result; skip a failed refresh.
    }
  }, []);

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
          const processor = processorRef.current;
          const resolved = processor
            ? resolveActionContext(action, (surfaceId, path) => {
                const surface = processor.model.surfacesMap.get(surfaceId)
                  ?? Array.from(processor.model.surfacesMap.values())[0];
                return surface?.dataModel.get(path);
              })
            : action;
          const result = await sendAction({ action: resolved });
          if (!processor) return;

          const inbound = messagesWithoutDelete(result.messages ?? []) as unknown as A2uiMessage[];
          if (inbound.length > 0) {
            applyMessages(processor, inbound);
          }

          const actionResult = extractActionResult(result.messages);
          const row = ledgerFromAction(actionResult);
          if (row) {
            setLedger((current) => mergeLedger(current, row));
          }
          void refreshLedger();

          if (actionResult?.nextStep === 'approval') {
            setRunStatus('Host persisted the draft and returned the approval surface.');
          } else if (actionResult?.status === 'approved' || actionResult?.status === 'rejected') {
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
      .then((info) => {
        setDemoInfo(info);
        setLedger(info.ledger ?? []);
      })
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
    setSelectedId(null);
    setIslandCaption(null);
  }, [processor, applyMessages]);

  const handleUtilizationEvent = useCallback((event: StreamUtilizationEvent) => {
    switch (event.type) {
      case 'runStarted':
        setRunStatus('Composing island for this case…');
        break;
      case 'runFinished':
        setRunStatus('Island ready.');
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

  const composeRecord = useCallback(async (record: DemoRecord) => {
    const content = record.content;
    if (!content) {
      setError('Composed record is missing case content.');
      return;
    }
    setLoading(true);
    setError(null);
    applyMessages(processor, []);

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
        {
          intent: 'case_island',
          instructions: record.instructions,
        },
      );

      if (record.dataModelSeeds) {
        processor.processMessages(
          applyDataModelSeeds(record.dataModelSeeds) as unknown as A2uiMessage[],
        );
        setSurfaces(Array.from(processor.model.surfacesMap.values()));
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [processor, applyMessages, handleUtilizationEvent]);

  const openRecord = useCallback(async (record: DemoRecord) => {
    if (loading) return;
    setSelectedId(record.id);
    setIslandCaption(record.caption);
    setError(null);
    setToolProgress(null);
    setRunStatus(null);
    applyMessages(processor, []);

    if (record.surfaceKind === 'assembled') {
      setLoading(true);
      try {
        const opened = await openAssembledRecord(record.id);
        applyMessages(processor, opened.messages as A2uiMessage[]);
        setIslandCaption(opened.caption);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to open record');
      } finally {
        setLoading(false);
      }
      return;
    }

    await composeRecord(record);
  }, [loading, processor, applyMessages, composeRecord]);

  const records = demoInfo?.records ?? [];
  const selectedRecord = records.find((record) => record.id === selectedId);

  return (
    <div className="app">
      <header>
        <h1>{demoInfo?.productName ?? 'payments-api workspace'}</h1>
        <h2 className="story-title">{demoInfo?.storyTitle ?? 'Your page, one slot'}</h2>
        <p>
          {demoInfo?.storyBlurb
            ?? 'This workspace is the product you own. The island is the only region that speaks A2UI.'}
        </p>
        {demoInfoError && (
          <p className="demo-info-warning">
            Demo metadata unavailable ({demoInfoError}). Start the showcase host on port 5001.
          </p>
        )}
      </header>

      <div className="workspace">
        <aside className="record-panel">
          <h3>Records</h3>
          <ul className="record-list">
            {records.map((record) => (
              <li key={record.id}>
                <button
                  type="button"
                  className={`record-row${selectedId === record.id ? ' is-selected' : ''}`}
                  onClick={() => void openRecord(record)}
                  disabled={loading || !demoInfo}
                  aria-current={selectedId === record.id ? 'true' : undefined}
                >
                  <span className="record-id">{record.id}</span>
                  <span className="record-title">{record.title}</span>
                  <span className="record-flags">{record.flags.join(' · ')}</span>
                  <span className="record-kind">{record.surfaceKind}</span>
                </button>
              </li>
            ))}
          </ul>
          <button type="button" className="secondary-button reset-button" onClick={clearSurfaces} disabled={loading}>
            Reset
          </button>
        </aside>

        <section className="island" aria-label={demoInfo?.islandLabel ?? 'GenUI slot'}>
          <div className="island-header">
            <span className="island-label">{demoInfo?.islandLabel ?? 'GenUI slot'}</span>
            {islandCaption && <span className="island-caption">{islandCaption}</span>}
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
                <p>
                  {loading
                    ? (selectedRecord?.surfaceKind === 'composed'
                      ? 'Composing island for this case…'
                      : 'Filling slot…')
                    : 'Select a record. This slot is the only region that speaks A2UI.'}
                </p>
              </div>
            ) : (
              surfaces.map((surface) => (
                <A2uiSurface key={surface.id} surface={surface} />
              ))
            )}
          </div>
        </section>
      </div>

      <section className="ledger" aria-label="Host ledger">
        <h3>Ledger</h3>
        {ledger.length === 0 ? (
          <p className="ledger-empty">No writes yet. Submitting from the island persists here — in this Spring host.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Id</th>
                <th>Status</th>
                <th>Service</th>
                <th>Type</th>
              </tr>
            </thead>
            <tbody>
              {ledger.map((row) => (
                <tr key={row.id}>
                  <td><code>{row.id}</code></td>
                  <td>{row.status}</td>
                  <td>{row.service ?? '—'}</td>
                  <td>{row.changeType ?? '—'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}
