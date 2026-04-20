// Fichero de configuración de Gradle a nivel raíz del proyecto.
// Solo se declaran los plugins con apply false; se aplican individualmente en cada módulo.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
}
