package com.kutaybuyukkorukcu.a2ui.runtime.webstarter.surface;

import com.kutaybuyukkorukcu.a2ui.runtime.protocol.A2UiMessage;
import com.kutaybuyukkorukcu.a2ui.runtime.surface.A2UiSurfaceBuffer;

public final class A2UiSurfaceBufferOps {

    private A2UiSurfaceBufferOps() {
    }

    public static void apply(A2UiSurfaceBuffer buffer, A2UiMessage message) {
        switch (message) {
            case A2UiMessage.CreateSurface cs -> buffer.applyCreateSurface(cs);
            case A2UiMessage.UpdateComponents uc -> buffer.applyUpdateComponents(uc);
            case A2UiMessage.UpdateDataModel udm -> buffer.applyUpdateDataModel(udm);
            case A2UiMessage.DeleteSurface ds -> buffer.deleteSurface(ds.surfaceId());
        }
    }
}
