package com.roadtrippin.shared.cloud

import cnames.structs.__CFData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.cinterop.UByteVar
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDataGetBytes
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRangeMake
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFTypeRefVar
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

@OptIn(ExperimentalForeignApi::class)
actual object SecureSessionStorage {
    private const val SERVICE = "com.roadtrippin.app.supabase"
    private const val ACCOUNT = "current-session"

    actual fun save(value: String) {
        delete()
        val query = CFDictionaryCreateMutable(null, 0, null, null) ?: error("Could not create Keychain query")
        val service = CFStringCreateWithCString(null, SERVICE, kCFStringEncodingUTF8)
        val account = CFStringCreateWithCString(null, ACCOUNT, kCFStringEncodingUTF8)
        val bytes = value.encodeToByteArray()
        val data = bytes.usePinned { pinned ->
            CFDataCreate(null, pinned.addressOf(0).reinterpret(), bytes.size.toLong())
        }
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
            CFDictionarySetValue(query, kSecAttrAccessible, kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly)
            CFDictionarySetValue(query, kSecValueData, data)
            check(SecItemAdd(query, null) == errSecSuccess) { "Could not save the secure session" }
        } finally {
            data?.let(::CFRelease)
            service?.let(::CFRelease)
            account?.let(::CFRelease)
            CFRelease(query)
        }
    }

    actual fun load(): String? = memScoped {
        val query = CFDictionaryCreateMutable(null, 0, null, null) ?: return null
        val service = CFStringCreateWithCString(null, SERVICE, kCFStringEncodingUTF8)
        val account = CFStringCreateWithCString(null, ACCOUNT, kCFStringEncodingUTF8)
        val result = alloc<CFTypeRefVar>()
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
            CFDictionarySetValue(query, kSecReturnData, kCFBooleanTrue)
            CFDictionarySetValue(query, kSecMatchLimit, kSecMatchLimitOne)
            if (SecItemCopyMatching(query, result.ptr) != errSecSuccess) return null
            val data = result.value?.reinterpret<__CFData>() ?: return null
            try {
                val length = CFDataGetLength(data).toInt()
                val bytes = ByteArray(length)
                if (length > 0) {
                    bytes.usePinned { pinned ->
                        CFDataGetBytes(
                            data,
                            CFRangeMake(0, length.toLong()),
                            pinned.addressOf(0).reinterpret<UByteVar>(),
                        )
                    }
                }
                bytes.decodeToString()
            } finally {
                CFRelease(data)
            }
        } finally {
            service?.let(::CFRelease)
            account?.let(::CFRelease)
            CFRelease(query)
        }
    }

    actual fun delete() {
        val query = CFDictionaryCreateMutable(null, 0, null, null) ?: return
        val service = CFStringCreateWithCString(null, SERVICE, kCFStringEncodingUTF8)
        val account = CFStringCreateWithCString(null, ACCOUNT, kCFStringEncodingUTF8)
        try {
            CFDictionarySetValue(query, kSecClass, kSecClassGenericPassword)
            CFDictionarySetValue(query, kSecAttrService, service)
            CFDictionarySetValue(query, kSecAttrAccount, account)
            SecItemDelete(query)
        } finally {
            service?.let(::CFRelease)
            account?.let(::CFRelease)
            CFRelease(query)
        }
    }
}
