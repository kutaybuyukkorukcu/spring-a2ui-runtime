package com.kutaybuyukkorukcu.a2ui.showcase.demo;

import java.util.List;

public record DemoInfoResponse(
    String productName,
    String generationMode,
    String storyTitle,
    String storyBlurb,
    String primaryCta,
    String primaryPrompt,
    List<String> samplePrompts) {}
