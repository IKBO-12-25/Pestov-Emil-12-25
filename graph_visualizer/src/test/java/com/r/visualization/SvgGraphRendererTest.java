package com.r.visualization;

import com.r.graph.DependencyGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SvgGraphRendererTest {
    @Test
    void createsSvgWithNodesAndEdges() {
        DependencyGraph graph = new DependencyGraph(
                Map.of("A", List.of("B"), "B", List.of()), List.of()
        );

        String result = new SvgGraphRenderer().render(graph);

        assertTrue(result.contains("<svg"));
        assertTrue(result.contains("A</text>"));
        assertTrue(result.contains("B</text>"));
        assertTrue(result.contains("marker-end=\"url(#arrow)\""));
        assertTrue(result.contains("</svg>"));
    }
}
