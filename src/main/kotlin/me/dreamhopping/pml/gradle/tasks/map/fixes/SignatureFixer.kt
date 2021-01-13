package me.dreamhopping.pml.gradle.tasks.map.fixes

import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.commons.SignatureRemapper
import org.objectweb.asm.signature.SignatureVisitor
import java.util.*

class SignatureFixer(visitor: SignatureVisitor?, mapper: Remapper) : SignatureRemapper(visitor, mapper) {
    private val classNames = Stack<String>()

    override fun visitClassType(name: String) {
        classNames.push(name)
        super.visitClassType(name)
    }

    override fun visitInnerClassType(name: String) {
        val outer = classNames.pop()

        val fixedName = name.takeUnless { it.startsWith("$outer\$") } ?: name.substring(outer.length + 1)

        classNames.push("$outer\$$fixedName")
        super.visitInnerClassType(fixedName)
    }

    override fun visitEnd() {
        classNames.pop()
        super.visitEnd()
    }
}