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

export async function generateSurface(content: string, signal?: AbortSignal): Promise<{ success: boolean; messages: unknown[]; error?: string; errorCode?: string }> {
  const request: A2UiSurfaceRequest = {
    content,
    a2uiClientCapabilities: {
      supportedCatalogIds: [CATALOG_ID],
    },
  };

  const response = await fetch('/a2ui/surface', {
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
    throw new Error(errorBody.error || `Surface generation failed: ${response.status}`);
  }

  return response.json();
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

    for (const line of lines) {
      const trimmed = line.trim();
      if (!trimmed) continue;

      if (trimmed.startsWith('data:')) {
        const data = trimmed.slice(5).trim();
        if (data === '[DONE]') return;
        try {
          const message = JSON.parse(data);
          onMessage(message);
        } catch {
          onError(`Failed to parse SSE message: ${data}`);
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