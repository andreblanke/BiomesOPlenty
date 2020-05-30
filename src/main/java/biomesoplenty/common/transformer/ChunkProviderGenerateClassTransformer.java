package biomesoplenty.common.transformer;

import java.util.ListIterator;
import java.util.NoSuchElementException;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.world.WorldType;

import org.apache.logging.log4j.LogManager;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import biomesoplenty.common.core.BOPBiomes;

import static org.objectweb.asm.Opcodes.*;

/** {@link net.minecraft.world.gen.ChunkProviderGenerate} */
public final class ChunkProviderGenerateClassTransformer implements IClassTransformer
{
    private static final String TARGET_CLASS = "net.minecraft.world.gen.ChunkProviderGenerate";

    @Override
    public byte[] transform(final String name, final String transformedName, final byte[] basicClass)
    {
        if (!transformedName.equals(TARGET_CLASS))
            return basicClass;

        final ClassNode node     = new ClassNode();
        final ClassReader reader = new ClassReader(basicClass);
        reader.accept(node, 0);

        LogManager.getLogger().info("Transforming {}", TARGET_CLASS);

        for (MethodNode method : node.methods)
        {
            if (method.name.equals(BiomesOPlentyCore.isObfuscated() ? "a"  : "func_147423_a")
                    && method.desc.equals("(III)V"))
            {
                transformFunc_147423_a(method);
                break;
            }
        }
        final ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        node.accept(writer);

        return writer.toByteArray();
    }

    @SuppressWarnings("SpellCheckingInspection")
    private void transformFunc_147423_a(final MethodNode method) {
        final boolean obfuscated = BiomesOPlentyCore.isObfuscated();

        final FieldInsnNode fieldInsnNode =
            findMatchingInsn(method.instructions, FieldInsnNode.class, (insnNode, index) ->
                insnNode.getOpcode() == GETSTATIC
                    && insnNode.owner.equals(obfuscated ? "ahm"   : "net/minecraft/world/WorldType")
                    && insnNode.name.equals(obfuscated  ? "e"     : "field_151360_e")
                    && insnNode.desc.equals(obfuscated  ? "Lahm;" : "Lnet/minecraft/world/WorldType;"));

        final InsnList worldTypeBOPAmplifiedCheckInsnList = new InsnList();
        worldTypeBOPAmplifiedCheckInsnList.add(
            new MethodInsnNode(
                INVOKESTATIC,
                Type.getInternalName(getClass()),
                "isAmplifiedHook",
                "(Lnet/minecraft/world/WorldType;)Z",
                false));
        worldTypeBOPAmplifiedCheckInsnList.add(
            new JumpInsnNode(
                IFEQ,
                ((JumpInsnNode) fieldInsnNode.getNext()).label));

        method.instructions.insertBefore(fieldInsnNode, worldTypeBOPAmplifiedCheckInsnList);

        method.instructions.remove(fieldInsnNode.getNext());
        method.instructions.remove(fieldInsnNode);
    }

    @FunctionalInterface
    private interface InsnNodePredicate<T extends AbstractInsnNode> {
        boolean test(T insnNode, int index);
    }

    @SuppressWarnings({ "SameParameterValue", "unchecked" })
    private static <T extends AbstractInsnNode> T findMatchingInsn(final InsnList insnList,
                                                                   final Class<T> clazz,
                                                                   final InsnNodePredicate<T> predicate) {
        final ListIterator<AbstractInsnNode> iterator = insnList.iterator();

        int i = 0;
        while (iterator.hasNext()) {
            final AbstractInsnNode insnNode = iterator.next();
            if (clazz.isInstance(insnNode) && predicate.test((T) insnNode, i++))
                return (T) insnNode;
        }
        throw new NoSuchElementException();
    }

    @SuppressWarnings("unused")
    public static boolean isAmplifiedHook(final WorldType worldType) {
        return (worldType == WorldType.AMPLIFIED) || (worldType == BOPBiomes.worldTypeBOPAmplified);
    }
}
