package com.r.visualization;

import com.r.graph.DependencyGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SvgGraphRenderer {
    private static final int NODE_WIDTH = 150;
    private static final int NODE_HEIGHT = 54;
    private static final int HORIZONTAL_GAP = 100;
    private static final int VERTICAL_GAP = 36;
    private static final int MARGIN = 40;

    /** Создаёт автономное SVG-изображение графа без внешних библиотек. */
    public String render(DependencyGraph graph) {
        if (graph.adjacencyList().isEmpty()) {
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"120\" "
                    + "height=\"80\" viewBox=\"0 0 120 80\"></svg>\n";
        }
        Map<String, Integer> levels = calculateLevels(graph);
        Map<Integer, List<String>> columns = groupByLevel(levels);
        Map<String, Point> points = calculatePoints(columns);
        int width = calculateWidth(columns.size());
        int height = calculateHeight(columns);

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"")
                .append(width).append("\" height=\"").append(height)
                .append("\" viewBox=\"0 0 ").append(width).append(' ')
                .append(height).append("\">\n");
        appendDefinitions(svg);
        appendEdges(svg, graph, points);
        appendNodes(svg, graph, points);
        return svg.append("</svg>\n").toString();
    }

    private String findRoot(DependencyGraph graph) {
        List<String> dependencies = graph.adjacencyList().values().stream()
                .flatMap(List::stream)
                .toList();
        return graph.adjacencyList().keySet().stream()
                .filter(name -> !dependencies.contains(name))
                .findFirst()
                .orElseGet(() -> graph.adjacencyList().keySet().iterator().next());
    }

    private Map<String, Integer> calculateLevels(DependencyGraph graph) {
        Map<String, Integer> levels = new LinkedHashMap<>();
        ArrayDeque<String> queue = new ArrayDeque<>();
        String root = findRoot(graph);
        levels.put(root, 0);
        queue.add(root);

        while (!queue.isEmpty()) {
            String current = queue.remove();
            for (String dependency : graph.adjacencyList().getOrDefault(current, List.of())) {
                if (!levels.containsKey(dependency)) {
                    levels.put(dependency, levels.get(current) + 1);
                    queue.add(dependency);
                }
            }
        }
        return levels;
    }

    private Map<Integer, List<String>> groupByLevel(Map<String, Integer> levels) {
        Map<Integer, List<String>> columns = new LinkedHashMap<>();
        levels.forEach((name, level) ->
                columns.computeIfAbsent(level, ignored -> new ArrayList<>()).add(name));
        return columns;
    }

    private Map<String, Point> calculatePoints(Map<Integer, List<String>> columns) {
        Map<String, Point> points = new HashMap<>();
        columns.forEach((level, names) -> {
            for (int index = 0; index < names.size(); index++) {
                int x = MARGIN + level * (NODE_WIDTH + HORIZONTAL_GAP);
                int y = MARGIN + index * (NODE_HEIGHT + VERTICAL_GAP);
                points.put(names.get(index), new Point(x, y));
            }
        });
        return points;
    }

    private int calculateWidth(int levels) {
        return MARGIN * 2 + Math.max(1, levels) * NODE_WIDTH
                + Math.max(0, levels - 1) * HORIZONTAL_GAP;
    }

    private int calculateHeight(Map<Integer, List<String>> columns) {
        int maxNodes = columns.values().stream().mapToInt(List::size).max().orElse(1);
        return MARGIN * 2 + maxNodes * NODE_HEIGHT
                + Math.max(0, maxNodes - 1) * VERTICAL_GAP;
    }

    private void appendDefinitions(StringBuilder svg) {
        svg.append("<defs><marker id=\"arrow\" markerWidth=\"10\" markerHeight=\"10\" "
                + "refX=\"8\" refY=\"3\" orient=\"auto\">")
                .append("<path d=\"M0,0 L0,6 L9,3 z\" fill=\"#526477\"/>")
                .append("</marker></defs>\n")
                .append("<rect width=\"100%\" height=\"100%\" fill=\"#f7f9fc\"/>\n");
    }

    private void appendEdges(
            StringBuilder svg, DependencyGraph graph, Map<String, Point> points
    ) {
        graph.adjacencyList().forEach((name, dependencies) -> {
            Point source = points.get(name);
            for (String dependency : dependencies) {
                Point target = points.get(dependency);
                if (source == null || target == null) {
                    continue;
                }
                int startX = source.x() + NODE_WIDTH;
                int startY = source.y() + NODE_HEIGHT / 2;
                int endX = target.x();
                int endY = target.y() + NODE_HEIGHT / 2;
                svg.append("<path d=\"M ").append(startX).append(' ').append(startY)
                        .append(" L ").append(endX).append(' ').append(endY)
                        .append("\" stroke=\"#526477\" stroke-width=\"2\" fill=\"none\" "
                                + "marker-end=\"url(#arrow)\"/>\n");
            }
        });
    }

    private void appendNodes(
            StringBuilder svg, DependencyGraph graph, Map<String, Point> points
    ) {
        points.forEach((name, point) -> {
            boolean root = findRoot(graph).equals(name);
            String fill = root ? "#dceafe" : "#ffffff";
            svg.append("<rect x=\"").append(point.x()).append("\" y=\"")
                    .append(point.y()).append("\" width=\"").append(NODE_WIDTH)
                    .append("\" height=\"").append(NODE_HEIGHT)
                    .append("\" rx=\"10\" fill=\"").append(fill)
                    .append("\" stroke=\"#526477\" stroke-width=\"1.5\"/>\n")
                    .append("<text x=\"").append(point.x() + NODE_WIDTH / 2)
                    .append("\" y=\"").append(point.y() + 33)
                    .append("\" text-anchor=\"middle\" font-family=\"Arial, sans-serif\" "
                            + "font-size=\"16\" fill=\"#172b4d\">")
                    .append(escapeXml(name)).append("</text>\n");
        });
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private record Point(int x, int y) {
    }
}
