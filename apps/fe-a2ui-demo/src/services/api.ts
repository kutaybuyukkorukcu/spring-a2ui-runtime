export interface StreamUtilizationEvent {
  type: string;
  data: unknown;
}

interface A2UiSurfaceRequest {
  content: string;
  context?: {
    intent?: string;
    preferredComponents?: string[];
    instructions?: string;
  };
  a2uiClientCapabilities: {
    supportedCatalogIds: string[];
    inlineCatalogs?: Record<string, unknown>[];
  };
}

const CATALOG_ID = 'https://a2ui.org/specification/v0_9/catalogs/basic/catalog.json';

const A2UI_SURFACE_EVENTS = new Set([
  'createSurface',
  'updateComponents',
  'updateDataModel',
  'deleteSurface',
]);

export async function streamSurface(
  content: string,
  onMessage: (message: unknown) => void,
  onError: (error: string) => void,
  onUtilizationEvent?: (event: StreamUtilizationEvent) => void,
  signal?: AbortSignal,
): Promise<void> {
  const request: A2UiSurfaceRequest = {
    content,
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
    signal,
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

export async function fetchCatalog(): Promise<Record<string, unknown>> {
  const response = await fetch('/a2ui/catalogs/basic-v0.9');
  if (!response.ok) {
    throw new Error(`Failed to fetch catalog: ${response.status}`);
  }
  return response.json();
}
