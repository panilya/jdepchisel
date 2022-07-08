package com.panilya.jdepchisel;

import com.panilya.jdepchisel.constantpool.ConstantPoolReader;

import java.util.Set;

public class JDependencyChisel {

    public static void main(String[] args) {
        try {
            // Get dependencies for my class:
            Set<Class<?>> dependencies = ConstantPoolReader.getDependencies(Class
                    .forName("com.panilya.jdepchisel.JDependencyChisel"));

            // Print the full class name for each interesting dependency:
            dependencies
                    .stream()
                    .filter(clazz -> !clazz.getCanonicalName().startsWith(
                            "java.lang")) // do not show java.lang dependencies,
                    // which add clutter
                    .forEach(c -> System.out.println(c.getCanonicalName()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
