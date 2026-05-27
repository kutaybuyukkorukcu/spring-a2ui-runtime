const CATALOG_ID = 'https://a2ui.org/specification/v0_8/standard_catalog_definition.json';

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

export async function streamSurface(
  content: string,
  onMessage: (message: unknown) => void,
  onError: (error: string) => void,
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

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;

    buffer += decoder.decode(value, { stream: true });
    const lines = buffer.split('\n');
    buffer = lines.pop() || '';

    let isErrorEvent = false;

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) continue;

      if (trimmed.startsWith('event:')) {
        const eventType = trimmed.slice(6).trim();
        isErrorEvent = eventType === 'error';
        continue;
      }

      if (trimmed.startsWith('data:')) {
        const data = trimmed.slice(5).trim();
        if (data === '[DONE]') return;
        if (isErrorEvent) {
          try {
            const parsed = JSON.parse(data) as { error?: string; errorCode?: string };
            onError(parsed.error || parsed.errorCode || 'Stream error');
          } catch {
            onError(`Stream error: ${data}`);
          }
          isErrorEvent = false;
          return;
        }
        try {
          const message = JSON.parse(data);
          try {
            onMessage(message);
          } catch (processingError) {
            const detail = processingError instanceof Error ? processingError.message : String(processingError);
            onError(`Invalid A2UI message from server: ${detail}`);
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
  const response = await fetch('/a2ui/catalogs/standard-v0.8');
  if (!response.ok) {
    throw new Error(`Failed to fetch catalog: ${response.status}`);
  }
  return response.json();
}
