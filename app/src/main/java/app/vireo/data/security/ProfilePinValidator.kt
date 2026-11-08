package app.vireo.data.security

object ProfilePinValidator {
    fun isValid(pin: String?): Boolean {
        if (pin.isNullOrEmpty()) return true
        return pin.length == 4 && pin.all { it in '0'..'9' }
    }

    fun isFourDigitPin(pin: String): Boolean =
        pin.length == 4 && pin.all { it in '0'..'9' }
}
