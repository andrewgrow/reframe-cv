package ankideckbuilder.shared

class JVMPlatform : PlatformDependency {
    override val name: String = "Java ${System.getProperty("java.version")}"
}

actual fun getPlatform(): PlatformDependency = JVMPlatform()
