/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.optaplanner.core.impl.domain.common.accessor.gizmo;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.optaplanner.core.impl.domain.common.accessor.MemberAccessor;

import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.TryBlock;

/**
 * Generates the bytecode for the MemberAccessor of a particular Member
 */
public class GizmoMemberAccessorImplementor {

    /**
     * The Gizmo generated bytecode. Used by
     * gizmoClassLoader when not run in Quarkus
     * in order to create an instance of the Member
     * Accessor
     */
    private static final Map<String, byte[]> classNameToBytecode = new HashMap<>();

    /**
     * A custom classloader that looks for the class in
     * classNameToBytecode
     */
    private static ClassLoader gizmoClassLoader = new ClassLoader() {
        // getName() is an abstract method in Java 11 but not in Java 8
        public String getName() {
            return "OptaPlanner Gizmo MemberAccessor ClassLoader";
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            if (classNameToBytecode.containsKey(name)) {
                // Gizmo generated class
                byte[] byteCode = classNameToBytecode.get(name);
                return defineClass(name, byteCode, 0, byteCode.length);
            } else {
                // Not a Gizmo generated class; load from context class loader
                return Thread.currentThread().getContextClassLoader().loadClass(name);
            }
        }
    };

    final static String GENERIC_TYPE_FIELD = "genericType";
    final static String ANNOTATED_ELEMENT_FIELD = "annotatedElement";

    /**
     * Generates the constructor and implementations of MemberAccessor
     * methods for the given MemberDescriptor using the given ClassCreator
     *
     * @param classCreator ClassCreator to write output to
     * @param member Member to generate MemberAccessor methods implementation for
     * @param annotationClass The annotation it was annotated with (used for
     *        error reporting)
     */
    public static void defineAccessorFor(ClassCreator classCreator, GizmoMemberDescriptor member,
            Class<? extends Annotation> annotationClass) {
        classCreator.getFieldCreator("genericType", Type.class)
                .setModifiers(Modifier.FINAL);
        classCreator.getFieldCreator("annotatedElement", AnnotatedElement.class)
                .setModifiers(Modifier.FINAL);

        GizmoMemberInfo memberInfo = new GizmoMemberInfo(member, annotationClass);
        // ************************************************************************
        // MemberAccessor methods
        // ************************************************************************
        createConstructor(classCreator, memberInfo);
        createGetDeclaringClass(classCreator, memberInfo);
        createGetType(classCreator, memberInfo);
        createGetGenericType(classCreator, memberInfo);
        createGetName(classCreator, memberInfo);
        createGetSpeedNote(classCreator, memberInfo);
        createSupportSetter(classCreator, memberInfo);
        createExecuteGetter(classCreator, memberInfo);
        createExecuteSetter(classCreator, memberInfo);

        // ************************************************************************
        // AnnotatedElement methods
        // ************************************************************************

        createIsAnnotationPresent(classCreator, memberInfo);
        createGetAnnotation(classCreator, memberInfo);
        createGetAnnotations(classCreator, memberInfo);
        createGetDeclaredAnnotations(classCreator, memberInfo);
    }

    /**
     * Creates a MemberAccessor for a given member, generating
     * the MemberAccessor bytecode if required
     *
     * @param member The member to generate a MemberAccessor for
     * @param annotationClass The annotation it was annotated with (used for
     *        error reporting)
     * @return A new MemberAccessor that uses Gizmo generated bytecode.
     *         Will generate the bytecode the first type it is called
     *         for a member, unless a classloader has been set,
     *         in which case no Gizmo code will be generated.
     */
    public static MemberAccessor createAccessorFor(Member member, Class<? extends Annotation> annotationClass) {
        String className = GizmoMemberAccessorFactory.getGeneratedClassName(member);
        if (classNameToBytecode.containsKey(className)) {
            return createInstance(className);
        }
        final byte[][] classBytecodeHolder = new byte[1][];
        ClassOutput classOutput = (path, byteCode) -> {
            classBytecodeHolder[0] = byteCode;
        };
        ClassCreator classCreator = ClassCreator.builder()
                .className(className)
                .interfaces(MemberAccessor.class)
                .superClass(Object.class)
                .classOutput(classOutput)
                .build();

        GizmoMemberDescriptor memberDescriptor = new GizmoMemberDescriptor(member);
        defineAccessorFor(classCreator, memberDescriptor, annotationClass);

        classCreator.close();
        byte[] classBytecode = classBytecodeHolder[0];

        classNameToBytecode.put(className, classBytecode);
        return createInstance(className);
    }

    private static MemberAccessor createInstance(String className) {
        try {
            return (MemberAccessor) gizmoClassLoader.loadClass(className)
                    .getConstructor().newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | ClassNotFoundException
                | NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    // ************************************************************************
    // MemberAccessor methods
    // ************************************************************************

    private static MethodCreator getMethodCreator(ClassCreator classCreator, String methodName, Class<?>... parameters) {
        try {
            return classCreator.getMethodCreator(
                    MethodDescriptor.ofMethod(MemberAccessor.class.getMethod(methodName, parameters)));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No such method: " + methodName, e);
        }
    }

    private static void createConstructor(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator =
                classCreator.getMethodCreator(MethodDescriptor.ofConstructor(classCreator.getClassName()));

        ResultHandle thisObj = methodCreator.getThis();

        // Invoke Object's constructor
        methodCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(Object.class), thisObj);

        ResultHandle declaringClass = methodCreator.loadClass(memberInfo.getDescriptor().getDeclaringClassName());
        memberInfo.getDescriptor().whenMetadataIsOnField(fd -> {
            TryBlock tryBlock = methodCreator.tryBlock();
            ResultHandle name = tryBlock.load(fd.getName());
            ResultHandle field = tryBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(Class.class, "getDeclaredField",
                    Field.class, String.class),
                    declaringClass, name);
            ResultHandle type =
                    tryBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(Field.class, "getGenericType", Type.class),
                            field);
            tryBlock.writeInstanceField(FieldDescriptor.of(classCreator.getClassName(), GENERIC_TYPE_FIELD, Type.class),
                    thisObj, type);
            tryBlock.writeInstanceField(
                    FieldDescriptor.of(classCreator.getClassName(), ANNOTATED_ELEMENT_FIELD, AnnotatedElement.class),
                    thisObj, field);

            tryBlock.addCatch(NoSuchFieldException.class).throwException(IllegalStateException.class, "Unable to find field (" +
                    fd.getName() + ") in class (" + fd.getDeclaringClass() + ").");
        });

        memberInfo.getDescriptor().whenMetadataIsOnMethod(md -> {
            TryBlock tryBlock = methodCreator.tryBlock();
            ResultHandle name = tryBlock.load(md.getName());
            ResultHandle method = tryBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(Class.class, "getDeclaredMethod",
                    Method.class, String.class, Class[].class),
                    declaringClass, name,
                    tryBlock.newArray(Class.class, 0));
            ResultHandle type =
                    tryBlock.invokeVirtualMethod(MethodDescriptor.ofMethod(Method.class, "getGenericReturnType", Type.class),
                            method);
            tryBlock.writeInstanceField(FieldDescriptor.of(classCreator.getClassName(), GENERIC_TYPE_FIELD, Type.class),
                    thisObj, type);
            tryBlock.writeInstanceField(
                    FieldDescriptor.of(classCreator.getClassName(), ANNOTATED_ELEMENT_FIELD, AnnotatedElement.class),
                    thisObj, method);

            tryBlock.addCatch(NoSuchMethodException.class).throwException(IllegalStateException.class,
                    "Unable to find method (" +
                            md.getName() + ") in class (" + md.getDeclaringClass() + ").");
        });

        // Return this (it a constructor)
        methodCreator.returnValue(thisObj);
    }

    /**
     * Generates the following code:
     * 
     * <pre>
     * Class getDeclaringClass() {
     *     return ClassThatDeclaredMember.class;
     * }
     * </pre>
     */
    private static void createGetDeclaringClass(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "getDeclaringClass");
        ResultHandle out = methodCreator.loadClass(memberInfo.getDescriptor().getDeclaringClassName());
        methodCreator.returnValue(out);
    }

    /**
     * Asserts method is a getter or read method
     *
     * @param method Method to assert is getter or read
     * @param annotationClass Used in exception message
     */
    private static void assertIsGoodMethod(MethodDescriptor method, Class<? extends Annotation> annotationClass) {
        String methodName = method.getName();
        if (method.getParameterTypes().length != 0) {
            // not read or getter method
            throw new IllegalStateException("The getterMethod (" + methodName + ") with a "
                    + annotationClass.getSimpleName() + " annotation must not have any parameters, but has parameters ("
                    + Arrays.toString(method.getParameterTypes()) + ").");
        }
        if (methodName.startsWith("get")) {
            if (method.getReturnType().equals("V")) {
                throw new IllegalStateException("The getterMethod (" + methodName + ") with a "
                        + annotationClass.getSimpleName() + " annotation must have a non-void return type.");
            }
        } else if (methodName.startsWith("is")) {
            if (!method.getReturnType().equals("boolean")) {
                throw new IllegalStateException("The getterMethod (" + methodName + ") with a "
                        + annotationClass.getSimpleName()
                        + " annotation must have a primitive boolean return type but returns ("
                        + method.getReturnType() + "). Maybe rename the method ("
                        + "get" + methodName.substring(2) + ")?");
            }
        } else {
            // must be a read method
            if (method.getReturnType().equals("V")) {
                throw new IllegalStateException("The readMethod (" + methodName + ") with a "
                        + annotationClass.getSimpleName() + " annotation must have a non-void return type.");
            }
        }
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * String getName() {
     *     return "fieldOrMethodName";
     * }
     * </pre>
     *
     * If it is a getter method, "get" is removed and the first
     * letter become lowercase
     */
    private static void createGetName(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "getName");

        // If it is a method, assert that it has the required
        // properties
        memberInfo.getDescriptor().whenIsMethod(method -> {
            assertIsGoodMethod(method, memberInfo.getAnnotationClass());
        });

        String fieldName = memberInfo.getDescriptor().getName();
        ResultHandle out = methodCreator.load(fieldName);
        methodCreator.returnValue(out);
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Class getType() {
     *     return FieldTypeOrMethodReturnType.class;
     * }
     * </pre>
     */
    private static void createGetType(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "getType");
        ResultHandle out = methodCreator.loadClass(memberInfo.getDescriptor().getTypeName());
        methodCreator.returnValue(out);
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Type getGenericType() {
     *     return GizmoMemberAccessorImplementor.getGenericTypeFor(this.getClass().getName());
     * }
     * </pre>
     *
     * We are unable to load a non-primitive object constant, so we need to store it
     * in the implementor, which then can return us the Type when needed. The type
     * is stored in gizmoMemberAccessorNameToGenericType when this method is called.
     */
    private static void createGetGenericType(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "getGenericType");
        ResultHandle thisObj = methodCreator.getThis();

        ResultHandle out =
                methodCreator.readInstanceField(FieldDescriptor.of(classCreator.getClassName(), GENERIC_TYPE_FIELD, Type.class),
                        thisObj);
        methodCreator.returnValue(out);
    }

    /**
     * Generates the following code:
     *
     * For a field
     * 
     * <pre>
     * Object executeGetter(Object bean) {
     *     return ((DeclaringClass) bean).field;
     * }
     * </pre>
     *
     * For a method
     * 
     * <pre>
     * Object executeGetter(Object bean) {
     *     return ((DeclaringClass) bean).method();
     * }
     * </pre>
     *
     * The member MUST be public if not called in Quarkus
     * (i.e. we don't delegate to the field getter/setter).
     * In Quarkus, we generate simple getter/setter for the
     * member if it is private (which get passed to the MemberDescriptor).
     */
    private static void createExecuteGetter(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "executeGetter", Object.class);
        ResultHandle bean = methodCreator.getMethodParam(0);

        memberInfo.getDescriptor().whenIsMethod(method -> {
            assertIsGoodMethod(method, memberInfo.getAnnotationClass());
            ResultHandle out = memberInfo.getDescriptor().invokeMemberMethod(methodCreator, method, bean);
            methodCreator.returnValue(out);
        });

        memberInfo.getDescriptor().whenIsField(field -> {
            ResultHandle out = methodCreator.readInstanceField(field, bean);
            methodCreator.returnValue(out);
        });
    }

    /**
     * Generates the following code:
     *
     * For a field or a getter method that also have a corresponding setter
     * 
     * <pre>
     * boolean supportSetter() {
     *     return true;
     * }
     * </pre>
     *
     * For a read method or a getter method without a setter
     * 
     * <pre>
     * boolean supportSetter() {
     *     return false;
     * }
     * </pre>
     */
    private static void createSupportSetter(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "supportSetter");
        memberInfo.getDescriptor().whenIsMethod(method -> {
            boolean supportSetter = memberInfo.getDescriptor().getSetter().isPresent();
            ResultHandle out = methodCreator.load(supportSetter);
            methodCreator.returnValue(out);
        });
        memberInfo.getDescriptor().whenIsField(field -> {
            ResultHandle out = methodCreator.load(true);
            methodCreator.returnValue(out);
        });
    }

    /**
     * Generates the following code:
     *
     * For a field
     * 
     * <pre>
     * void executeSetter(Object bean, Object value) {
     *     return ((DeclaringClass) bean).field = value;
     * }
     * </pre>
     *
     * For a getter method with a corresponding setter
     * 
     * <pre>
     * void executeSetter(Object bean, Object value) {
     *     return ((DeclaringClass) bean).setValue(value);
     * }
     * </pre>
     *
     * For a read method or a getter method without a setter
     * 
     * <pre>
     * void executeSetter(Object bean, Object value) {
     *     throw new UnsupportedOperationException("Setter not supported");
     * }
     * </pre>
     */
    private static void createExecuteSetter(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "executeSetter", Object.class,
                Object.class);

        memberInfo.getDescriptor().whenIsMethod(method -> {
            Optional<MethodDescriptor> setter = memberInfo.getDescriptor().getSetter();
            if (setter.isPresent()) {
                ResultHandle bean = methodCreator.getMethodParam(0);
                ResultHandle value = methodCreator.getMethodParam(1);
                memberInfo.getDescriptor().invokeMemberMethod(methodCreator, setter.get(), bean, value);
                methodCreator.returnValue(null);
            } else {
                methodCreator.throwException(UnsupportedOperationException.class, "Setter not supported");
            }
        });

        memberInfo.getDescriptor().whenIsField(field -> {
            ResultHandle bean = methodCreator.getMethodParam(0);
            ResultHandle value = methodCreator.getMethodParam(1);
            methodCreator.writeInstanceField(field, bean, value);
            methodCreator.returnValue(null);
        });
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * String getSpeedNote() {
     *     return "Fast access with generated bytecode";
     * }
     * </pre>
     */
    private static void createGetSpeedNote(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getMethodCreator(classCreator, "getSpeedNote");
        ResultHandle out = methodCreator.load("Fast access with generated bytecode");
        methodCreator.returnValue(out);
    }

    // ************************************************************************
    // AnnotatedElement methods
    // ************************************************************************
    private static MethodCreator getAnnotationMethodCreator(ClassCreator classCreator, String methodName,
            Class<?>... parameters) {
        return classCreator.getMethodCreator(getAnnotationMethod(methodName, parameters));
    }

    private static MethodDescriptor getAnnotationMethod(String methodName, Class<?>... parameters) {
        try {
            return MethodDescriptor.ofMethod(AnnotatedElement.class.getMethod(methodName, parameters));
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No such method: " + methodName, e);
        }
    }

    // These methods all delegate to an AnnotatedElement we store in
    // gizmoMemberAccessorNameToAnnotatedElement

    // getAnnotatedElementGetter() simply returns the method descriptor for getAnnotatedElementFor

    /**
     * Generates the following code:
     *
     * <pre>
     * boolean isAnnotationPresent(Class annotationClass) {
     *     AnnotatedElement annotatedElement = GizmoMemberAccessorImplementor
     *             .getAnnotatedElementFor(this.getClass().getName());
     *     return annotatedElement.isAnnotationPresent(annotationClass);
     * }
     * </pre>
     */
    private static void createIsAnnotationPresent(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getAnnotationMethodCreator(classCreator, "isAnnotationPresent",
                Class.class);
        ResultHandle thisObj = methodCreator.getThis();

        ResultHandle annotatedElement = methodCreator.readInstanceField(
                FieldDescriptor.of(classCreator.getClassName(), ANNOTATED_ELEMENT_FIELD, AnnotatedElement.class),
                thisObj);
        ResultHandle query = methodCreator.getMethodParam(0);
        ResultHandle out = methodCreator.invokeInterfaceMethod(getAnnotationMethod("isAnnotationPresent", Class.class),
                annotatedElement, query);
        methodCreator.returnValue(out);
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Object getAnnotation(Class annotationClass) {
     *     AnnotatedElement annotatedElement = GizmoMemberAccessorImplementor
     *             .getAnnotatedElementFor(this.getClass().getName());
     *     return annotatedElement.getAnnotation(annotationClass);
     * }
     * </pre>
     */
    private static void createGetAnnotation(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getAnnotationMethodCreator(classCreator, "getAnnotation",
                Class.class);
        ResultHandle thisObj = methodCreator.getThis();

        ResultHandle annotatedElement = methodCreator.readInstanceField(
                FieldDescriptor.of(classCreator.getClassName(), ANNOTATED_ELEMENT_FIELD, AnnotatedElement.class),
                thisObj);
        ResultHandle query = methodCreator.getMethodParam(0);
        ResultHandle out = methodCreator.invokeInterfaceMethod(getAnnotationMethod("getAnnotation", Class.class),
                annotatedElement, query);
        methodCreator.returnValue(out);
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Object[] getAnnotations() {
     *     AnnotatedElement annotatedElement = GizmoMemberAccessorImplementor
     *             .getAnnotatedElementFor(this.getClass().getName());
     *     return annotatedElement.getAnnotations();
     * }
     * </pre>
     */
    private static void createGetAnnotations(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getAnnotationMethodCreator(classCreator, "getAnnotations");
        ResultHandle thisObj = methodCreator.getThis();

        ResultHandle annotatedElement = methodCreator.readInstanceField(
                FieldDescriptor.of(classCreator.getClassName(), ANNOTATED_ELEMENT_FIELD, AnnotatedElement.class),
                thisObj);
        ResultHandle out = methodCreator.invokeInterfaceMethod(getAnnotationMethod("getAnnotations"),
                annotatedElement);
        methodCreator.returnValue(out);
    }

    /**
     * Generates the following code:
     *
     * <pre>
     * Object[] getDeclaredAnnotations() {
     *     AnnotatedElement annotatedElement = GizmoMemberAccessorImplementor
     *             .getAnnotatedElementFor(this.getClass().getName());
     *     return annotatedElement.getDeclaredAnnotations();
     * }
     * </pre>
     */
    private static void createGetDeclaredAnnotations(ClassCreator classCreator, GizmoMemberInfo memberInfo) {
        MethodCreator methodCreator = getAnnotationMethodCreator(classCreator, "getDeclaredAnnotations");
        ResultHandle thisObj = methodCreator.getThis();

        ResultHandle annotatedElement = methodCreator.readInstanceField(
                FieldDescriptor.of(classCreator.getClassName(), ANNOTATED_ELEMENT_FIELD, AnnotatedElement.class),
                thisObj);
        ResultHandle out = methodCreator.invokeInterfaceMethod(getAnnotationMethod("getDeclaredAnnotations"),
                annotatedElement);
        methodCreator.returnValue(out);
    }

    private GizmoMemberAccessorImplementor() {

    }

    private static class GizmoMemberInfo {
        private final GizmoMemberDescriptor descriptor;
        private final Class<? extends Annotation> annotationClass;

        public GizmoMemberInfo(GizmoMemberDescriptor descriptor, Class<? extends Annotation> annotationClass) {
            this.descriptor = descriptor;
            this.annotationClass = annotationClass;
        }

        public GizmoMemberDescriptor getDescriptor() {
            return descriptor;
        }

        public Class<? extends Annotation> getAnnotationClass() {
            return annotationClass;
        }
    }
}
