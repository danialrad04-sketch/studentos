package com.example.data.api.backend

/**
 * Set this to your own backend's domain once setup.sh has finished
 * provisioning it (must end with a trailing slash, and must match both
 * the DOMAIN in the VPS's .env AND the <domain> entry in
 * network_security_config.xml exactly, or requests will fail SSL pinning).
 */
object BackendConfig {
    const val BASE_URL = "https://api.yourdomain.com/"
}
