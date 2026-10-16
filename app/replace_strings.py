import os
import re

files = {
    'app/src/main/java/com/example/ui/SettingsScreen.kt': [
        (r'"Search settings"', r'LocalTranslation.current.searchSettings'),
        (r'"Account"', r'LocalTranslation.current.account'),
        (r'"Privacy"', r'LocalTranslation.current.privacy'),
        (r'"Notifications"', r'LocalTranslation.current.notifications'),
        (r'"Storage & Data"', r'LocalTranslation.current.storageAndData'),
        (r'"Appearance"', r'LocalTranslation.current.appearance'),
        (r'"Power Usage"', r'LocalTranslation.current.powerUsage'),
        (r'"Language"', r'LocalTranslation.current.language'),
        (r'"Help & Support"', r'LocalTranslation.current.helpAndSupport'),
        (r'"About"', r'LocalTranslation.current.about'),
        (r'"Google Account, Username"', r'LocalTranslation.current.googleAccountUsername'),
        (r'"Who can see your activity"', r'LocalTranslation.current.whoCanSeeActivity'),
        (r'"Alerts, sounds, mentions"', r'LocalTranslation.current.alertsSoundsMentions'),
        (r'"Auto-download, composer, backups"', r'LocalTranslation.current.autoDownloadComposer'),
        (r'"Change colors and UI themes"', r'LocalTranslation.current.changeColorsUiThemes'),
        (r'"Battery & animations"', r'LocalTranslation.current.batteryAnimations'),
        (r'"Interface & region"', r'LocalTranslation.current.interfaceAndRegion'),
        (r'"FAQ, contact us"', r'LocalTranslation.current.faqContactUs'),
        (r'"Version 1.0.0 · Terms · Privacy"', r'LocalTranslation.current.versionTermsPrivacy'),
        (r'"Sign out"', r'LocalTranslation.current.signOut'),
        (r'"Edit"', r'LocalTranslation.current.edit'),
        (r'"Back"', r'LocalTranslation.current.back')
    ],
    'app/src/main/java/com/example/ui/LanguageScreen.kt': [
        (r'"Language"', r'LocalTranslation.current.language'),
        (r'"Interface language"', r'LocalTranslation.current.interfaceLanguage'),
        (r'"Auto-translate incoming messages"', r'LocalTranslation.current.autoTranslateIncoming'),
        (r'"Translate entire chat"', r'LocalTranslation.current.translateEntireChat'),
        (r'"Translate messages"', r'LocalTranslation.current.translateMessages'),
        (r'"Appears when you tap a text message"', r'LocalTranslation.current.appearsWhenTapMessage'),
        (r'"Show translate button"', r'LocalTranslation.current.showTranslateButton'),
        (r'"Help translate Cryptvora into your language — contact support to join our translator program."', r'LocalTranslation.current.helpTranslate'),
        (r'"Time format"', r'LocalTranslation.current.timeFormat'),
        (r'"24-hour"', r'LocalTranslation.current.twentyFourHour'),
        (r'"Region & format"', r'LocalTranslation.current.regionAndFormat'),
        (r'"Region"', r'LocalTranslation.current.region'),
        (r'"Back"', r'LocalTranslation.current.back')
    ],
    'app/src/main/java/com/example/ui/ChatListScreen.kt': [
        (r'"All Chats"', r'LocalTranslation.current.allChats'),
        (r'"Unread"', r'LocalTranslation.current.unread'),
        (r'"Groups"', r'LocalTranslation.current.groups'),
        (r'"Channels"', r'LocalTranslation.current.channels'),
        (r'"Search"', r'LocalTranslation.current.search'),
        (r'"Waiting for network"', r'LocalTranslation.current.waitingForNetwork'),
        (r'"No chats yet"', r'LocalTranslation.current.noChatsYet'),
        (r'"No unread messages"', r'LocalTranslation.current.noUnreadMessages'),
        (r'"No groups yet"', r'LocalTranslation.current.noGroupsYet'),
        (r'"No channels yet"', r'LocalTranslation.current.noChannelsYet'),
        (r'"Settings"', r'LocalTranslation.current.settings'),
        (r'"Delete Conversation"', r'LocalTranslation.current.deleteConversation'),
        (r'"Clear Chat History"', r'LocalTranslation.current.clearChatHistory'),
        (r'"Mute Notifications"', r'LocalTranslation.current.muteNotifications'),
        (r'"Unmute Notifications"', r'LocalTranslation.current.unmuteNotifications'),
        (r'"Pin Chat"', r'LocalTranslation.current.pinChat'),
        (r'"Unpin Chat"', r'LocalTranslation.current.unpinChat'),
        (r'"Add to Favorites"', r'LocalTranslation.current.addToFavorites'),
        (r'"Remove from Favorites"', r'LocalTranslation.current.removeFromFavorites'),
        (r'"View Profile"', r'LocalTranslation.current.viewProfile'),
        (r'"Share Contact"', r'LocalTranslation.current.shareContact'),
        (r'"Block User"', r'LocalTranslation.current.blockUser'),
        (r'"Are you sure you want to delete this conversation\? This cannot be undone."', r'LocalTranslation.current.deleteConversationConfirm'),
        (r'"All messages will be removed permanently."', r'LocalTranslation.current.clearHistoryConfirm'),
        (r'"You won\'t receive messages from this user."', r'LocalTranslation.current.blockUserConfirm'),
        (r'"Cancel"', r'LocalTranslation.current.cancel'),
        (r'"Delete"', r'LocalTranslation.current.delete'),
        (r'"Clear"', r'LocalTranslation.current.clear'),
        (r'"Block"', r'LocalTranslation.current.block'),
        (r'"Your Story"', r'LocalTranslation.current.yourStory')
    ]
}

for filepath, replacements in files.items():
    if not os.path.exists(filepath):
        print(f"File not found: {filepath}")
        continue
    with open(filepath, 'r') as f:
        content = f.read()
    
    if 'LocalTranslation' not in content:
        content = content.replace('import androidx.compose.ui.Alignment', 'import androidx.compose.ui.Alignment\nimport com.example.ui.i18n.LocalTranslation')
        
    for old, new in replacements:
        content = re.sub(old, new, content)
        
    with open(filepath, 'w') as f:
        f.write(content)

print("Done replacing.")
