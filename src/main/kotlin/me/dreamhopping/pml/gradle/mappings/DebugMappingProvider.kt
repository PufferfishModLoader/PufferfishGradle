package me.dreamhopping.pml.gradle.mappings

import me.dreamhopping.pml.gradle.data.mappings.Mappings
import org.gradle.api.Project

object DebugMappingProvider : MappingProvider {
    override val id = "debug"
    override val mappings = Mappings.mappings {
        className("a", "net/minecraft/network/packet/client/UseEntityPacket")
        field("a", "a", "playerEntityId")
        method("a", "a", "(Ljava/io/DataInputStream;)V", "read") {
            local("var1", "input")
        }
        method("abs", "a", "(Ljava/io/DataOutputStream;)V", "write") {
            local("var1", "output")
        }
        field("abs", "b", "testInheritance")
    }

    override fun load(project: Project, minecraftVersion: String) {

    }
}