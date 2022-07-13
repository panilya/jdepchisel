package com.panilya.jdepchisel.graph;

import com.panilya.jdepchisel.constantpool.ClassFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DependencyGraph {

    private final Map<String, DependencyGraphNode> processedClasses;

    public DependencyGraph(Map<String, DependencyGraphNode> processedClasses) {
        this.processedClasses = processedClasses;
    }

    public Map<String, DependencyGraphNode> getProcessedClasses() {
        return processedClasses;
    }

    public static class DependencyGraphNode {
        private final ClassFile classFile;
        private boolean isProcessed;
        private List<DependencyGraphNode> dependsOn = new ArrayList<>();

        public DependencyGraphNode(ClassFile classFile) {
            this.classFile = classFile;
        }

        public ClassFile getClassFile() {
            return classFile;
        }

        public boolean isProcessed() {
            return isProcessed;
        }

        public void setProcessed(boolean processed) {
            isProcessed = processed;
        }

        public List<DependencyGraphNode> getDependsOn() {
            return dependsOn;
        }
    }
}
