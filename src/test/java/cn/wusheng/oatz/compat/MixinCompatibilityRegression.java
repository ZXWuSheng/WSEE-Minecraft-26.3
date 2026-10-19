package cn.wusheng.oatz.compat;

import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

/** Checks the actual patched Minecraft bytecode without loading or starting the game. */
public final class MixinCompatibilityRegression {
    private static int assertions;

    private MixinCompatibilityRegression() {
    }

    public static void main(String[] args) {
        assertions = 0;
        int mixins = 0;
        try (var reader = new InputStreamReader(resource("wsee.mixins.json"), StandardCharsets.UTF_8)) {
            var config = JsonParser.parseReader(reader).getAsJsonObject();
            String prefix = config.get("package").getAsString().replace('.', '/') + "/";
            for (String group : List.of("mixins", "client")) {
                for (var entry : config.getAsJsonArray(group)) {
                    verifyMixin(prefix + entry.getAsString());
                    mixins++;
                }
            }
        } catch (IOException e) {
            throw new AssertionError("Cannot inspect mixin resources", e);
        }
        check(mixins > 0, "No configured mixins were checked");
        System.out.println("Mixin compatibility: " + mixins + " mixins, " + assertions + " assertions passed.");
    }

    private static void verifyMixin(String name) throws IOException {
        ClassNode mixin = readClass(name);
        AnnotationNode binding = annotations(mixin.visibleAnnotations, mixin.invisibleAnnotations).stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;"))
                .findFirst().orElseThrow(() -> new AssertionError("Missing @Mixin: " + name));
        List<?> targets = list(value(binding, "value", List.of()));
        check(!targets.isEmpty(), name + " has no class target");
        for (Object entry : targets) {
            ClassNode target = readClass(((Type) entry).getInternalName());
            for (MethodNode handler : mixin.methods) {
                for (AnnotationNode annotation : annotations(handler.visibleAnnotations, handler.invisibleAnnotations)) {
                    String context = name + "." + handler.name + " -> " + target.name;
                    if (annotation.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")) {
                        MethodNode shadow = target.methods.stream()
                                .filter(m -> m.name.equals(handler.name) && m.desc.equals(handler.desc))
                                .findFirst().orElseThrow(() -> new AssertionError(context + " missing shadow"));
                        check(isStatic(handler) == isStatic(shadow), context + " shadow staticness changed");
                    } else if (annotation.desc.startsWith("Lorg/spongepowered/asm/mixin/injection/")) {
                        String kind = annotation.desc.substring(annotation.desc.lastIndexOf('/') + 1);
                        check(List.of("Inject;", "Redirect;", "ModifyConstant;").contains(kind),
                                context + " unsupported injection annotation: " + kind);
                        for (Object selector : list(value(annotation, "method", List.of()))) {
                            List<MethodNode> selected = select(target, (String) selector);
                            check(!selected.isEmpty(), context + " missing method " + selector);
                            for (MethodNode method : selected) {
                                check(isStatic(handler) == isStatic(method), context + " staticness changed");
                                verifyInjection(handler, annotation, kind, method, context);
                            }
                        }
                    }
                }
            }
        }
    }

    private static void verifyInjection(MethodNode handler, AnnotationNode annotation, String kind,
            MethodNode target, String context) {
        if (kind.equals("Inject;")) {
            String callback = Type.getReturnType(target.desc).equals(Type.VOID_TYPE)
                    ? "org/spongepowered/asm/mixin/injection/callback/CallbackInfo"
                    : "org/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable";
            List<Type> fullArgs = new ArrayList<>(Arrays.asList(Type.getArgumentTypes(target.desc)));
            fullArgs.add(Type.getObjectType(callback));
            List<Type> actual = Arrays.asList(Type.getArgumentTypes(handler.desc));
            check(Type.getReturnType(handler.desc).equals(Type.VOID_TYPE)
                            && (actual.equals(fullArgs) || actual.equals(List.of(Type.getObjectType(callback)))),
                    context + " callback signature mismatch");
        }
        int matches = 0;
        if (kind.equals("ModifyConstant;")) {
            for (Object constantEntry : list(value(annotation, "constant", List.of()))) {
                AnnotationNode constant = (AnnotationNode) constantEntry;
                Object expected = value(constant, "doubleValue", null);
                check(expected != null, context + " unsupported constant selector");
                check(handler.desc.equals("(D)D"), context + " constant handler signature mismatch");
                for (AbstractInsnNode instruction : target.instructions) {
                    if (instruction instanceof LdcInsnNode ldc && expected.equals(ldc.cst)) {
                        matches++;
                    }
                }
            }
        } else {
            for (Object atEntry : list(value(annotation, "at", List.of()))) {
                AnnotationNode at = (AnnotationNode) atEntry;
                String point = (String) value(at, "value", "");
                if (point.equals("INVOKE")) {
                    String invocation = (String) value(at, "target", "");
                    List<MethodInsnNode> calls = new ArrayList<>();
                    for (AbstractInsnNode instruction : target.instructions) {
                        if (instruction instanceof MethodInsnNode call
                                && invocation.equals("L" + call.owner + ";" + call.name + call.desc)) {
                            calls.add(call);
                        }
                    }
                    int ordinal = (int) value(at, "ordinal", -1);
                    check(ordinal < calls.size() && !calls.isEmpty(), context + " missing invocation " + invocation);
                    matches += ordinal < 0 ? calls.size() : 1;
                    if (kind.equals("Redirect;")) {
                        MethodInsnNode call = calls.get(Math.max(0, ordinal));
                        List<Type> args = new ArrayList<>();
                        if (call.getOpcode() != Opcodes.INVOKESTATIC) {
                            args.add(Type.getObjectType(call.owner));
                        }
                        args.addAll(Arrays.asList(Type.getArgumentTypes(call.desc)));
                        check(handler.desc.equals(Type.getMethodDescriptor(Type.getReturnType(call.desc),
                                args.toArray(Type[]::new))), context + " redirect signature mismatch");
                    }
                } else if (point.equals("HEAD")) {
                    check(target.instructions.size() > 0, context + " empty injection target");
                    matches++;
                } else if (point.equals("RETURN") || point.equals("TAIL")) {
                    int returns = 0;
                    for (AbstractInsnNode instruction : target.instructions) {
                        if (instruction.getOpcode() >= Opcodes.IRETURN && instruction.getOpcode() <= Opcodes.RETURN) {
                            returns++;
                        }
                    }
                    check(returns > 0, context + " no return instruction");
                    matches += point.equals("TAIL") ? 1 : returns;
                } else {
                    throw new AssertionError(context + " unsupported injection point: " + point);
                }
            }
        }
        int required = Math.max(1, (int) value(annotation, "require", 1));
        int allowed = (int) value(annotation, "allow", -1);
        check(matches >= required, context + " too few injection sites: " + matches);
        check(allowed < 0 || matches <= Math.max(required, allowed), context + " too many injection sites: " + matches);
    }

    private static List<MethodNode> select(ClassNode target, String selector) {
        int descriptorStart = selector.indexOf('(');
        String name = descriptorStart < 0 ? selector : selector.substring(0, descriptorStart);
        return target.methods.stream().filter(m -> m.name.equals(name)
                && (descriptorStart < 0 || m.desc.equals(selector.substring(descriptorStart)))).toList();
    }

    private static boolean isStatic(MethodNode method) {
        return (method.access & Opcodes.ACC_STATIC) != 0;
    }

    private static List<AnnotationNode> annotations(List<AnnotationNode> visible, List<AnnotationNode> invisible) {
        List<AnnotationNode> result = new ArrayList<>();
        if (visible != null) result.addAll(visible);
        if (invisible != null) result.addAll(invisible);
        return result;
    }

    private static Object value(AnnotationNode annotation, String key, Object fallback) {
        if (annotation.values != null) {
            for (int i = 0; i < annotation.values.size(); i += 2) {
                if (annotation.values.get(i).equals(key)) return annotation.values.get(i + 1);
            }
        }
        return fallback;
    }

    private static List<?> list(Object value) {
        return value instanceof List<?> values ? values : List.of(value);
    }

    private static ClassNode readClass(String name) throws IOException {
        try (InputStream input = resource(name + ".class")) {
            ClassNode result = new ClassNode();
            new ClassReader(input).accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return result;
        }
    }

    private static InputStream resource(String path) {
        InputStream input = MixinCompatibilityRegression.class.getClassLoader().getResourceAsStream(path);
        if (input == null) throw new AssertionError("Missing compatibility resource: " + path);
        return input;
    }

    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }
}
