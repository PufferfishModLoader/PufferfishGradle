package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.remap;

import com.google.common.collect.Lists;
import net.md_5.specialsource.AccessMap;
import net.md_5.specialsource.JarRemapper;
import net.md_5.specialsource.NodeType;
import org.objectweb.asm.Opcodes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class MappingAccessMap extends AccessMap {
    private Map<String, List<AccessTransformerEntry>> entries = new HashMap<>();
    private JarRemapper remapper;

    @Override
    public void addAccessChange(String line) {
        AccessTransformerEntry entry = AccessTransformerEntry.parseATLine(line);
        entries.computeIfAbsent(entry.getClassName().replace('.', '/'), e -> Lists.newArrayList()).add(entry);
    }

    @Override
    public int applyClassAccess(String className, int access) {
        return apply(className, entry -> entry.getType() == AccessTransformerEntry.Type.CLASS, access);
    }

    @Override
    public int applyFieldAccess(String className, String fieldName, int access) {
        String mappedFieldName = remapper.jarMapping.tryClimb(remapper.jarMapping.fields, NodeType.FIELD, className, fieldName, access);
        return apply(className, entry -> entry.getType() == AccessTransformerEntry.Type.FIELD && entry.getFieldName().equals(mappedFieldName), access);
    }

    @Override
    public int applyMethodAccess(String className, String methodName, String methodDesc, int access) {
        String mappedMethodName = remapper.mapMethodName(className, methodName, methodDesc, access);
        String mappedMethodDesc = remapper.mapMethodDesc(methodDesc);
        return apply(className, entry -> entry.getType() == AccessTransformerEntry.Type.METHOD
                && (entry.getMethodName().equals("*") || entry.getMethodName().equals(mappedMethodName))
                && (entry.getMethodDesc().equals("()") || entry.getMethodDesc().equals(mappedMethodDesc)), access);
    }

    private int apply(String className, Predicate<AccessTransformerEntry> predicate, int access) {
        List<AccessTransformerEntry> list = entries.get(remapper.map(className));
        if (list != null) {
            for (AccessTransformerEntry entry : list) {
                if (predicate.test(entry)) {
                    switch (entry.getNewAccess()) {
                        case PUBLIC:
                            access |= Opcodes.ACC_PUBLIC;
                            access &= ~Opcodes.ACC_PRIVATE;
                            access &= ~Opcodes.ACC_PROTECTED;
                            break;
                        case PROTECTED:
                            access |= Opcodes.ACC_PROTECTED;
                            access &= ~Opcodes.ACC_PRIVATE;
                            access &= ~Opcodes.ACC_PUBLIC;
                            break;
                        case PRIVATE:
                            access |= Opcodes.ACC_PRIVATE;
                            access &= ~Opcodes.ACC_PUBLIC;
                            access &= ~Opcodes.ACC_PROTECTED;
                            break;
                        case DEFAULT:
                            access &= ~Opcodes.ACC_PUBLIC;
                            access &= ~Opcodes.ACC_PRIVATE;
                            access &= ~Opcodes.ACC_PROTECTED;
                            break;
                    }
                    switch (entry.getFinalBehaviour()) {
                        case KEEP:
                            break;
                        case ADD:
                            access |= Opcodes.ACC_FINAL;
                            break;
                        case REMOVE:
                            access &= ~Opcodes.ACC_FINAL;
                            break;
                    }
                }
            }
        }
        return access;
    }

    public void setRemapper(JarRemapper remapper) {
        this.remapper = remapper;
    }
}
