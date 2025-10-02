package cz.deels.admin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform