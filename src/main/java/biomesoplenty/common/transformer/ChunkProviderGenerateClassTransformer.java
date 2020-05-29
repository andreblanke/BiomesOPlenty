package biomesoplenty.common.transformer;

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
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import biomesoplenty.common.core.BOPBiomes;

import static org.objectweb.asm.Opcodes.*;

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

        final String targetMethodName = BiomesOPlentyCore.isObfuscated() ? "a"  : "func_147423_a";
        for (MethodNode method : node.methods)
        {
            if (method.name.equals(targetMethodName) && method.desc.equals("(III)V"))
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
    private void transformFunc_147423_a(final MethodNode method)
    {
        final boolean obfuscated = BiomesOPlentyCore.isObfuscated();

        for (AbstractInsnNode abstractInsn : method.instructions.toArray())
        {
            if (abstractInsn.getOpcode() != GETSTATIC)
                continue;
            final FieldInsnNode insn = (FieldInsnNode) abstractInsn;

            if (!insn.owner.equals(obfuscated ? "ahm" : "net/minecraft/world/WorldType")
                    || !insn.name.equals(obfuscated ? "e" : "field_151360_e")
                    || !insn.desc.equals(obfuscated ? "Lahm;" : "Lnet/minecraft/world/WorldType;"))
                continue;
            final LabelNode amplifiedHandlerLabel = new LabelNode();

            final InsnList isAmplifiedInsn = new InsnList();
            isAmplifiedInsn.add(
                new MethodInsnNode(
                    INVOKESTATIC,
                        Type.getInternalName(getClass()),
                        "isAmplified",
                        "(Lnet/minecraft/world/WorldType;)Z",
                        false));
            isAmplifiedInsn.add(
                new JumpInsnNode(IFNE, amplifiedHandlerLabel));

            method.instructions.insertBefore(insn, isAmplifiedInsn);
            method.instructions.insert(insn.getNext(), amplifiedHandlerLabel);

            /* 272: getstatic     #97 */
            method.instructions.remove(insn.getNext());
            /* 275: if_acmpne     302 */
            method.instructions.remove(insn);
            return;
        }
    }

    public static boolean isAmplified(final WorldType worldType)
    {
        return (worldType == WorldType.AMPLIFIED) || (worldType == BOPBiomes.worldTypeBOPAmplified);
    }
}
