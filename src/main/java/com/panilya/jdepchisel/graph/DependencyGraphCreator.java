package com.panilya.jdepchisel.graph;

import com.panilya.jdepchisel.constantpool.ClassFile;
import com.panilya.jdepchisel.constantpool.ConstantPoolReader;

import java.io.IOException;
import java.util.*;

public class DependencyGraphCreator {

    public DependencyGraph generate(String rootClass) {
        List<DependencyGraph.DependencyGraphNode> classesToProcess = new ArrayList<>();
        Map<String, DependencyGraph.DependencyGraphNode> processedClasses = new HashMap<>();

        Set<Class<?>> classes = getDependencies(rootClass);
        classesToProcess.add(new DependencyGraph.DependencyGraphNode(new ClassFile(classes, rootClass)));

        while (classesToProcess.size() > 0) {
            DependencyGraph.DependencyGraphNode graphNode = classesToProcess.remove(classesToProcess.size() - 1);

            if (graphNode.isProcessed) continue;

            graphNode.isProcessed = true;

            Set<Class<?>> dependencies = getDependencies(graphNode.classFile.className);

            for (Class<?> dependency : dependencies) {
                if (dependency.getName().contains("$")) continue; // Very bad smell
                if (!dependency.getName().startsWith("com.panilya")) continue; // Isn't better smell
                DependencyGraph.DependencyGraphNode classNode = checkClassNode(dependency.getName(), processedClasses);

                if (classNode.classFile.className.equals(graphNode.classFile.className)) continue;

                if (!classNode.isProcessed) {
                    classesToProcess.add(classNode);
                }
                graphNode.dependsOn.add(classNode);
            }
        }

        return new DependencyGraph(processedClasses);
    }

    private DependencyGraph.DependencyGraphNode checkClassNode(String className, Map<String, DependencyGraph.DependencyGraphNode> processedClasses) {
        if (processedClasses.containsKey(className)) return processedClasses.get(className);
        Set<Class<?>> dependencies = getDependencies(className);
        DependencyGraph.DependencyGraphNode dependencyGraphNode = new DependencyGraph.DependencyGraphNode(new ClassFile(dependencies, className));
        processedClasses.put(className, dependencyGraphNode);
        return dependencyGraphNode;
    }

    private Set<Class<?>> getDependencies(String className) {
        try {
            return ConstantPoolReader.getDependencies(Class
                    .forName(className));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
