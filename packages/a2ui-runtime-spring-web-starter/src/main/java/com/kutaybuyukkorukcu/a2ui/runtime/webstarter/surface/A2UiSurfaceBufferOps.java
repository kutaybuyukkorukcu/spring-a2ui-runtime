package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiSurfaceBuffer;

public final class A2UiSurfaceBufferOps {

    private A2UiSurfaceBufferOps() {
    }

    public static void apply(A2UiSurfaceBuffer buffer, A2UiMessage message) {
        switch (message) {
            case A2UiMessage.SurfaceUpdate su -> buffer.applySurfaceUpdate(su);
            case A2UiMessage.DataModelUpdate dmu -> buffer.applyDataModelUpdate(dmu);
            case A2UiMessage.BeginRendering br -> {
                A2UiSurfaceBuffer.SurfaceState state = buffer.getOrCreateSurface(br.surfaceId());
                state.setRenderingBegun(true);
                state.setRootComponentId(br.root());
                state.setCatalogId(br.catalogId());
            }
            case A2UiMessage.DeleteSurface ds -> buffer.deleteSurface(ds.surfaceId());
        }
    }
}
