package app.vireo.data.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfilePinValidatorTest {

    @Test
    fun emptyPinIsAllowedForOptionalOrUnchangedPin() {
        assertTrue(ProfilePinValidator.isValid(null))
        assertTrue(ProfilePinValidator.isValid(""))
    }

    @Test
    fun exactlyFourAsciiDigitsAreAllowed() {
        assertTrue(ProfilePinValidator.isValid("1234"))
        assertTrue(ProfilePinValidator.isValid("0000"))
    }

    @Test
    fun wrongLengthIsRejected() {
        assertFalse(ProfilePinValidator.isValid("1"))
        assertFalse(ProfilePinValidator.isValid("123"))
        assertFalse(ProfilePinValidator.isValid("12345"))
    }

    @Test
    fun nondigitCharactersAreRejected() {
        assertFalse(ProfilePinValidator.isValid("12a4"))
        assertFalse(ProfilePinValidator.isValid("12 4"))
        assertFalse(ProfilePinValidator.isValid("１２３４"))
    }

    @Test
    fun verifyRequiresExactlyFourDigits() {
        assertTrue(ProfilePinValidator.isFourDigitPin("1234"))
        assertFalse(ProfilePinValidator.isFourDigitPin(""))
        assertFalse(ProfilePinValidator.isFourDigitPin("123"))
        assertFalse(ProfilePinValidator.isFourDigitPin("12a4"))
    }
}
