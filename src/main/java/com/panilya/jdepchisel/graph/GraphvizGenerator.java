package com.panilya.jdepchisel.graph;

public class GraphvizGenerator {

    public static void generateOutput(String rootClassName) {
        try {
            DependencyGraphCreator graphCreator = new DependencyGraphCreator();
            DependencyGraph dependencyGraph = graphCreator.generate(rootClassName); // Enter root class e.g. entry point for deps searching

            StringBuilder stringBuilder = new StringBuilder();

            stringBuilder.append("digraph classDependencies {" + "\n");
            for (var classNodeEntry : dependencyGraph.getProcessedClasses().entrySet()) {
                var className = classNodeEntry.getKey();
                var classValue = classNodeEntry.getValue().dependsOn;
                for (var depends : classValue) {
                    // Prints output in DOT language in order to display Dependency Graph in Graphviz
                    stringBuilder.append("  \"").append(className).append("\" -> \"")
                            .append(depends.classFile.getClassName()).append("\";").append("\n");
                }
            }
            stringBuilder.append("}").append("\n").append("https://sketchviz.com/new" + "Graphviz Online Visualizer in order to see graph");
            System.out.println(stringBuilder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
