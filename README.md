# Block Number

**[⬇️ حمّل الـ APK (آخر نسخة)](https://github.com/tareknahas85-star/block-number-android/releases/download/latest/block-number.apk)** &nbsp;|&nbsp; **[⬇️ Download latest APK](https://github.com/tareknahas85-star/block-number-android/releases/download/latest/block-number.apk)**

---

## بالعربي

تطبيق حظر مكالمات لأندرويد، شغال على `CallScreeningService` الرسمية تبع أندرويد — يعني بدون روت وبدون ما تستبدل تطبيق الاتصال الافتراضي. واجهة Material Design 3، عربي وإنكليزي (RTL).

### شو فيه (v2.0)
- حظر أي مكالمة من رقم مش محفوظ عندك بجهات الاتصال (تقدر تفعّله أو لأ)
- **قائمة سوداء محلية مع wildcards**: `*` = أي أرقام، `#` = رقم واحد (مثلاً `+9665*`)
- **قاعدة بيانات سبام أوفلاين**: بتحظر الأرقام يلي تقييمها سلبي، وبتتحدث من ملف CSV بعيد (تلقائي يومي أو يدوي)
- إشعار بمعلومات المتصل وهو عم يرن (تصنيف السبام، أو تنبيه إنو الرقم مجهول)
- إشعار اختياري لما تنحظر مكالمة
- سجل المكالمات المحظورة مع **سبب الحظر** — دوس على أي سطر تحظره أو تنسخ الرقم
- تحكم منفصل لكل شريحة على أجهزة الدوال سيم
- حظر الأرقام المخفية/الخاصة
- واجهة Material 3: تنقل سفلي (الرئيسية / القائمة السوداء / السجل)، بطاقة حالة، ألوان ديناميكية على أندرويد 12+

### قاعدة بيانات السبام
مستضافة كملفات ثابتة (افتراضياً من [block-number-data](https://github.com/tareknahas85-star/block-number-data)):
- `version.txt`: تاريخ آخر تحديث
- `spamdb.csv`: `number,category,negative,positive,neutral,name`

التطبيق بيتفقّد `version.txt` بس، وبيحمّل `spamdb.csv` الجديد بس لما يتغيّر — يعني تحديث بدون ما تحتاج تحدث التطبيق نفسه.

### شو بدك
- أندرويد 10 (API 29) أو أحدث
- صلاحيات: `READ_CONTACTS`, `READ_PHONE_STATE`, `POST_NOTIFICATIONS` (13+), `INTERNET` + دور Call Screening

### كيف بتشتغل
`ScreeningService` بتستقبل كل مكالمة قبل ما ترن، وبتمشي بسلسلة قرار: معطل/شريحة مستثناة → تسمح؛ رقم مخفي → تحظر لو مفعّل؛ **بجهات الاتصال → دايماً تسمح**؛ بالقائمة السوداء → تحظر؛ تقييم سلبي → تحظر لو مفعّل؛ مش بجهات الاتصال → تحظر لو مفعّل؛ غير هيك تسمح (وممكن تعرض إشعار معلومات المتصل). كل حظر بينسجل مع سببه. لو صلاحية جهات الاتصال مو موجودة، التطبيق بيسمح افتراضياً (fail open).

---

## In English

Android call blocking app built on the official `CallScreeningService` API — no root, no dialer replacement needed. Material Design 3 UI, Arabic + English with full RTL support.

### Features (v2.0)
- Block incoming calls from numbers **not in your contacts** (toggle on/off)
- **Local blacklist with wildcards**: `*` = any digits, `#` = one digit (e.g. `+9665*`)
- **Offline spam number database**: blocks numbers with a negative rating, updatable from a remote CSV (daily auto update or manual)
- **Caller info notification** while ringing (spam rating/category, or unknown caller notice)
- Optional notification when a call is blocked
- Blocked call log with **block reason** — tap an entry to blacklist or copy the number
- Per-SIM control on dual SIM devices
- Block hidden/private numbers
- Material 3 UI: bottom navigation (Home / Blacklist / Log), status card, dynamic color on Android 12+

### Spam database
Hosted as static files (default: [block-number-data](https://github.com/tareknahas85-star/block-number-data)):
- `version.txt` — single line, e.g. `2026-07-04`
- `spamdb.csv` — `number,category,negative,positive,neutral,name`

The CSV only gets downloaded when `version.txt` changes. A number gets auto-blocked when `negative >= 2 × positive` and the negative-rating toggle is on.

### Requirements
- Android 10 (API 29) or newer
- Permissions: `READ_CONTACTS`, `READ_PHONE_STATE`, `POST_NOTIFICATIONS` (13+), `INTERNET` + the **Call Screening role**

### Build
**GitHub Actions (recommended):** push to the repo and the *Build APK* workflow publishes `block-number.apk` straight to the [Releases page](https://github.com/tareknahas85-star/block-number-android/releases/tag/latest).

**Locally:**
```bash
gradle assembleDebug
# output: app/build/outputs/apk/debug/app-debug.apk
```
Needs JDK 17 and the Android SDK (compileSdk 35).

### How it works
`ScreeningService` receives every incoming call before it rings and walks a decision chain: screening off / SIM excluded → allow; hidden number → block if enabled; **in contacts → always allow**; blacklist match → block; negative spam rating → block if enabled; not in contacts → block if enabled; otherwise allow (optionally with a caller info notification). Every block gets logged with its reason. Fails open if the contacts permission isn't granted.
