package ru.ifmo.ctddev.shah.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.HashMap;
import java.util.Map;

public class Implementor implements Impler {

    private HashMap<String, MethodOnSteroids> methods = new HashMap<>();

    @Override
    public void implement(final Class<?> token, final File root) throws ImplerException {
        try {
            writeImplementation(token, root);
        } finally {
            methods.clear();
        }
    }

    public static void main(final String[] args) {
        if (args != null && args.length == 1 && args[0] != null) {
            try {
                getInstance().implement(Class.forName(args[0]), new File("/home/sultan/Documents/Year2/java/hw3/src"));
            } catch (ImplerException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else {
            System.err.println("Invalid usage");
        }
    }

    private static Implementor getInstance() {
        return new Implementor();
    }

    private File createDirs(final Package pack, final File root) throws ImplerException {
        if (pack == null) {
            return root;
        }
        String packagePath = pack.getName().replaceAll("\\.", File.separator);
        File dir = new File(root.getPath() + File.separator + packagePath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new ImplerException("Cannot create dirs");
            }
        }
        return dir;
    }

    private void writeImplementation(final Class<?> loadedClass, final File root) throws ImplerException {
        if (loadedClass.isPrimitive() || Modifier.isFinal(loadedClass.getModifiers())) {
            throw new ImplerException("Cannot implement "
                    + (loadedClass.isPrimitive() ? "primitive" : "final") + " class");
        }
        String className = loadedClass.getSimpleName();
        String implClassName = className + "Impl" ;

        StringBuilder writer = new StringBuilder();
        if (loadedClass.getPackage() != null) {
            writer.append("package ").append(loadedClass.getPackage().getName()).append(";").append("\n");
        }
        writer.append("import ").append(loadedClass.getName()).append(";");
        writer.append("\n");
        writer.append("public class ")
                .append(implClassName)
                .append(loadedClass.isInterface() ? " implements " : " extends ")
                .append(className)
                .append(" {");
        writer.append("\n");

        implementConstructors(loadedClass, implClassName, writer);

        implementMethods(loadedClass, writer);

        writer.append("}");
        writer.append("\n");

        File newRoot = createDirs(loadedClass.getPackage(), root);
        File implFile = new File(newRoot.getPath() + File.separator + implClassName + ".java");
        try (BufferedWriter fileOut = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(implFile), "UTF-8"))) {
            fileOut.write(writer.toString());
        } catch (IOException e) {
            throw new ImplerException("Error during writing file", e.getCause());
        }
    }

    private void implementMethods(final Class<?> loadedClass, final StringBuilder writer) {
        loadAllMethods(loadedClass, true);
        loadAllMethods(loadedClass, false);
        for (Map.Entry<String, MethodOnSteroids> item : methods.entrySet()) {
                writer.append(getMethodImplementation(item.getValue()));
        }
    }

    private void implementConstructors(final Class<?> clazz, final String implClazzName, final StringBuilder writer) throws ImplerException {
        if (clazz.isInterface()) {
            return;
        }
        int counterConstructors = 0;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            int modifiers = constructor.getModifiers();
            if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
                counterConstructors++;
                writer.append(getAnnotationsImplementation(constructor.getDeclaredAnnotations(), false));
                writer.append(implClazzName);
                writer.append(getParametrsImplementation(constructor.getParameters(), false));
                writer.append(getExceptionsImplementation(constructor.getExceptionTypes()));
                writer.append(" {");
                writer.append("\n");
                writer.append("super");
                writer.append(getParametrsImplementation(constructor.getParameters(), true));
                writer.append(";");
                writer.append("\n");
                writer.append("}");
                writer.append("\n");
            }
        }
        if (!clazz.isInterface() && counterConstructors == 0) {
            throw new ImplerException("Class has only private constructors");
        }
    }

    private String getAnnotationsImplementation(final Annotation[] annotations, final boolean isOverrided) {
        StringBuilder writer = new StringBuilder();
        if (isOverrided) {
            writer.append("@Override\n");
        }
        for (Annotation annotation : annotations) {
            writer.append(annotation).append("\n");
        }
        return writer.toString();
    }

    private void loadAllMethods(final Class<?> clazz, final boolean addAbstract) {
        if (clazz == null) {
            return;
        }
        loadAllMethods(clazz.getSuperclass(), addAbstract);
        for (Class<?> item : clazz.getInterfaces()) {
            loadAllMethods(item, addAbstract);
        }

        for (Method method : clazz.getDeclaredMethods()) {
            if (addAbstract) {
                addMethod(method);
            } else {
                removeMethod(method);
            }
        }
    }

    private void removeMethod(Method method) {
        int modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers) || Modifier.isPrivate(modifiers)) {
            return;
        }
        final String methodSignature = getMethodSignature(method);
        if (methods.containsKey(methodSignature)) {
            MethodOnSteroids steroids = methods.get(methodSignature);
            if (steroids.getMethod().getDeclaringClass()
                    .isAssignableFrom(method.getDeclaringClass())) {
                methods.remove(methodSignature);
            }
        }
    }

    private void addMethod(Method method) {
        int modifiers = method.getModifiers();
        if (!Modifier.isAbstract(modifiers) || Modifier.isPrivate(modifiers)) {
            return;
        }
        final String methodSignature = getMethodSignature(method);
        MethodOnSteroids addedMethod = new MethodOnSteroids(method);
        if (methods.containsKey(methodSignature)) {
            MethodOnSteroids steroids = methods.get(methodSignature);

            if (!steroids.getMethod().getDeclaringClass().isAssignableFrom(method.getDeclaringClass())
                    && !method.getDeclaringClass().isAssignableFrom(steroids.getMethod().getDeclaringClass())) {
                addedMethod.setMethodExceptions(null);

                if (Modifier.isProtected(modifiers) && Modifier.isPublic(steroids.getMethodModifiers())) {
                    addedMethod.setMethodModifiers(modifiers);
                }
            }
        }
        methods.put(getMethodSignature(method), addedMethod);
    }

    private String getMethodSignature(final Method method) {
        return method.getName() + getParametrsImplementation(method.getParameters(), false);
    }

    private String getExceptionsImplementation(final Class<?>[] exceptions) {
        StringBuilder writer = new StringBuilder();
        int countExceptions = exceptions.length;
        if (countExceptions > 0) {
            writer.append(" throws ");
            for (int i = 0; i < countExceptions; i++) {
                if (i > 0) {
                    writer.append(", ");
                }
                writer.append(exceptions[i].getName());
            }
        }
        return writer.toString();
    }

    private String getMethodImplementation(final MethodOnSteroids steroids) {
        final Method method = steroids.getMethod();
        int modifiers = steroids.getMethodModifiers();
        StringBuilder writer = new StringBuilder();
        if (Modifier.isPublic(modifiers) || Modifier.isProtected(modifiers)) {
            writer.append(getAnnotationsImplementation(method.getDeclaredAnnotations(), true));
            if (Modifier.isProtected(modifiers)) {
                writer.append("protected ");
            } else {
                writer.append("public ");
            }
            if (method.getGenericReturnType() instanceof TypeVariable) {
                writer.append("<").append(((TypeVariable) method.getGenericReturnType()).getName()).append(">");
            }
            writer.append(method.getGenericReturnType().getTypeName()).append(" ");
            writer.append(method.getName());
            writer.append(getParametrsImplementation(method.getParameters(), false));
            writer.append(getExceptionsImplementation(steroids.getMethodExceptions()));
            writer.append(" {\n");
            writer.append(getReturnValueImplementation(method.getReturnType()));
            writer.append("}\n\n");
        }
        return writer.toString();
    }

    private String getParametrsImplementation(final Parameter[] parameters, final boolean onlyNames) {
        StringBuilder writer = new StringBuilder();
        writer.append("(");
        for (int i = 0; i < parameters.length; i++) {
            if (i > 0) {
                writer.append(", ");
            }
            if (onlyNames) {
                writer.append(parameters[i].getName());
            } else {
                writer.append(parameters[i].toString());
            }
        }
        writer.append(") ");
        return writer.toString();
    }

    private String getReturnValueImplementation(final Class<?> returnType) {
        StringBuilder writer = new StringBuilder();

        if (returnType != Void.TYPE) {
            writer.append("return ");
            if (returnType.isPrimitive()) {
                writer.append(getDefaultValue(returnType));
            } else {
                writer.append("null");
            }
            writer.append(";\n");
        }
        return writer.toString();
    }

    private static String getDefaultValue(final Class clazz) {
        if (clazz.equals(boolean.class)) {
            return "false";
        } else if (clazz.equals(byte.class)) {
            return "(byte)0";
        } else if (clazz.equals(short.class)) {
            return "(short)0";
        } else if (clazz.equals(int.class)) {
            return "0";
        } else if (clazz.equals(long.class)) {
            return "0L";
        } else if (clazz.equals(float.class)) {
            return "0.0f";
        } else if (clazz.equals(double.class)) {
            return "0.0d";
        } else if (clazz.equals(char.class)) {
            return "'\\u0000'";
        } else {
            throw new IllegalArgumentException(
                    "Class type " + clazz + " not supported");
        }
    }

    private class MethodOnSteroids {
        private Method method;
        private int methodModifiers;
        private Class<?>[] methodExceptions;

        public MethodOnSteroids(Method method) {
            this.method = method;
            this.methodModifiers = method.getModifiers();
            this.methodExceptions = method.getExceptionTypes();
        }

        public Method getMethod() {
            return method;
        }

        public int getMethodModifiers() {
            return methodModifiers;
        }

        public Class<?>[] getMethodExceptions() {
            return methodExceptions;
        }

        public void setMethodModifiers(int methodModifiers) {
            this.methodModifiers = methodModifiers;
        }

        public void setMethodExceptions(Class<?>[] methodExceptions) {
            if (methodExceptions == null) {
                this.methodExceptions = new Class<?>[0];
            } else {
                this.methodExceptions = methodExceptions;
            }
        }
    }
}
