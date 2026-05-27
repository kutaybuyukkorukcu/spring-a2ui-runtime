import { useState, useCallback } from 'react';
import { useA2UIActions } from '@a2ui/react/v0_8';
import type { A2UIClientEventMessage, ServerToClientMessage } from '@a2ui/react/v0_8';
import { streamSurface, sendAction } from '../services/api';

/** Must be called from a component rendered inside {@link A2UIProvider}. */
export function useSurfaceGeneration() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const { processMessages, clearSurfaces } = useA2UIActions();

  const generate = useCallback(async (content: string) => {
    setLoading(true);
    setError(null);
    clearSurfaces();

    try {
      await streamSurface(
        content,
        (message) => {
          processMessages([message as ServerToClientMessage]);
        },
        (err) => {
          setError(err);
        },
      );
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [processMessages, clearSurfaces]);

  const handleAction = useCallback(async (event: A2UIClientEventMessage) => {
    try {
      const result = await sendAction(event);
      if (result.messages && result.messages.length > 0) {
        processMessages(result.messages as ServerToClientMessage[]);
      }
    } catch (err) {
      console.error('Action failed:', err);
    }
  }, [processMessages]);

  const clear = useCallback(() => {
    clearSurfaces();
    setError(null);
  }, [clearSurfaces]);

  return { loading, error, generate, clear, handleAction };
}
