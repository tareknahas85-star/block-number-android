# Block Number

**[⬇️ Download latest APK](https://github.com/tareknahas85-star/block-number-android/releases/download/latest/block-number.apk)** &nbsp;|&nbsp; **[⬇️ حمّل آخر نسخة APK](https://github.com/tareknahas85-star/block-number-android/releases/download/latest/block-number.apk)**

---

## In English

An Android app that blocks calls you do not want. It uses the official Android way to check calls, so you do not need root, and you do not need to change your phone dialer. The app is in Arabic and English.

### What it does

- Blocks any call from a number you did not save in your contacts (you can turn this on or off)
- Lets you make your own block list. You can block a whole group of numbers, for example every number that starts with `+9665`
- Keeps a list of known spam numbers on the phone
- Shows you who is calling while the phone rings, or tells you the number is unknown
- Can send you a notification when a call is blocked
- Keeps a list of blocked calls, and tells you why each call was blocked. Tap any line to block the number or copy it
- Works with two SIM cards. You can turn it on for one SIM and off for the other
- Blocks hidden and private numbers

### What you need

- Android 10 or newer
- The app asks for: your contacts, the phone state, notifications, and internet. You also give it the call screening role.

### How to build it

**With GitHub Actions (easier):** push your changes to the repo, and the app file is published on the [releases page](https://github.com/tareknahas85-star/block-number-android/releases/tag/latest).

**On your computer:**

```bash
gradle assembleDebug
```

The app file comes out at `app/build/outputs/apk/debug/app-debug.apk`. You need JDK 17 and the Android SDK.

### How it decides

When a call comes in, before the phone rings, the app checks it step by step:

1. Is the app off, or is this SIM excluded? Let the call in.
2. Is the number hidden? Block it, if you turned that on.
3. Is the number in your contacts? Always let it in.
4. Is the number in your block list? Block it.
5. Is the number known as spam? Block it, if you turned that on.
6. Is the number not in your contacts? Block it, if you turned that on.
7. If none of these, let the call in.

Every blocked call is saved with the reason. If the app cannot read your contacts, it lets calls in instead of blocking them.

---

## بالعربي

تطبيق أندرويد يحظر المكالمات التي لا تريدها. يستخدم الطريقة الرسمية في أندرويد لفحص المكالمات، فلا يحتاج روت، ولا يحتاج أن تغيّر تطبيق الاتصال في هاتفك. التطبيق بالعربي والإنكليزي.

### ماذا يفعل

- يحظر أي مكالمة من رقم لم تحفظه في جهات الاتصال (تستطيع تشغيل هذا أو إيقافه)
- يتيح لك عمل قائمة حظر خاصة بك. تستطيع حظر مجموعة أرقام كاملة، مثلاً كل رقم يبدأ بـ `+9665`
- يحتفظ بقائمة أرقام مزعجة معروفة داخل الهاتف
- يعرض لك من المتصل أثناء رنين الهاتف، أو يخبرك أن الرقم مجهول
- يستطيع إرسال إشعار لك عند حظر أي مكالمة
- يحتفظ بسجل المكالمات المحظورة، ويخبرك سبب حظر كل مكالمة. اضغط على أي سطر لتحظر الرقم أو تنسخه
- يعمل مع شريحتين. تستطيع تشغيله على شريحة وإيقافه على الأخرى
- يحظر الأرقام المخفية والخاصة

### ما الذي تحتاجه

- أندرويد 10 أو أحدث
- التطبيق يطلب: جهات الاتصال، وحالة الهاتف، والإشعارات، والإنترنت. كما تعطيه صلاحية فحص المكالمات.

### كيف تبنيه

**عبر GitHub Actions (الأسهل):** ارفع تعديلاتك إلى الريبو، وسيُنشر ملف التطبيق في [صفحة الإصدارات](https://github.com/tareknahas85-star/block-number-android/releases/tag/latest).

**على جهازك:**

```bash
gradle assembleDebug
```

يخرج ملف التطبيق في `app/build/outputs/apk/debug/app-debug.apk`. تحتاج JDK 17 و Android SDK.

### كيف يقرّر

عندما تأتي مكالمة، وقبل أن يرن الهاتف، يفحصها التطبيق خطوة خطوة:

1. هل التطبيق مُوقَف، أو هذه الشريحة مستثناة؟ يسمح بالمكالمة.
2. هل الرقم مخفي؟ يحظره، إذا كنت قد فعّلت ذلك.
3. هل الرقم في جهات اتصالك؟ يسمح به دائماً.
4. هل الرقم في قائمة الحظر عندك؟ يحظره.
5. هل الرقم معروف كرقم مزعج؟ يحظره، إذا كنت قد فعّلت ذلك.
6. هل الرقم غير موجود في جهات اتصالك؟ يحظره، إذا كنت قد فعّلت ذلك.
7. إذا لم ينطبق أي شيء مما سبق، يسمح بالمكالمة.

كل مكالمة محظورة تُسجَّل مع سببها. وإذا لم يستطع التطبيق قراءة جهات اتصالك، فإنه يسمح بالمكالمات بدلاً من حظرها.
