package com.panilya.jdepchisel.constantpool;

import java.util.Set;

public class ClassFile {

    /**
     * Dependencies that this class has
     * @see ConstantPoolReader
     */
    public final Set<Class<?>> dependencies;
    public final String className;

    public ClassFile(Set<Class<?>> dependencies, String className) {
        this.dependencies = dependencies;
        this.className = className;
    }
}
