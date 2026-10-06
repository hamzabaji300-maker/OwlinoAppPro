import sys
import re

def process_file(filepath, funcs):
    with open(filepath, 'r') as f:
        content = f.read()

    for func in funcs:
        # find the function definition
        pattern = re.compile(r'(fun ' + func + r'\b.*?\{)', re.DOTALL)
        match = pattern.search(content)
        if match:
            # insert val translations = LocalTranslation.current
            insertion = '\n    val translations = com.example.ui.i18n.LocalTranslation.current'
            content = content[:match.end()] + insertion + content[match.end():]

    content = content.replace('LocalTranslation.current.', 'translations.')
    
    with open(filepath, 'w') as f:
        f.write(content)

process_file('app/src/main/java/com/example/ui/ChatListScreen.kt', ['ChatListScreen', 'MainBottomNavBar'])
process_file('app/src/main/java/com/example/ui/SettingsScreen.kt', ['SettingsScreen'])
process_file('app/src/main/java/com/example/ui/LanguageScreen.kt', ['LanguageScreen'])
