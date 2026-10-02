import java.util.Properties
import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.android.build.api.instrumentation.InstrumentationScope
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose) apply false
}

// Explicit local input for both terminal builds and Android Studio Run.
val localProperties = Properties().apply {
    val propertiesFile = rootProject.file("local.properties")
    if (propertiesFile.isFile) propertiesFile.inputStream().use { load(it) }
}
val localAuthenticationAssets = providers.environmentVariable("DIPLAY_AUTH_ASSETS_DIR")
    .orNull ?: localProperties.getProperty("diplay.auth.assets.dir")
val authenticationAssetsDirectory = localAuthenticationAssets?.let { file(it).canonicalFile }

val kitkat = providers.gradleProperty("kitkat").map(String::toBoolean).getOrElse(false)

if (kitkat) layout.buildDirectory.set(layout.projectDirectory.dir("build/kitkat"))

if (!kitkat) apply(plugin = "org.jetbrains.kotlin.plugin.compose")

android {
    namespace = "com.shilapi.xcertplay"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = if (kitkat) "cn.manstep.phonemirrorBox" else "com.xyz.dashflow"
        minSdk = if (kitkat) 19 else 28
        targetSdk = 37
        versionCode = 25
        versionName = "0.2.6"
        if (kitkat) {
            versionNameSuffix = "-kitkat"
            ndk { abiFilters += "armeabi-v7a" }
            multiDexEnabled = true
        }

    }


    authenticationAssetsDirectory?.let { sourceSets.getByName("main").assets.srcDir(it) }

    signingConfigs {
        create("release") {
            storeFile = file(
                providers.environmentVariable("ANDROID_KEYSTORE_PATH")
                    .getOrElse("missing-release-keystore.jks"),
            )
            storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").getOrElse("")
            keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").getOrElse("")
            keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").getOrElse("")
        }
    }

    buildTypes {
        debug {
            if (!kitkat) applicationIdSuffix = ".hudtest"
            versionNameSuffix = "-hud-test"
        }
        release {
            optimization {
                enable = false
            }
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = kitkat
    }
    lint {
        checkDependencies = kitkat
        if (kitkat) checkOnly.addAll(listOf("NewApi", "InlinedApi", "MissingClass"))
    }

    buildFeatures {
        compose = !kitkat
    }
}

dependencies {
    if (kitkat) {
        implementation("androidx.activity:activity-ktx:1.8.0")
        implementation("androidx.core:core-ktx:1.13.1")
        coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
    } else {
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.activity.compose)
        implementation(libs.androidx.compose.material3)
        implementation(libs.androidx.compose.ui)
        implementation(libs.androidx.compose.ui.graphics)
        implementation(libs.androidx.compose.ui.tooling.preview)
        implementation(libs.androidx.core.ktx)
        debugImplementation(libs.androidx.compose.ui.tooling)
    }
    implementation(project(":common"))
    implementation(project(":shared"))
    if (!kitkat) implementation(libs.androidx.app.projected)
    if (kitkat) implementation("androidx.multidex:multidex:2.0.1")
    implementation(libs.androidx.lifecycle.runtime.ktx)
}

// No implicit import. Only the two explicitly selected local runtime assets are allowed.
val credentialAssets = files(android.sourceSets.flatMap { source ->
    source.assets.directories.map { directory ->
        fileTree(directory) {
            include("**/offline-mfi/**", "**/*.pk8", "**/*.p7b", "**/*.key",
                "**/*.pem", "**/*.p12", "**/*.pfx", "**/*.jks", "**/*.keystore")
        }
    }
})
val rejectBundledCredentials by tasks.registering {
    group = "verification"
    description = "Reject unexpected credential files in APK assets."
    val filesToCheck = credentialAssets
    val allowed = authenticationAssetsDirectory?.let { dir ->
        listOf("identity.pk8", "certificate.p7b").map { dir.resolve("offline-mfi/$it").canonicalFile }.toSet()
    } ?: emptySet()
    inputs.files(filesToCheck)
    doLast {
        check(allowed.all { it.isFile }) { "Explicit local authentication assets are incomplete" }
        val unexpected = filesToCheck.files.filter { it.canonicalFile !in allowed }
        check(unexpected.isEmpty()) { "Unexpected credential files in APK assets" }
    }
}
tasks.named("preBuild") { dependsOn(rejectBundledCredentials) }

// Car-test packages must be standalone. Keep ordinary source/CI builds identity-free.
val verifyStandaloneAuthentication by tasks.registering {
    group = "verification"
    description = "Require the explicit runtime authentication input for a standalone car-test APK."
    val directory = authenticationAssetsDirectory
    doLast {
        check(directory != null) {
            "Standalone car builds require DIPLAY_AUTH_ASSETS_DIR or diplay.auth.assets.dir in local.properties."
        }
        check(listOf("identity.pk8", "certificate.p7b").all {
            directory.resolve("offline-mfi/$it").let { file -> file.isFile && file.length() > 0 }
        }) { "Standalone CarPlay authentication files are missing or empty" }
    }
}
tasks.named("preBuild") { mustRunAfter(verifyStandaloneAuthentication) }
tasks.register("assembleStandaloneDebug") {
    group = "build"
    description = "Build a standalone car-test APK with explicitly provisioned authentication."
    dependsOn(verifyStandaloneAuthentication, "assembleDebug")
}

if (kitkat) {
    androidComponents.onVariants { variant ->
        variant.instrumentation.transformClassesWith(KitkatMdnsSockets::class.java, InstrumentationScope.ALL) {}
    }
    android {
        sourceSets {
            named("main") { manifest.srcFile("src/kitkat/AndroidManifest.xml") }
            named("debug") {
                java.directories.clear()
                kotlin.directories.clear()
                manifest.srcFile("src/kitkat/DebugAndroidManifest.xml")
            }
        }
    }
}

// Keep the pinned JmDNS library; fix its socket construction only in the KitKat APK.
abstract class KitkatMdnsSockets : AsmClassVisitorFactory<InstrumentationParameters.None> {
    override fun isInstrumentable(classData: ClassData) = classData.className == "javax.jmdns.impl.JmDNSImpl"

    override fun createClassVisitor(classContext: ClassContext, nextClassVisitor: ClassVisitor) =
        object : ClassVisitor(Opcodes.ASM9, nextClassVisitor) {
            override fun visitMethod(access: Int, name: String, descriptor: String,
                signature: String?, exceptions: Array<out String>?): MethodVisitor {
                val next = super.visitMethod(access, name, descriptor, signature, exceptions)
                if (name != "openMulticastSocket") return next
                return object : MethodVisitor(Opcodes.ASM9, next) {
                    override fun visitTypeInsn(opcode: Int, type: String) {
                        super.visitTypeInsn(opcode, if (opcode == Opcodes.NEW && type == "java/net/MulticastSocket") SOCKET else type)
                    }
                    override fun visitMethodInsn(opcode: Int, owner: String, name: String,
                        descriptor: String, isInterface: Boolean) {
                        super.visitMethodInsn(opcode, if (owner == "java/net/MulticastSocket" && name == "<init>") SOCKET else owner,
                            name, descriptor, isInterface)
                    }
                }
            }
        }

    companion object { private const val SOCKET = "com/shilapi/xcertplay/network/KitkatMdnsSocket" }
}
