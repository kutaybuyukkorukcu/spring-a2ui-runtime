/// <reference types="vite/client" />

interface ImportMetaEnv {
  readonly VITE_A2UI_GENERATION_MODE?: 'template' | 'dynamic';
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}