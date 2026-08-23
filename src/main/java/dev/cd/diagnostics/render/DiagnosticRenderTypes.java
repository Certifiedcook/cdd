package dev.cd.diagnostics.render;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public final class DiagnosticRenderTypes {
    private static final RenderPipeline NO_DEPTH_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
                    .withLocation(Identifier.fromNamespaceAndPath("cd_diagnostics", "pipeline/diagnostic_lines_no_depth"))
                    .withDepthStencilState(Optional.empty())
                    .build()
    );

    public static final RenderType NO_DEPTH_LINES = RenderType.create(
            "cd_diagnostics_no_depth_lines",
            RenderSetup.builder(NO_DEPTH_LINES_PIPELINE).createRenderSetup()
    );

    private DiagnosticRenderTypes() {
    }

    public static void bootstrap() {
    }
}
