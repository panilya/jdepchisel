package com.panilya.jdepchisel.constantpool;

import java.util.Set;

public class ClassFile {

    /**
     * Dependencies that this class has
     * @see ConstantPoolReader
     */
    private final Set<Class<?>> dependencies;
    private final String className;

    public ClassFile(Set<Class<?>> dependencies, String className) {
        this.dependencies = dependencies;
        this.className = className;
    }

    public String getClassName() {
        return className;
    }

    public Set<Class<?>> getDependencies() {
        return dependencies;
    }
}
