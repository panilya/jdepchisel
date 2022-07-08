package com.panilya.jdepchisel.graph;

import com.panilya.jdepchisel.constantpool.ClassFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DependencyGraph {

    public final Map<String, DependencyGraphNode> processedClasses;

    public DependencyGraph(Map<String, DependencyGraphNode> processedClasses) {
        this.processedClasses = processedClasses;
    }

    public static class DependencyGraphNode {
        public ClassFile classFile;
        public boolean isProcessed;
        public List<DependencyGraphNode> dependsOn = new ArrayList<>();

        public DependencyGraphNode(ClassFile classFile) {
            this.classFile = classFile;
        }
    }

}
