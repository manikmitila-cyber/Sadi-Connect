package com.example.utils

import com.example.domain.model.AppLanguage

data class LocalizedText(val bn: String, val en: String) {
  fun get(lang: AppLanguage): String = if (lang == AppLanguage.BN) bn else en
}

object AppStrings {
  val tagline = LocalizedText("Connect • Control • Communicate", "Connect • Control • Communicate")
  val splashSubtitle =
    LocalizedText(
      "স্মার্ট সংযোগ\nউন্নত আগামীকালের জন্য",
      "Smart Connection\nFor a Better Tomorrow",
    )

  // Auth
  val signInTitle = LocalizedText("Sadi Connect-এ স্বাগতম", "Welcome to Sadi Connect")
  val signInSubtitle =
    LocalizedText(
      "নিরাপদ স্ক্রিন শেয়ারিং, রিমোট কন্ট্রোল, ভয়েস কল ও চ্যাটের জন্য আপনার Google অ্যাকাউন্ট দিয়ে সাইন ইন করুন।",
      "Sign in with your Google Account for secure screen sharing, remote control, voice calls, and chat.",
    )
  val signInWithGoogle = LocalizedText("Google দিয়ে সাইন ইন করুন", "Sign in with Google")
  val securityBadgeNote =
    LocalizedText(
      "প্রতিটি রিমোট সেশনের জন্য ডিভাইস মালিকের স্পষ্ট অনুমোদন আবশ্যক।",
      "Every remote session requires explicit approval from the device owner.",
    )

  // Bottom Navigation
  val navThisDevice = LocalizedText("এই ডিভাইস", "This Device")
  val navRemoteControl = LocalizedText("রিমোট কন্ট্রোল", "Remote Control")
  val navChat = LocalizedText("চ্যাট", "Chat")

  // This Device Screen
  val online = LocalizedText("Online", "Online")
  val offline = LocalizedText("Offline", "Offline")
  val logout = LocalizedText("লগআউট", "Logout")
  val generateSupportLink = LocalizedText("সাপোর্ট লিংক তৈরি করুন", "Generate Support Link")
  val nineDigitCode = LocalizedText("৯-ডিজিট কোড", "9-Digit Code")
  val copyCode = LocalizedText("কপি করুন", "Copy")
  val copiedToClipboard = LocalizedText("ক্লিপবোর্ডে কপি করা হয়েছে", "Copied to clipboard")
  val shareDevice = LocalizedText("শেয়ার করুন", "Share")
  val connectToDevice = LocalizedText("অন্য ডিভাইসে সংযোগ করুন", "Connect to Device")
  val scanQr = LocalizedText("QR স্ক্যান", "Scan QR")
  val localP2pHost = LocalizedText("Local P2P Host", "Local P2P Host")
  val accessibilityPermission = LocalizedText("Accessibility Permission", "Accessibility Permission")
  val screenCapturePermission = LocalizedText("Screen Capture Permission", "Screen Capture Permission")
  val active = LocalizedText("Active", "Active")
  val granted = LocalizedText("Granted", "Granted")
  val required = LocalizedText("প্রয়োজন", "Required")
  val ready = LocalizedText("Ready", "Ready")
  val connectedDevices = LocalizedText("সংযুক্ত ডিভাইস", "Connected Devices")
  val noConnectedDevices =
    LocalizedText(
      "এখনো কোনো ডিভাইস সংযুক্ত হয়নি। ৯-ডিজিট কোড বা QR ব্যবহার করে যুক্ত হন।",
      "No devices connected yet. Use a 9-digit code or QR to connect.",
    )
  val supportLinkCreated =
    LocalizedText("অস্থায়ী সাপোর্ট লিংক তৈরি হয়েছে (১ ঘণ্টা মেয়াদ)", "Temporary support link created (1h valid)")
  val copyLink = LocalizedText("লিংক কপি করুন", "Copy Link")
  val cancelLink = LocalizedText("সেশন বাতিল", "Cancel Session")

  // Remote Control Screen
  val remoteControl = LocalizedText("রিমোট কন্ট্রোল", "Remote Control")
  val disconnect = LocalizedText("সংযোগ বিচ্ছিন্ন", "Disconnect")
  val reconnect = LocalizedText("পুনঃসংযোগ", "Reconnect")
  val back = LocalizedText("পিছনে", "Back")
  val home = LocalizedText("হোম", "Home")
  val recent = LocalizedText("Recent", "Recent")
  val notification = LocalizedText("নোটিফিকেশন", "Notification")
  val brightness = LocalizedText("উজ্জ্বলতা", "Brightness")
  val volume = LocalizedText("ভলিউম", "Volume")
  val quality = LocalizedText("কোয়ালিটি", "Quality")
  val fps = LocalizedText("FPS", "FPS")
  val call = LocalizedText("কল", "Call")
  val camera = LocalizedText("ক্যামেরা", "Camera")
  val switchCamera = LocalizedText("ক্যামেরা বদল", "Switch Camera")
  val fullScreen = LocalizedText("ফুল স্ক্রিন", "Full Screen")
  val shareMyScreen = LocalizedText("আমার স্ক্রিন শেয়ার করুন", "Share My Screen")
  val stopSharing = LocalizedText("শেয়ারিং বন্ধ করুন", "Stop Sharing")
  val enter9DigitCode = LocalizedText("৯-ডিজিট ডিভাইস কোড বা লিংক লিখুন", "Enter 9-digit code or support link")
  val sendRequest = LocalizedText("সংযোগ অনুরোধ পাঠান", "Send Request")
  val waitingApproval =
    LocalizedText(
      "অপর ডিভাইসের মালিকের অনুমতির জন্য অপেক্ষা করা হচ্ছে...",
      "Waiting for remote device owner approval...",
    )
  val incomingRequestTitle = LocalizedText("নতুন সংযোগ অনুরোধ", "Incoming Connection Request")
  val accept = LocalizedText("অনুমোদন দিন", "Accept")
  val reject = LocalizedText("প্রত্যাখ্যান", "Reject")
  val allowTouchControl = LocalizedText("রিমোট টাচ কন্ট্রোল অনুমতি", "Allow Remote Touch Control")
  val restrictedScreenNotice =
    LocalizedText(
      "নিরাপত্তার কারণে পাসওয়ার্ড বা ব্যাংকিং স্ক্রিনে রিমোট কন্ট্রোল সীমাবদ্ধ থাকতে পারে।",
      "For security, protected banking or password screens may restrict capture/touch.",
    )

  // Voice Call
  val voiceCall = LocalizedText("ভয়েস কল", "Voice Call")
  val micMute = LocalizedText("মাইক (মিউট)", "Mic (Mute)")
  val micUnmute = LocalizedText("মাইক (চালু)", "Mic (Unmute)")
  val speaker = LocalizedText("স্পিকার", "Speaker")
  val keypad = LocalizedText("অডিও তথ্য", "Audio Stats")
  val endCall = LocalizedText("কল শেষ", "End Call")
  val ringing = LocalizedText("রিং হচ্ছে...", "Ringing...")
  val incomingCall = LocalizedText("ইনকামিং ভয়েস কল", "Incoming Voice Call")

  // Remote Camera
  val remoteCamera = LocalizedText("রিমোট ক্যামেরা", "Remote Camera")
  val frontBack = LocalizedText("ফ্রন্ট / ব্যাক", "Front / Back")
  val cameraOff = LocalizedText("ক্যামেরা বন্ধ", "Camera Off")
  val cameraOn = LocalizedText("ক্যামেরা চালু", "Camera On")
  val cameraConsentPrompt =
    LocalizedText(
      "রিমোট ডিভাইস আপনার ক্যামেরা প্রিভিউ দেখতে চাইছে। আপনি কি অনুমতি দেবেন?",
      "The remote peer is requesting camera preview access. Allow?",
    )

  // Chat
  val typeMessage = LocalizedText("মেসেজ লিখুন...", "Type a message...")
  val clearChat = LocalizedText("চ্যাট মুছুন", "Clear Chat")
  val noActiveSessionChat =
    LocalizedText(
      "রিয়েল-টাইম চ্যাট শুরু করতে প্রথমে একটি ডিভাইসের সাথে সংযুক্ত হোন।",
      "Connect with a device first to start real-time chat.",
    )

  // Permissions & Settings
  val permissionsAndSettings = LocalizedText("পারমিশন ও সেটিংস", "Permissions & Settings")
  val settings = LocalizedText("সেটিংস", "Settings")
  val connectionHistory = LocalizedText("সংযোগ ইতিহাস", "Connection History")
  val about = LocalizedText("সম্পর্কে", "About")
  val darkMode = LocalizedText("ডার্ক মোড", "Dark Mode")
  val languageSelection = LocalizedText("ভাষা নির্বাচন", "Language")
  val deviceManagement = LocalizedText("ডিভাইস ব্যবস্থাপনা", "Device Management")
  val notifications = LocalizedText("নোটিফিকেশন", "Notifications")
  val connectionSettings = LocalizedText("সংযোগ সেটিংস", "Connection Settings")
  val appUpdate = LocalizedText("অ্যাপ আপডেট", "App Update")
  val clearAll = LocalizedText("সব মুছে ফেলুন", "Clear All")
  val privacyPolicy = LocalizedText("Privacy Policy", "Privacy Policy")
  val helpAndSupport = LocalizedText("Help & Support", "Help & Support")
  val shareApp = LocalizedText("Share App", "Share App")
  val permissionGuideHint =
    LocalizedText(
      "কোনো সমস্যা হলে এখানে নির্দেশনা দেখুন",
      "Tap here for step-by-step permission guide",
    )
  val aboutDescription =
    LocalizedText(
      "Sadi Connect হলো একটি আধুনিক রিমোট কন্ট্রোল এবং স্ক্রিন শেয়ারিং অ্যাপ, যা আপনাকে সংযোগ রাখে সবসময়।",
      "Sadi Connect is a modern remote control and screen sharing app that keeps you securely connected anytime.",
    )

  val permissionGuideBody =
    LocalizedText(
      "১. Accessibility Permission: সেটিংস > Accessibility > Installed Services > Sadi Connect Remote Control চালু করুন। এটি ছাড়া রিমোট টাচ কাজ করবে না।\n\n" +
        "২. Screen Capture: রিমোট স্ক্রিন শেয়ারিং শুরু করতে 'Share My Screen' চাপলে Android সিস্টেম অনুমতি চাইবে।\n\n" +
        "৩. Camera ও Microphone: ভয়েস কল এবং রিমোট ক্যামেরা ব্যবহারের আগে স্পষ্ট অনুমতি প্রয়োজন।\n\n" +
        "৪. Notification: স্ক্রিন শেয়ারিং চলাকালে দৃশ্যমান নোটিফিকেশন দেখাতে এবং এক ক্লিকে বন্ধ করতে এটি প্রয়োজন।",
      "1. Accessibility Permission: Open Settings > Accessibility > Installed Services > Enable 'Sadi Connect Remote Control'. Remote touch requires this.\n\n" +
        "2. Screen Capture: Android will prompt for MediaProjection permission when you start screen sharing.\n\n" +
        "3. Camera & Microphone: Explicit runtime consent is required before voice calls or remote camera.\n\n" +
        "4. Notifications: Required on Android 13+ to display the active screen-sharing control notification.",
    )
}
