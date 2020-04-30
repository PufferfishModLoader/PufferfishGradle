package dev.cbyrne.pufferfishmodloader.gradle.tasks.minecraft.remap;

import org.objectweb.asm.Opcodes;

import java.util.regex.Pattern;

public class AccessTransformerEntry {
    private static final Pattern COMMENT_REMOVAL_PATTERN = Pattern.compile("#[^\n]*");

    private final Type type;
    private final NewAccess newAccess;
    private final FinalTransformationType finalBehaviour;
    private final String className;
    private final String methodName;
    private final String methodDesc;
    private final String fieldName;

    public AccessTransformerEntry(Type type, NewAccess newAccess, FinalTransformationType finalBehaviour, String className, String methodName, String methodDesc, String fieldName) {
        this.type = type;
        this.newAccess = newAccess;
        this.finalBehaviour = finalBehaviour;
        this.className = className;
        this.methodName = methodName;
        this.methodDesc = methodDesc;
        this.fieldName = fieldName;
    }

    public Type getType() {
        return type;
    }

    public NewAccess getNewAccess() {
        return newAccess;
    }

    public FinalTransformationType getFinalBehaviour() {
        return finalBehaviour;
    }

    public String getClassName() {
        return className;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public String getFieldName() {
        return fieldName;
    }

    @Override
    public String toString() {
        return "AccessTransformerEntry{" +
                "type=" + type +
                ", newAccess=" + newAccess +
                ", finalBehaviour=" + finalBehaviour +
                ", className='" + className + '\'' +
                ", methodName='" + methodName + '\'' +
                ", methodDesc='" + methodDesc + '\'' +
                ", fieldName='" + fieldName + '\'' +
                '}';
    }

    public static AccessTransformerEntry parseATLine(String line) {
        line = COMMENT_REMOVAL_PATTERN.matcher(line).replaceAll("");

        NewAccess access;
        FinalTransformationType finalBehaviour = FinalTransformationType.KEEP;
        if (line.startsWith("public")) {
            access = NewAccess.PUBLIC;
        } else if (line.startsWith("protected")) {
            access = NewAccess.PROTECTED;
        } else if (line.startsWith("private")) {
            access = NewAccess.PRIVATE;
        } else if (line.startsWith("default")) {
            access = NewAccess.DEFAULT;
        } else {
            throw new IllegalArgumentException("Invalid AT entry");
        }
        line = line.substring(access.name().length());
        int toRemove = 1;
        if (!line.startsWith(" ")) {
            toRemove++;
            if (line.startsWith("-f")) {
                finalBehaviour = FinalTransformationType.REMOVE;
            } else if (line.startsWith("+f")) {
                finalBehaviour = FinalTransformationType.ADD;
            } else {
                throw new IllegalArgumentException("Invalid AT entry");
            }
        }
        line = line.substring(toRemove);

        String[] parts = line.split(" ");
        if (parts.length < 1 || parts.length > 2) throw new IllegalArgumentException("Invalid AT entry");

        Type type = null;
        String className;
        String methodName = null;
        String fieldName = null;
        StringBuilder methodDesc = new StringBuilder();

        className = parts[0];
        if (parts.length == 1) {
            type = Type.CLASS;
        } else {
            StringBuilder memberName = new StringBuilder();
            for (char c : parts[1].toCharArray()) {
                if (c == '(') {
                    type = Type.METHOD;
                    methodDesc = new StringBuilder("(");
                } else {
                    if (type == null) {
                        memberName.append(c);
                    } else {
                        methodDesc.append(c);
                    }
                }
            }
            if (type == null) {
                type = Type.FIELD;
                fieldName = memberName.toString();
            } else {
                methodName = memberName.toString();
            }
        }

        return new AccessTransformerEntry(
                type,
                access,
                finalBehaviour,
                className,
                methodName,
                methodDesc.toString(),
                fieldName
        );
    }

    public enum NewAccess {
        PUBLIC,
        PROTECTED,
        PRIVATE,
        DEFAULT // package private
    }

    public enum FinalTransformationType {
        KEEP,
        ADD,
        REMOVE
    }

    public enum Type {
        CLASS,
        FIELD,
        METHOD
    }
}
