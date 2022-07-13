package com.panilya.jdepchisel.graph;

import com.panilya.jdepchisel.constantpool.ClassFile;
import com.panilya.jdepchisel.constantpool.ConstantPoolReader;

import java.io.IOException;
import java.util.*;

public class DependencyGraphCreator {

    public DependencyGraph generate(String rootClass) {
        List<DependencyGraph.DependencyGraphNode> classesToProcess = new ArrayList<>();
        Map<String, DependencyGraph.DependencyGraphNode> processedClasses = new HashMap<>();

        Set<Class<?>> classes = listDependencies(rootClass);
        classesToProcess.add(new DependencyGraph.DependencyGraphNode(new ClassFile(classes, rootClass)));

        while (classesToProcess.size() > 0) {
            DependencyGraph.DependencyGraphNode graphNode = classesToProcess.remove(classesToProcess.size() - 1);

            if (graphNode.isProcessed) continue;

            setIsProcessed(graphNode);

            Set<Class<?>> dependencies = listDependencies(graphNode.classFile.getClassName());

            for (Class<?> dependency : dependencies) {
                if (dependency.getName().contains("$")) continue; // Very bad smell. TODO: implement support for inner classes
                if (!dependency.getName().startsWith("com.panilya")) continue; // Isn't better smell
                DependencyGraph.DependencyGraphNode classNode = checkClassNode(dependency.getName(), processedClasses);

                if (classNode.classFile.getClassName().equals(graphNode.classFile.getClassName())) continue;

                if (!classNode.isProcessed) {
                    classesToProcess.add(classNode);
                }
                graphNode.dependsOn.add(classNode);
            }
        }

        return new DependencyGraph(processedClasses);
    }

    private void setIsProcessed(DependencyGraph.DependencyGraphNode node) {
        node.isProcessed = true;
    }

    private DependencyGraph.DependencyGraphNode checkClassNode(String className, Map<String, DependencyGraph.DependencyGraphNode> processedClasses) {
        if (processedClasses.containsKey(className)) return processedClasses.get(className);
        Set<Class<?>> dependencies = listDependencies(className);
        DependencyGraph.DependencyGraphNode dependencyGraphNode = new DependencyGraph.DependencyGraphNode(new ClassFile(dependencies, className));
        processedClasses.put(className, dependencyGraphNode);
        return dependencyGraphNode;
    }

    private Set<Class<?>> listDependencies(String className) {
        try {
            return ConstantPoolReader.getDependencies(Class
                    .forName(className));
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
}
