package com.panilya.jdepchisel;

import com.panilya.jdepchisel.constantpool.ConstantPoolReader;
import com.panilya.jdepchisel.graph.DependencyGraph;
import com.panilya.jdepchisel.graph.DependencyGraphCreator;

import java.util.Map;

public class JDependencyChisel {

    public static void main(String[] args) {
        try {
            DependencyGraphCreator graphCreator = new DependencyGraphCreator();
            DependencyGraph dependencyGraph = graphCreator.generate("com.panilya.jdepchisel.graph.DependencyGraph"); // Enter root class e.g. entry point for deps searching
            for (var classNodeEntry : dependencyGraph.processedClasses.entrySet()) {
                var className = classNodeEntry.getKey();
                var classValue = classNodeEntry.getValue().dependsOn;
                for (var depends : classValue) {
                    // Prints output in DOT language in order to display Dependency Graph in Graphviz
                    System.out.println("\"" + className + "\" -> \"" + depends.classFile.className + "\";");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
