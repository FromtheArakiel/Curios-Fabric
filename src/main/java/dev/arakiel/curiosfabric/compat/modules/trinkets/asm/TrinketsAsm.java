package dev.arakiel.curiosfabric.compat.modules.trinkets.asm;

import dev.arakiel.curiosfabric.CuriosFabricConstants;
import dev.arakiel.curiosfabric.compat.shared.reflection.ForeignProxy;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

/**
 * Stable ASM adapters for the Trinkets bridge.
 *
 * <p>Only the long-lived ASM 9 API is used: ClassWriter, MethodVisitor, visitMethodInsn,
 * visitVarInsn and the basic constant/type visitors. All generated classes are cached by their
 * target signature; no reflective method lookup occurs on a hot call after the first miss.</p>
 */
public final class TrinketsAsm {

  private static final String GENERATED_PACKAGE =
      "dev/arakiel/curiosfabric/compat/modules/trinkets/asm/generated/";
  private static final Map<String, Invoker> INVOKERS = new ConcurrentHashMap<>();
  private static final Map<String, ProxyDefinition> PROXIES = new ConcurrentHashMap<>();
  private static final Invoker MISSING = (target, args) -> null;
  private static final AtomicInteger NEXT_CLASS = new AtomicInteger();

  private TrinketsAsm() {
  }

  @FunctionalInterface
  public interface Invoker {
    Object invoke(Object target, Object[] args) throws Throwable;
  }

  public static Invoker invoker(Class<?> targetType, String name, Object[] args) {
    String key = targetType.getName() + "#" + name + "#" + argumentKey(args);
    return INVOKERS.computeIfAbsent(key, ignored -> createInvoker(targetType, name, args));
  }

  /**
   * Creates an ASM implementation of one or more foreign interfaces. The proxy factory itself is a
   * generated static method, so creating an inventory view does not need reflective constructors.
   */
  public static Object proxy(String[] interfaceNames, ForeignProxy.Invoker dispatcher) {
    String key = String.join("|", interfaceNames);
    ProxyDefinition definition = PROXIES.computeIfAbsent(
        key, ignored -> createProxyDefinition(interfaceNames));
    if (definition == null) {
      return null;
    }
    try {
      return definition.factory().invoke(dispatcher);
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Unable to instantiate ASM Trinkets proxy", throwable);
      return null;
    }
  }

  public static Object safeDispatch(
      ForeignProxy.Invoker dispatcher,
      String name,
      Class<?> returnType,
      Object[] args) {
    try {
      return dispatcher.invoke(name, returnType, null, args);
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.debug("Bridged call {} failed", name, throwable);
      return ForeignProxy.defaultValue(returnType);
    }
  }

  private static Invoker createInvoker(Class<?> targetType, String name, Object[] args) {
    Method method = findMethod(targetType, name, args);
    if (method == null) {
      return MISSING;
    }
    try {
      Class<?> owner = publicOwner(targetType, method);
      String className = GENERATED_PACKAGE + "TrinketsInvoker$"
          + NEXT_CLASS.getAndIncrement();
      byte[] bytes = generateInvoker(className, targetType, owner, method);
      Class<?> generated = define(
          className, bytes);
      return (Invoker) generated.getDeclaredConstructor().newInstance();
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.warn(
          "Unable to generate ASM Trinkets invoker {}#{}",
          targetType.getName(), name, throwable);
      return MISSING;
    }
  }

  private static byte[] generateInvoker(
      String className, Class<?> targetType, Class<?> owner, Method method) {
    String ownerName = internalName(owner);
    Class<?>[] parameters = method.getParameterTypes();
    boolean isStatic = Modifier.isStatic(method.getModifiers());
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    writer.visit(
        Opcodes.V17,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
        className,
        null,
        "java/lang/Object",
        new String[]{Type.getInternalName(Invoker.class)});
    writeDefaultConstructor(writer);

    MethodVisitor mv = writer.visitMethod(
        Opcodes.ACC_PUBLIC,
        "invoke",
        "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;",
        null,
        new String[]{"java/lang/Throwable"});
    mv.visitCode();
    if (!isStatic) {
      mv.visitVarInsn(Opcodes.ALOAD, 1);
      mv.visitTypeInsn(Opcodes.CHECKCAST, ownerName);
    }
    for (int index = 0; index < parameters.length; index++) {
      mv.visitVarInsn(Opcodes.ALOAD, 2);
      pushInt(mv, index);
      mv.visitInsn(Opcodes.AALOAD);
      loadArgument(mv, parameters[index]);
    }
    int opcode = isStatic
        ? Opcodes.INVOKESTATIC
        : (owner.isInterface() ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL);
    mv.visitMethodInsn(
        opcode,
        ownerName,
        method.getName(),
        Type.getMethodDescriptor(method),
        owner.isInterface());
    boxReturn(mv, method.getReturnType());
    mv.visitInsn(Opcodes.ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    writer.visitEnd();
    return writer.toByteArray();
  }

  private static ProxyDefinition createProxyDefinition(String[] interfaceNames) {
    Class<?>[] interfaces = new Class<?>[interfaceNames.length];
    for (int index = 0; index < interfaceNames.length; index++) {
      try {
        interfaces[index] = Class.forName(
            interfaceNames[index], false, TrinketsAsm.class.getClassLoader());
      } catch (Throwable throwable) {
        return null;
      }
    }
    try {
      String className = GENERATED_PACKAGE + "TrinketsProxy$"
          + NEXT_CLASS.getAndIncrement();
      byte[] bytes = generateProxy(className, interfaces);
      Class<?> generated = define(className, bytes);
      MethodHandle factory = MethodHandles.publicLookup().findStatic(
          generated,
          "create",
          MethodType.methodType(Object.class, ForeignProxy.Invoker.class));
      return new ProxyDefinition(factory);
    } catch (Throwable throwable) {
      CuriosFabricConstants.LOG.warn(
          "Unable to generate ASM Trinkets interface proxy", throwable);
      return null;
    }
  }

  private static byte[] generateProxy(String className, Class<?>[] interfaces) {
    ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
    String[] interfaceNames = Arrays.stream(interfaces)
        .map(Type::getInternalName).toArray(String[]::new);
    writer.visit(
        Opcodes.V17,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL,
        className,
        null,
        "java/lang/Object",
        interfaceNames);

    MethodVisitor ctor = writer.visitMethod(
        Opcodes.ACC_PUBLIC, "<init>", "(Ldev/arakiel/curiosfabric/compat/shared/reflection/"
            + "ForeignProxy$Invoker;)V", null, null);
    ctor.visitCode();
    ctor.visitVarInsn(Opcodes.ALOAD, 0);
    ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    ctor.visitVarInsn(Opcodes.ALOAD, 0);
    ctor.visitVarInsn(Opcodes.ALOAD, 1);
    ctor.visitFieldInsn(
        Opcodes.PUTFIELD,
        className,
        "dispatcher",
        "Ldev/arakiel/curiosfabric/compat/shared/reflection/ForeignProxy$Invoker;");
    ctor.visitInsn(Opcodes.RETURN);
    ctor.visitMaxs(0, 0);
    ctor.visitEnd();
    writer.visitField(
        Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL,
        "dispatcher",
        "Ldev/arakiel/curiosfabric/compat/shared/reflection/ForeignProxy$Invoker;",
        null,
        null).visitEnd();

    MethodVisitor factory = writer.visitMethod(
        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
        "create",
        "(Ldev/arakiel/curiosfabric/compat/shared/reflection/ForeignProxy$Invoker;)"
            + "Ljava/lang/Object;",
        null,
        null);
    factory.visitCode();
    factory.visitTypeInsn(Opcodes.NEW, className);
    factory.visitInsn(Opcodes.DUP);
    factory.visitVarInsn(Opcodes.ALOAD, 0);
    factory.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        className,
        "<init>",
        "(Ldev/arakiel/curiosfabric/compat/shared/reflection/ForeignProxy$Invoker;)V",
        false);
    factory.visitInsn(Opcodes.ARETURN);
    factory.visitMaxs(0, 0);
    factory.visitEnd();

    Map<String, Method> methods = new LinkedHashMap<>();
    for (Class<?> iface : interfaces) {
      for (Method method : iface.getMethods()) {
        if (!Modifier.isStatic(method.getModifiers())) {
          methods.putIfAbsent(method.getName() + Type.getMethodDescriptor(method), method);
        }
      }
    }
    methods.values().forEach(method -> writeProxyMethod(writer, className, method));
    writer.visitEnd();
    return writer.toByteArray();
  }

  private static void writeProxyMethod(
      ClassWriter writer, String className, Method method) {
    MethodVisitor mv = writer.visitMethod(
        Opcodes.ACC_PUBLIC,
        method.getName(),
        Type.getMethodDescriptor(method),
        null,
        exceptionNames(method));
    mv.visitCode();
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitFieldInsn(
        Opcodes.GETFIELD,
        className,
        "dispatcher",
        "Ldev/arakiel/curiosfabric/compat/shared/reflection/ForeignProxy$Invoker;");
    mv.visitLdcInsn(method.getName());
    typeConstant(mv, method.getReturnType());
    buildArgumentsArray(mv, method.getParameterTypes());
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        Type.getInternalName(TrinketsAsm.class),
        "safeDispatch",
        "(Ldev/arakiel/curiosfabric/compat/shared/reflection/ForeignProxy$Invoker;"
            + "Ljava/lang/String;Ljava/lang/Class;[Ljava/lang/Object;)Ljava/lang/Object;",
        false);
    writeProxyReturn(mv, method.getReturnType());
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static void buildArgumentsArray(MethodVisitor mv, Class<?>[] parameters) {
    pushInt(mv, parameters.length);
    mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/lang/Object");
    int local = 1;
    for (int index = 0; index < parameters.length; index++) {
      mv.visitInsn(Opcodes.DUP);
      pushInt(mv, index);
      mv.visitVarInsn(loadOpcode(parameters[index]), local);
      box(mv, parameters[index]);
      mv.visitInsn(Opcodes.AASTORE);
      local += width(parameters[index]);
    }
  }

  private static void writeProxyReturn(MethodVisitor mv, Class<?> returnType) {
    if (returnType == void.class) {
      mv.visitInsn(Opcodes.POP);
      mv.visitInsn(Opcodes.RETURN);
      return;
    }
    if (returnType.isPrimitive()) {
      mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperName(returnType));
      mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL,
          wrapperName(returnType),
          unboxName(returnType),
          "()" + Type.getDescriptor(returnType),
          false);
      mv.visitInsn(returnOpcode(returnType));
      return;
    }
    if (!returnType.equals(Object.class)) {
      mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(returnType));
    }
    mv.visitInsn(Opcodes.ARETURN);
  }

  private static void writeDefaultConstructor(ClassWriter writer) {
    MethodVisitor mv = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(Opcodes.ALOAD, 0);
    mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
    mv.visitInsn(Opcodes.RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static void loadArgument(MethodVisitor mv, Class<?> type) {
    if (type.isPrimitive()) {
      mv.visitTypeInsn(Opcodes.CHECKCAST, wrapperName(type));
      mv.visitMethodInsn(
          Opcodes.INVOKEVIRTUAL,
          wrapperName(type),
          unboxName(type),
          "()" + Type.getDescriptor(type),
          false);
    } else if (!type.equals(Object.class)) {
      mv.visitTypeInsn(Opcodes.CHECKCAST, Type.getInternalName(type));
    }
  }

  private static void boxReturn(MethodVisitor mv, Class<?> type) {
    if (type == void.class) {
      mv.visitInsn(Opcodes.ACONST_NULL);
    } else {
      box(mv, type);
    }
  }

  private static void box(MethodVisitor mv, Class<?> type) {
    if (!type.isPrimitive()) {
      return;
    }
    mv.visitMethodInsn(
        Opcodes.INVOKESTATIC,
        wrapperName(type),
        "valueOf",
        "(" + Type.getDescriptor(type) + ")L" + wrapperName(type) + ";",
        false);
  }

  private static void typeConstant(MethodVisitor mv, Class<?> type) {
    if (type == void.class) {
      mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/Void", "TYPE", "Ljava/lang/Class;");
      return;
    }
    if (type.isPrimitive()) {
      mv.visitFieldInsn(
          Opcodes.GETSTATIC,
          wrapperName(type),
          "TYPE",
          "Ljava/lang/Class;");
      return;
    }
    mv.visitLdcInsn(Type.getType(type));
  }

  private static Class<?> publicOwner(Class<?> targetType, Method method) {
    Class<?> owner = method.getDeclaringClass();
    if (Modifier.isPublic(owner.getModifiers())) {
      return owner;
    }
    List<Class<?>> queue = new ArrayList<>();
    queue.add(targetType);
    for (int index = 0; index < queue.size(); index++) {
      Class<?> current = queue.get(index);
      for (Class<?> iface : current.getInterfaces()) {
        if (Modifier.isPublic(iface.getModifiers())) {
          try {
            iface.getMethod(method.getName(), method.getParameterTypes());
            return iface;
          } catch (NoSuchMethodException ignored) {
          }
        }
        queue.add(iface);
      }
      if (current.getSuperclass() != null) {
        queue.add(current.getSuperclass());
      }
    }
    return owner;
  }

  private static Method findMethod(Class<?> targetType, String name, Object[] args) {
    for (Method method : targetType.getMethods()) {
      if (method.getName().equals(name) && accepts(method, args)) {
        return method;
      }
    }
    return null;
  }

  private static boolean accepts(Method method, Object[] args) {
    if (method.getParameterCount() != args.length) {
      return false;
    }
    Class<?>[] types = method.getParameterTypes();
    for (int index = 0; index < types.length; index++) {
      Object arg = args[index];
      if (arg == null ? types[index].isPrimitive() : !box(types[index]).isInstance(arg)) {
        return false;
      }
    }
    return true;
  }

  private static Class<?> box(Class<?> type) {
    if (!type.isPrimitive()) {
      return type;
    }
    if (type == boolean.class) return Boolean.class;
    if (type == byte.class) return Byte.class;
    if (type == short.class) return Short.class;
    if (type == int.class) return Integer.class;
    if (type == long.class) return Long.class;
    if (type == float.class) return Float.class;
    if (type == double.class) return Double.class;
    return Character.class;
  }

  private static String argumentKey(Object[] args) {
    StringBuilder builder = new StringBuilder();
    for (Object arg : args) {
      builder.append(arg == null ? "null" : arg.getClass().getName()).append(',');
    }
    return builder.toString();
  }

  private static String internalName(Class<?> type) {
    return Type.getInternalName(type);
  }

  private static String wrapperName(Class<?> type) {
    return Type.getInternalName(box(type));
  }

  private static String unboxName(Class<?> type) {
    return type.getName() + "Value";
  }

  private static int loadOpcode(Class<?> type) {
    if (type == long.class) return Opcodes.LLOAD;
    if (type == float.class) return Opcodes.FLOAD;
    if (type == double.class) return Opcodes.DLOAD;
    if (type.isPrimitive()) return Opcodes.ILOAD;
    return Opcodes.ALOAD;
  }

  private static int returnOpcode(Class<?> type) {
    if (type == long.class) return Opcodes.LRETURN;
    if (type == float.class) return Opcodes.FRETURN;
    if (type == double.class) return Opcodes.DRETURN;
    if (type == void.class) return Opcodes.RETURN;
    if (type.isPrimitive()) return Opcodes.IRETURN;
    return Opcodes.ARETURN;
  }

  private static int width(Class<?> type) {
    return type == long.class || type == double.class ? 2 : 1;
  }

  private static void pushInt(MethodVisitor mv, int value) {
    if (value >= -1 && value <= 5) {
      mv.visitInsn(Opcodes.ICONST_0 + value);
    } else if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
      mv.visitIntInsn(Opcodes.BIPUSH, value);
    } else {
      mv.visitIntInsn(Opcodes.SIPUSH, value);
    }
  }

  private static String[] exceptionNames(Method method) {
    return Arrays.stream(method.getExceptionTypes())
        .map(Type::getInternalName).toArray(String[]::new);
  }

  private static Class<?> define(String name, byte[] bytes) {
    return new GeneratedLoader(TrinketsAsm.class.getClassLoader()).define(name, bytes);
  }

  private record ProxyDefinition(MethodHandle factory) {
  }

  private static final class GeneratedLoader extends ClassLoader {
    private GeneratedLoader(ClassLoader parent) {
      super(parent);
    }

    private Class<?> define(String name, byte[] bytes) {
      return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
    }
  }
}
