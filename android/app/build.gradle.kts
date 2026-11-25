import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Android menolak Markdown di res/values dan res/xml. Simpan dokumentasi di source,
// lalu compile salinan resource yang hanya berisi file Android yang valid.
val prepareMainResources by tasks.registering(Sync::class) {
    from("src/main/res") { exclude("**/*.md") }
    into(layout.buildDirectory.dir("generated/loopyRes/main"))
}

android {
    namespace = "id.ac.loopy"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        applicationId = "id.ac.loopy"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-template"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        debug {
            // Emulator Android memakai 10.0.2.2 untuk mengakses localhost host.
            val apiUrl = providers.gradleProperty("LOOPY_API_BASE_URL")
                .getOrElse("http://10.0.2.2:8080/")
            require(apiUrl.endsWith("/")) { "LOOPY_API_BASE_URL harus diakhiri /" }
            buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        }
        release {
            // Build verifikasi bisa berjalan tanpa endpoint deployment.
            val property = "LOOPY_RELEASE_API_BASE_URL"
            val apiUrl = providers.gradleProperty(property).getOrElse("https://api.example.invalid/")
            val invalidUrl = "$property harus berupa base URL HTTPS yang diakhiri /"
            val uri = requireNotNull(runCatching { URI(apiUrl) }.getOrNull()) { invalidUrl }
            require(
                uri.scheme == "https" && uri.host != null && uri.userInfo == null &&
                    uri.query == null && uri.fragment == null && apiUrl.endsWith("/"),
            ) { invalidUrl }
            buildConfigField("String", "API_BASE_URL", "\"${uri.toASCIIString()}\"")
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    buildFeatures { compose = true; buildConfig = true }
    sourceSets.getByName("main").res.setSrcDirs(listOf(prepareMainResources.map { it.destinationDir }))
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}
tasks.named("preBuild") { dependsOn(prepareMainResources) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.preview)
    implementation(libs.compose.material3)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.coroutines.android)
    implementation(libs.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    debugImplementation(libs.compose.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(libs.compose.test)
    debugImplementation(libs.compose.test.manifest)
}
