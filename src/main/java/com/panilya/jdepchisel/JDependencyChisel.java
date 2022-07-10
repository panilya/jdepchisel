package com.panilya.jdepchisel;

import com.panilya.jdepchisel.graph.DependencyGraph;
import com.panilya.jdepchisel.graph.DependencyGraphCreator;

public class JDependencyChisel {

    public static void main(String[] args) {
        try {
            DependencyGraphCreator graphCreator = new DependencyGraphCreator();
            DependencyGraph dependencyGraph = graphCreator.generate("com.panilya.jdepchisel.JDependencyChisel"); // Enter root class e.g. entry point for deps searching
            System.out.println("digraph classDependencies {");
            for (var classNodeEntry : dependencyGraph.getProcessedClasses().entrySet()) {
                var className = classNodeEntry.getKey();
                var classValue = classNodeEntry.getValue().dependsOn;
                for (var depends : classValue) {
                    // Prints output in DOT language in order to display Dependency Graph in Graphviz
                    System.out.println("\"" + className + "\" -> \"" + depends.classFile.getClassName() + "\";");
                }
            }
            System.out.println("}");
            System.out.println("https://sketchviz.com/new" + "Graphviz Online Visualizer in order to see graph");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
