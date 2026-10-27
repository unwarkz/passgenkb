# PassGen Keyboard (`kz.unwar.passgenkb`)

**EN:** Minimalist Android keyboard for generating secure passwords on the fly. Zero permissions, no internet access, no ads, zero external dependencies.  
**RU:** Клавиатура — генератор паролей для Android без разрешений, подключений к интернету и рекламы. Позволяет задать используемые символы и длину, а также скопировать пароль в буфер обмена при генерации.  
**KK:** Рұқсаттарсыз, интернетсіз және жарнамасыз Android-қа арналған құпиясөз генератор-пернетақтасы. Пайдаланылатын таңбаларды және ұзындықты реттеуге, генерация кезінде құпиясөзді буферге көшіруге мүмкіндік береді.

## Features / Особенности / Ерекшеліктері
- **Zero Permissions:** No `<uses-permission>` tags in `AndroidManifest.xml`.
- **Zero Dependencies:** Pure Android SDK (`android.app.Activity` & `InputMethodService`), APK size ~50 KB.
- **Cryptographically Secure:** Uses `java.security.SecureRandom` with Fisher-Yates shuffle, guaranteed character set inclusion, and in-memory `CharArray` zeroing.
- **Clipboard Privacy:** Marks copied passwords with `ClipDescription.EXTRA_IS_SENSITIVE` on Android 13+ to hide plaintext in system clipboard previews.
- **Languages:** English, Русский, Қазақша.

## Build / Сборка
```bash
gradle assembleRelease