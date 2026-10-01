package ankideckbuilder.shared

interface PlatformDependency {
    val name: String
}

expect fun getPlatform(): PlatformDependency
