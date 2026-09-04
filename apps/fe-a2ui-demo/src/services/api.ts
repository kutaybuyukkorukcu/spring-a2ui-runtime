export interface StreamUtilizationEvent {
  type: string;
  data: unknown;
}

export interface StreamContext {
  intent?: string;
  preferredComponents?: string[];
  instructions?: string;
}

interface A2UiSurfaceRequest {
  content: string;
  context?: StreamContext;
  a2uiClientCapabilities: {
    supportedCatalogIds: string[];
    inlineCatalogs?: Record<string, unknown>[];
  };
}

export type SurfaceKind = 'assembled' | 'composed';

export interface DemoRecord {
  id: string;
  title: string;
  kind: 'known' | 'unknown';
  flags: string[];
  surfaceKind: SurfaceKind;
  caption: string;
  content?: string;
  instructions?: string;
  /** Case-known facts the host seeds after compose so submit_change context resolves. */
  dataModelSeeds?: Record<string, string>;
}

export interface LedgerEntry {
  id: string;
  status: string;
  service?: string;
  changeType?: string;
}

export interface DemoInfo {
  productName: string;
  generationMode: 'template' | 'dynamic';
  storyTitle: string;
  storyBlurb: string;
  islandLabel: string;
  records: DemoRecord[];
  ledger: LedgerEntry[];
}

export interface ActionResultPayload {
  action?: string;
  status?: string;
  changeId?: string;
  service?: string;
  changeType?: string;
  summary?: string;
  nextStep?: string;
}

const CATALOG_ID = 'https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json';

const A2UI_SURFACE_EVENTS = new Set([
  'createSurface',
  'updateComponents',
  'updateDataModel',
  'deleteSurface',
]);

export async function fetchDemoInfo(): Promise<DemoInfo> {
  const response = await fetch('/api/demo/info');
  if (!response.ok) {
    throw new Error(`Failed to load demo info: ${response.status}`);
  }
  return response.json() as Promise<DemoInfo>;
}

export async function streamSurface(
  content: string,
  onMessage: (message: unknown) => void,
  onError: (error: string) => void,
  onUtilizationEvent?: (event: StreamUtilizationEvent) => void,
  context?: StreamContext,
): Promise<void> {
  const request: A2UiSurfaceRequest = {
    content,
    context,
    a2uiClientCapabilities: {
      supportedCatalogIds: [CATALOG_ID],
    },
  };

  const response = await fetch('/a2ui/surface/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Request-Id': crypto.randomUUID(),
    },
    body: JSON.stringify(request),
  });

  if (!response.ok) {
    const errorBody = await response.json().catch(() => ({ error: response.statusText }));
    throw new Error(errorBody.error || `Stream failed: ${response.status}`);
  }

  const reader = response.body?.getReader();
  if (!reader) throw new Error('No response body');

  const decoder = new TextDecoder();
  let buffer = '';
  let currentEventType = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) continue;

      if (trimmed.startsWith('event:')) {
        currentEventType = trimmed.slice(6).trim();
        continue;
      }

      if (trimmed.startsWith('data:')) {
        const data = trimmed.slice(5).trim();
        if (data === '[DONE]') return;

        if (currentEventType === 'error') {
          try {
            const parsed = JSON.parse(data) as { error?: string; errorCode?: string };
            onError(parsed.error || parsed.errorCode || 'Stream error');
          } catch {
            onError(`Stream error: ${data}`);
          }
          return;
        }

        try {
          const parsed = JSON.parse(data);
          if (A2UI_SURFACE_EVENTS.has(currentEventType)) {
            try {
              onMessage(parsed);
            } catch (processingError) {
              const detail = processingError instanceof Error ? processingError.message : String(processingError);
              onError(`Invalid A2UI message from server: ${detail}`);
            }
          } else if (onUtilizationEvent && currentEventType) {
            onUtilizationEvent({ type: currentEventType, data: parsed });
          }
        } catch {
          onError(`Failed to parse SSE JSON: ${data}`);
        }
      }
    }
  }
}

/**
 * A2UI DynamicValue strings are literals. A planner often emits "notes": "/notes"
 * meaning a path; the client must look that up in the surface data model or the
 * host receives the path string instead of the typed text.
 */
export function isJsonPointerString(value: unknown): value is string {
  return typeof value === 'string' && /^\/[A-Za-z0-9_-]+(?:\/[A-Za-z0-9_-]+)*$/.test(value);
}

export function resolveContextValue(
  value: unknown,
  getValue: (path: string) => unknown,
): unknown {
  if (value && typeof value === 'object' && !Array.isArray(value)) {
    const path = (value as { path?: unknown }).path;
    if (typeof path === 'string') {
      const resolved = getValue(path);
      return resolved === undefined ? '' : resolved;
    }
  }
  if (isJsonPointerString(value)) {
    const resolved = getValue(value);
    return resolved === undefined ? '' : resolved;
  }
  return value;
}

export function resolveActionContext<T extends { surfaceId?: string; context?: Record<string, unknown> }>(
  action: T,
  getValue: (surfaceId: string, path: string) => unknown,
): T {
  const context = action.context;
  if (!context || typeof context !== 'object') {
    return action;
  }
  const surfaceId = action.surfaceId ?? 'main';
  const resolved: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(context)) {
    resolved[key] = resolveContextValue(value, (path) => getValue(surfaceId, path));
  }
  return { ...action, context: resolved };
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export async function sendAction(event: any): Promise<{ accepted: boolean; messages?: unknown[]; eventType?: string; errorCode?: string }> {
  const response = await fetch('/a2ui/actions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Request-Id': crypto.randomUUID(),
    },
    body: JSON.stringify(event),
  });

  return response.json();
}

type A2uiLikeMessage = Record<string, unknown>;

export function extractActionResult(messages: unknown[] | undefined): ActionResultPayload | null {
  if (!messages) return null;
  for (const message of messages) {
    const update = message as { updateDataModel?: { path?: string; value?: ActionResultPayload } };
    if (update.updateDataModel?.path === '/actionResult') {
      return update.updateDataModel.value ?? null;
    }
  }
  return null;
}

/** Apply host-provided case facts without wiping fields the user may already have typed. */
export function applyDataModelSeeds(
  seeds: Record<string, string>,
  surfaceId = 'main',
): A2uiLikeMessage[] {
  return Object.entries(seeds).map(([key, value]) => ({
    version: 'v0.9',
    updateDataModel: {
      surfaceId,
      path: `/${key}`,
      value,
    },
  }));
}

export function messagesWithoutDelete(messages: unknown[]): A2uiLikeMessage[] {
  return messages.filter((message) => {
    const candidate = message as { deleteSurface?: unknown };
    return candidate.deleteSurface == null;
  }) as A2uiLikeMessage[];
}
