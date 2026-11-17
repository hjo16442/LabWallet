# Offline TRON Key Check

This is a separate Android application module (:offline) and does not replace the existing online LabWallet diagnostic app.

## Security design
- The offline app manifest declares no android.permission.INTERNET.
- It does not make RPC/API requests, sign transactions, export private keys, or modify on-chain permissions.
- It checks BIP39 English 12/24-word mnemonic checksum locally.
- It derives the standard TRON BIP44 path m/44'/195'/0'/0/index, indexes 0 through 100, and displays only public addresses.
- The optional BIP39 passphrase is processed locally.
- Backup is disabled in the manifest; the input fields are cleared after processing.

## Important limitations
- Clearing Android text fields is best-effort only. JVM strings and BigInteger objects cannot be guaranteed to be securely erased from memory.
- The keyboard/IME, clipboard, screen capture, rooted/compromised OS, accessibility services, and a tampered APK are outside this app's protection.
- This is a diagnostic tool, not a wallet. It cannot recover or bypass a TRON permission whose signing key is not derived from the user's mnemonic.
- A negative result does not prove the mnemonic is wrong: a different BIP39 passphrase, derivation path, account, or address index may have been used.
- A positive address match proves only that the entered mnemonic/passphrase derives that address. It does not by itself prove the current TRON permission can be changed or that a transfer can be signed.

Do not enter a real recovery phrase until the source and exact APK have been independently reviewed and the device is isolated (airplane mode, Wi-Fi and Bluetooth off).
