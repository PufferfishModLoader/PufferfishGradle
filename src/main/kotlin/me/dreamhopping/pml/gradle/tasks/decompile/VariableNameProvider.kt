package me.dreamhopping.pml.gradle.tasks.decompile

/*import me.dreamhopping.pml.gradle.mappings.Mapping
import me.dreamhopping.pml.gradle.tasks.map.InheritanceMap
import me.dreamhopping.pml.gradle.tasks.map.PGRemapper
import org.jetbrains.java.decompiler.main.DecompilerContext
import org.jetbrains.java.decompiler.main.extern.IVariableNameProvider
import org.jetbrains.java.decompiler.main.extern.IVariableNamingFactory
import org.jetbrains.java.decompiler.modules.decompiler.vars.VarVersionPair
import org.jetbrains.java.decompiler.struct.StructMethod

class VariableNameProvider(private val map: Mapping.ClassMap.MethodMap?) : IVariableNameProvider {
    private var idx = 0

    override fun rename(variables: MutableMap<VarVersionPair, String>) = hashMapOf<VarVersionPair, String?>().also {
        val entries = variables.entries.sortedBy { it.key.version }
        entries.forEach { (key, _) ->
            it[key] = map?.locals?.get("var$idx") ?: "var$idx"
            idx++
        }
    }

    override fun renameAbstractParameter(abstractParam: String, index: Int) = abstractParam.also {
        return (map?.locals?.get("var$idx") ?: "var$idx").also { idx++ }
    }

    override fun addParentContext(renamer: IVariableNameProvider?) {

    }

    class Factory : IVariableNamingFactory {
        override fun createFactory(structMethod: StructMethod): IVariableNameProvider {
            structMethod.classStruct.allSuperClasses

            return mappings.classes.values
                .find {
                    it.mappedName == structMethod.classStruct.qualifiedName
                }
                ?.methods?.entries?.find { (obf, map) ->
                    val idx = obf.indexOf('/')

                    remapper.mapMethodDesc(obf.substring(idx + 1)) == structMethod.descriptor
                            && map.mappedName == structMethod.name
                }
                ?.value.let { VariableNameProvider(it) }
        }
    }

    companion object {
        private val inheritance: InheritanceMap = hashMapOf()
        val mappings = Mapping()
        private val remapper = PGRemapper(mappings, inheritance) {
            DecompilerContext.getStructContext().getClass(it)?.let { cl ->
                setOf(*cl.interfaceNames, cl.superClass?.string).filterNotNull().toSet()
            } ?: emptySet()
        }
    }
}*/