# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in the SDK tools.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Compose
-keep class androidx.compose.** { *; }

# Keep ZeroTier SDK classes
-keep class com.zerotier.sockets.** { *; }
-dontwarn com.zerotier.sockets.**

# Keep ZeroTier VPN Service
-keep class com.catchuppos.app.network.ZeroTierVpnService { *; }
-keep class com.catchuppos.app.network.ZeroTierManager { *; }
-keep class com.catchuppos.app.network.ZeroTierSettingsManager { *; }
-keep class com.catchuppos.app.network.ZeroTierApiClient { *; }

# Keep foreground service
-keep class com.catchuppos.app.service.NetworkService { *; }
