// Top-level build file — apenas declara os plugins disponíveis para os submódulos.
// Cada plugin é aplicado no módulo que precisar dele (apply false aqui).
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
