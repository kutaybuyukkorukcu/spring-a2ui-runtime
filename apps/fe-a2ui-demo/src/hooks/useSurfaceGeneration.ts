import { useState, useCallback } from 'react';
import { useA2UI } from '@a2ui/react/v0_8';
import type { A2UIClientEventMessage, ServerToClientMessage } from '@a2ui/react/v0_8';
import { generateSurface, streamSurface, sendAction } from '../services/api';

type Mode = 'sync' | 'stream';

export function useSurfaceGeneration() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [mode, setMode] = useState<Mode>('stream');
  const [activeSurfaceId, setActiveSurfaceId] = useState('main');
  const { processMessages, clearSurfaces } = useA2UI();

  const getSurfaceIdFromMessage = (message: unknown): string | null => {
    if (!message || typeof message !== 'object') return null;
    const msg = message as Record<string, unknown>;

    const surfaceUpdate = msg.surfaceUpdate as Record<string, unknown> | undefined;
    if (surfaceUpdate && typeof surfaceUpdate.surfaceId === 'string') {
      return surfaceUpdate.surfaceId;
    }

    const beginRendering = msg.beginRendering as Record<string, unknown> | undefined;
    if (beginRendering && typeof beginRendering.surfaceId === 'string') {
      return beginRendering.surfaceId;
    }

    const dataModelUpdate = msg.dataModelUpdate as Record<string, unknown> | undefined;
    if (dataModelUpdate && typeof dataModelUpdate.surfaceId === 'string') {
      return dataModelUpdate.surfaceId;
    }

    const deleteSurface = msg.deleteSurface as Record<string, unknown> | undefined;
    if (deleteSurface && typeof deleteSurface.surfaceId === 'string') {
      return deleteSurface.surfaceId;
    }

    return null;
  };

  const updateActiveSurfaceId = (messages: unknown[]) => {
    for (const message of messages) {
      const surfaceId = getSurfaceIdFromMessage(message);
      if (surfaceId) {
        setActiveSurfaceId(surfaceId);
      }
    }
  };

  const generate = useCallback(async (content: string) => {
    setLoading(true);
    setError(null);
    clearSurfaces();
    setActiveSurfaceId('main');

    try {
      if (mode === 'sync') {
        const response = await generateSurface(content);
        if (response.success && response.messages) {
          updateActiveSurfaceId(response.messages);
          processMessages(response.messages as ServerToClientMessage[]);
        } else {
          setError(response.error || 'Generation failed');
        }
      } else {
        await streamSurface(
          content,
          (message) => {
            updateActiveSurfaceId([message]);
            processMessages([message as ServerToClientMessage]);
          },
          (err) => {
            setError(err);
          },
        );
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  }, [mode, processMessages, clearSurfaces]);

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
    setActiveSurfaceId('main');
    setError(null);
  }, [clearSurfaces]);

  return { loading, error, mode, setMode, generate, clear, handleAction, activeSurfaceId };
}